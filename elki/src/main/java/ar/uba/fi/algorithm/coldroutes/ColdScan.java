package ar.uba.fi.algorithm.coldroutes;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.graph.structure.DirectedEdge;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

import ar.uba.fi.algorithm.hotroutes.TrafficSets;
import ar.uba.fi.result.ColdRoute;
import ar.uba.fi.result.ColdRoutes;
import ar.uba.fi.result.JamRoutes;
import ar.uba.fi.roadnetwork.RoadNetwork;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
/*
 This file is developed to be used as part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Implementation of ColdScan algorithm - discovery of cold routes (alternative paths to jam routes)
 *
 * TODO: complete description
 *
 * @author Mariano Kohan
 *
 */
//TODO: tag '@' - min description
public class ColdScan implements Algorithm {

  private static final double MAX_ANGLE_DIFF = Math.PI / 5;
  private static final Logging LOG = Logging.getLogger(ColdScan.class);

  public static enum EXTEND_DIRECTION { FORWARD, BACKWARD};

  protected RoadNetwork roadNetwork;
  protected TrafficSets trafficSets;

  private double maxTraffic;
  private double expandXBR;
  private double expandYBR;
  private File jamRoutesFile;

  public ColdScan(File roadNetworkFile, double maxTraffic, double expandXBR, double expandYBR, File jamRoutesFile) {
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile); //TODO: cons. separar de constructor <-> ¿demora en GUI al setear parametros?
    this.maxTraffic = maxTraffic;
    this.expandXBR = expandXBR;
    this.expandYBR = expandYBR;
    this.jamRoutesFile = jamRoutesFile;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  //TODO: considerar declaracion de signature devolviendo Result del tipo ColdRoutes (en vez de base Result)
  @Override
  public Result run(Database database) {
    trafficSets = new TrafficSets(database);
    Set<String> jamEdgeIds = JamRoutes.parseJamEdgeIds(jamRoutesFile);

    ColdRoutes coldRoutes = new ColdRoutes(roadNetwork);

    try {
      List<DirectedEdge> coldEdges = this.getColdEdges(jamEdgeIds);
      Set<ColdRoute> forwardExtendedColdRoutes = new HashSet<ColdRoute>();
      for(DirectedEdge coldEdge : coldEdges) {
        ColdRoute coldRoute = new ColdRoute(coldEdge);
        forwardExtendedColdRoutes.addAll(this.extendColdRoute(coldRoute, EXTEND_DIRECTION.FORWARD, coldEdges));
      }
      Set<ColdRoute> extendedColdRoutes = new HashSet<ColdRoute>();
      for(ColdRoute forwardExtendedColdRoute : forwardExtendedColdRoutes) {
        extendedColdRoutes.addAll(this.extendColdRoute(forwardExtendedColdRoute, EXTEND_DIRECTION.BACKWARD, coldEdges));
      }
      coldRoutes.addColdRoutes(extendedColdRoutes);
      LOG.info("ColdScan discovers " + coldRoutes.getColdRoutes().size() + " cold routes");
    } catch (IOException ioException) {
      LOG.error("Exception during cold routes discovery: " + ioException.getMessage());
    }

    return coldRoutes;
  }

  private Set<ColdRoute> extendColdRoute(ColdRoute coldRoute, EXTEND_DIRECTION direction, List<DirectedEdge> coldEdges) {
    Set<ColdRoute> extendedColdRoutes = new HashSet<ColdRoute>();

    List<ColdRoute> coldRoutesToExtend = new LinkedList<ColdRoute>();
    coldRoutesToExtend.add(coldRoute);
    List<ColdRoute> currentColdRoutes;

    while (!coldRoutesToExtend.isEmpty()) {
      currentColdRoutes = coldRoutesToExtend;
      coldRoutesToExtend = new LinkedList<ColdRoute>();
      for(ColdRoute currentColdRoute : currentColdRoutes) {
        Set<DirectedEdge> coldTrafficReachableEdges = this.getDirectlyColdTrafficReachableEdges(currentColdRoute, direction);
        if (!coldTrafficReachableEdges.isEmpty()) {
          for(DirectedEdge coldTrafficReachableEdge : coldTrafficReachableEdges) {
            boolean addEdgeToEnd = direction.equals(EXTEND_DIRECTION.FORWARD) ? true: false;
            boolean isColdEdge = coldEdges.contains(coldTrafficReachableEdge)? true: false;
            ColdRoute extendedColdRoute = currentColdRoute.copyWithEdge(coldTrafficReachableEdge, addEdgeToEnd, isColdEdge);
            coldRoutesToExtend.add(extendedColdRoute);

          }
        } else {
          extendedColdRoutes.add(currentColdRoute);
        }
      }
    }
    return extendedColdRoutes;
  }

  private Set<DirectedEdge> getDirectlyColdTrafficReachableEdges(ColdRoute coldRoute, EXTEND_DIRECTION direction) {
    Set<DirectedEdge> coldTrafficReachableEdges = new HashSet<DirectedEdge>();
    DirectedEdge currentEdge;
    List adjacentEdges = null;
    if (direction.equals(EXTEND_DIRECTION.FORWARD)) {
      currentEdge = coldRoute.getLastEdge();
      adjacentEdges = currentEdge.getOutNode().getOutEdges();
    } else { //EXTEND_DIRECTION.BACKWARD
      currentEdge = coldRoute.getStartEdge();
      adjacentEdges = currentEdge.getInNode().getInEdges();
    }
    for(Iterator adjacentEdgesIterator = adjacentEdges.iterator(); adjacentEdgesIterator.hasNext();) {
      DirectedEdge adjacentEdge = (DirectedEdge) adjacentEdgesIterator.next();
      String adjacentEdgeId = ((SimpleFeature)adjacentEdge.getObject()).getID();
      Set<Integer> traffic = this.trafficSets.traffic(adjacentEdgeId);
      if ((traffic.size() <= this.maxTraffic) && (this.sameDirection(currentEdge, adjacentEdge))) {
        // |traffic(edge)| <= maxTraffic according to the order use to build the cold route
        if (!this.isRoadNetworkCycle(adjacentEdge, coldRoute)) {
          coldTrafficReachableEdges.add(adjacentEdge);
        }
      }
    }
    return coldTrafficReachableEdges;
  }

  private boolean sameDirection(DirectedEdge currentEdge, DirectedEdge adjacentEdge) {
    //TODO: consider improvement to avoid recalculate angle multiple times for same edge
    Coordinate[] currentEdgeCoordinates = ((MultiLineString)((SimpleFeature)currentEdge.getObject()).getDefaultGeometry()).getCoordinates();
    double currentEdgeAngleAngle = Angle.angle(currentEdgeCoordinates[0], currentEdgeCoordinates[currentEdgeCoordinates.length - 1]);

    Coordinate[] adjacentEdgeCoordinates = ((MultiLineString)((SimpleFeature)adjacentEdge.getObject()).getDefaultGeometry()).getCoordinates();
    double adjacentEdgeAngle = Angle.angle(adjacentEdgeCoordinates[0], adjacentEdgeCoordinates[adjacentEdgeCoordinates.length - 1]);

    double angleDiff = Angle.diff(currentEdgeAngleAngle, adjacentEdgeAngle);
    return (angleDiff < MAX_ANGLE_DIFF);
  }

  private boolean isRoadNetworkCycle(DirectedEdge adjacentEdge, ColdRoute coldRoute) {
    return coldRoute.contains(adjacentEdge);
  }

  private List<DirectedEdge> getColdEdges(Set<String> jamEdgeIds)
      throws IOException {
    List<DirectedEdge> coldEdges = new LinkedList<DirectedEdge>();

    SimpleFeatureCollection coldEdgeFeatures = this.getColdEdgeFeatures(jamEdgeIds);
    Collection edges = this.roadNetwork.getGraph().getEdges();

    for(Iterator edgesIterator = edges.iterator(); edgesIterator.hasNext();) {
      DirectedEdge edge = (DirectedEdge) edgesIterator.next();
      SimpleFeature edgeFeature = ((SimpleFeature)edge.getObject());
      if (coldEdgeFeatures.contains(edgeFeature)) {
        coldEdges.add(edge);
      }
    }

    return coldEdges;
  }

  private SimpleFeatureCollection getColdEdgeFeatures(Set<String> jamEdgeIds)
      throws IOException {
    DefaultFeatureCollection coldEdges = new DefaultFeatureCollection();

    SimpleFeatureCollection jamEdges = getJamEdgesFeatures(jamEdgeIds);
    SimpleFeatureCollection neighborhoodBREdges = getNeighborhoodBREdges(jamEdges);
    SimpleFeatureIterator simpleFeatureIterator =  neighborhoodBREdges.features();
    try {
      while (simpleFeatureIterator.hasNext()) {
        SimpleFeature neighborhoodBREdge = simpleFeatureIterator.next();
        Set<Integer> traffic = trafficSets.traffic(neighborhoodBREdge.getID());
        if (traffic.size() <= this.maxTraffic) {
          coldEdges.add(neighborhoodBREdge);
        }
      }
    }
    finally {
      simpleFeatureIterator.close();
    }

    return coldEdges;
  }

  private SimpleFeatureCollection getJamEdgesFeatures(Set<String> jamEdgeIds)
          throws IOException {
    FilterFactory2 ffilterFactory = CommonFactoryFinder.getFilterFactory2();

    List<FeatureIdImpl> jamEdgeFeatureIds = new LinkedList<FeatureIdImpl>();
    for(String jamEdgeId : jamEdgeIds) {
      jamEdgeFeatureIds.add(new FeatureIdImpl(jamEdgeId));
    }
    Id idFilter = ffilterFactory.id(jamEdgeFeatureIds.toArray(new FeatureId[] {}));
    return this.roadNetwork.getRoadsFeatureSource().getFeatures(idFilter);
  }

  private SimpleFeatureCollection getNeighborhoodBREdges(SimpleFeatureCollection edges)
      throws IOException {
    DefaultFeatureCollection neighborhoodBREdges = new DefaultFeatureCollection();
    SimpleFeatureCollection boundingRectangleEdges;
    SimpleFeatureIterator simpleFeatureIterator =  edges.features();
    try {
      while (simpleFeatureIterator.hasNext()) {
        SimpleFeature edge = simpleFeatureIterator.next();
        boundingRectangleEdges = getBoundingRectangleEdges(edge);
        neighborhoodBREdges.addAll(getDirectionEdges(edge, boundingRectangleEdges));
      }
    }
    finally {
      simpleFeatureIterator.close();
    }
    return neighborhoodBREdges;
  }

  private SimpleFeatureCollection getBoundingRectangleEdges(SimpleFeature edge)
        throws IOException {
    SimpleFeatureSource featureSource = this.roadNetwork.getRoadsFeatureSource();
    FilterFactory2 ffilterFactory = CommonFactoryFinder.getFilterFactory2();

    FeatureType schema = featureSource.getSchema();
    // usually "THE_GEOM" for shapefiles
    String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();

    BoundingBox edgeBoudingBox = edge.getBounds();
    ReferencedEnvelope boundingRectangle = new ReferencedEnvelope(edgeBoudingBox);
    double expandX = boundingRectangle.getWidth() * this.expandXBR / 2;
    double expandY = boundingRectangle.getHeight() * this.expandYBR / 2;
    boundingRectangle.expandBy(expandX, expandY);
    Filter bboxFilter = ffilterFactory.bbox(ffilterFactory.property(geometryPropertyName), boundingRectangle);

    //better definition according to (Banaei-Kashani et. al., 2011)
    Polygon boundingRectanglePolygon = JTS.toGeometry(boundingRectangle);
    Expression centroidFunction = ffilterFactory.function("centroid", ffilterFactory.property(geometryPropertyName));
    Filter brFilter = ffilterFactory.contains(ffilterFactory.literal(boundingRectanglePolygon), centroidFunction);

    //both filters (time improvement)
    Filter filters = ffilterFactory.and(bboxFilter, brFilter);
    return featureSource.getFeatures(filters);
  }

  private SimpleFeatureCollection getDirectionEdges(SimpleFeature jamEdge, SimpleFeatureCollection edges) {
    DefaultFeatureCollection directionEdges = new DefaultFeatureCollection();

    Coordinate[] jamCoordinates = ((MultiLineString)jamEdge.getDefaultGeometry()).getCoordinates();
    double jamAngle = Angle.angle(jamCoordinates[0], jamCoordinates[jamCoordinates.length - 1]);

    SimpleFeatureIterator simpleFeatureIterator =  edges.features();
    try {
        while (simpleFeatureIterator.hasNext()) {
          SimpleFeature edge = simpleFeatureIterator.next();
          if (!edge.getID().equals(jamEdge.getID())) {
            Coordinate[] edgeCoordinates = ((MultiLineString)edge.getDefaultGeometry()).getCoordinates();
            double edgeAngle = Angle.angle(edgeCoordinates[0], edgeCoordinates[edgeCoordinates.length - 1]);
            double angleDiff = Angle.diff(jamAngle, edgeAngle);
            if (angleDiff < MAX_ANGLE_DIFF) {
              directionEdges.add(edge);
            }
          }
      }
    }
    finally {
      simpleFeatureIterator.close();
    }
    return directionEdges;
  }


  /**
   * Parameterization class.
   *
   * @author Mariano Kohan
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {

    /**
     * Parameter that specifies the name of the file with the road network data
     * <p>
     * Key: {@code -coldscan.roadnetwork}
     * </p>
     */
    public static final OptionID ROAD_NETWORK_FILE_ID = new OptionID("coldscan.roadnetwork", "The file with the road network (shapefile format with line strings).");

    /**
     * Parameter to specify the threshold for maximum number of moving objects
     * Must be an double greater than 0.
     */
    public static final OptionID MAX_TRAFFIC_ID = new OptionID("coldscan.maxTraffic", "Threshold for maximum of moving objects to identify a cold route.");

    /**
     * Parameter to specify the multiplier to expand bounding rectangle in the X axis
     * Default value 0 (no expand = use bounding box).
     */
    public static final OptionID EXPAND_X_BR = new OptionID("coldscan.expandXBR", "Multiplier to expand bounding rectangle from neighborhood in the X axis direction (both sides).");

    /**
     * Parameter to specify the multiplier to expand bounding rectangle in the Y axis
     * Default value 0 (no expand = use bounding box).
     */
    public static final OptionID EXPAND_Y_BR = new OptionID("coldscan.expandYBR", "Multiplier to expand bounding rectangle from neighborhood in the Y axis direction (both sides).");

    /**
     * Parameter that specifies the name of the file with the discovered jam routes
     * <p>
     * Key: {@code -coldscan.jamroutes}
     * </p>
     */
    public static final OptionID ROAD_JAM_ROUTES_FILE_ID = new OptionID("coldscan.jamroutes", "The file with the jam routes discovered (and generated) by JamFlowScan.");

    protected File roadNetworkFile;
    private double maxTraffic;
    private double expandXBR;
    private double expandYBR;
    private File jamRoutesFile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter roadNetworkParameter = new FileParameter(ROAD_NETWORK_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(roadNetworkParameter)) {
        roadNetworkFile = roadNetworkParameter.getValue();
      }
      DoubleParameter maxTrafficParameter = new DoubleParameter(MAX_TRAFFIC_ID);
      if(config.grab(maxTrafficParameter)) {
        maxTraffic = maxTrafficParameter.getValue();
      }
      DoubleParameter expandXBRParameter = new DoubleParameter(EXPAND_X_BR, 0);
      if(config.grab(expandXBRParameter)) {
        expandXBR = expandXBRParameter.getValue();
      }
      DoubleParameter expandYBRParameter = new DoubleParameter(EXPAND_Y_BR, 0);
      if(config.grab(expandYBRParameter)) {
        expandYBR = expandYBRParameter.getValue();
      }
      FileParameter jamRoutesFileParameter = new FileParameter(ROAD_JAM_ROUTES_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(jamRoutesFileParameter)) {
        jamRoutesFile = jamRoutesFileParameter.getValue();
      }
    }

    @Override
    protected ColdScan makeInstance() {
      return new ColdScan(roadNetworkFile, maxTraffic, expandXBR, expandYBR, jamRoutesFile);
    }
  }

}
