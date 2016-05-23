package ar.uba.fi.algorithm.coldroutes;

import java.io.File;
import java.io.IOException;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

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
//TODO: confirm license description
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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

  private static final Logging LOG = Logging.getLogger(ColdScan.class);

  protected RoadNetwork roadNetwork;

  private double maxTraffic;
//TODO: define field/parameters for BR -> define parameterization
  private double expandXBR;
  private double expandYBR;
  private File jamRoutesFile;

  //TODO: define parameters for BR
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
    LOG.debug("ColdScan - jam routes file: " + jamRoutesFile); //TODO: tmp for debug test
    Set<String> jamEdgeIds = JamRoutes.parseJamEdgeIds(jamRoutesFile);
    LOG.debug("ColdScan - jamEdgeIds: " + jamEdgeIds); //TODO: tmp for debug test
    /*
     * TODO: tmp test for debug next elements
     * ColdScan - jamEdgeIds: [san-francisco_california_osm_line.25204, san-francisco_california_osm_line.24434,  san-francisco_california_osm_line.22765, san-francisco_california_osm_line.22998, san-francisco_california_osm_line.23448, san-francisco_california_osm_line.22631, san-francisco_california_osm_line.23225,  san-francisco_california_osm_line.24641,  san-francisco_california_osm_line.22769,  san-francisco_california_osm_line.25082, san-francisco_california_osm_line.24539, san-francisco_california_osm_line.24344, san-francisco_california_osm_line.25433,  san-francisco_california_osm_line.24890, san-francisco_california_osm_line.24149,  san-francisco_california_osm_line.22670,  san-francisco_california_osm_line.23160, san-francisco_california_osm_line.23019, san-francisco_california_osm_line.23315, san-francisco_california_osm_line.25395, san-francisco_california_osm_line.22840, san-francisco_california_osm_line.22762,  san-francisco_california_osm_line.25203, san-francisco_california_osm_line.22209, san-francisco_california_osm_line.22704, san-francisco_california_osm_line.22626]
    edge [san-francisco_california_osm_line.22209]
  --  bounding box: ReferencedEnvelope[-122.4079951 : -122.4078923, 37.7379642 : 37.7387213]
  --  bounding rectangle: ReferencedEnvelope[-122.40809789999999 : -122.4077895, 37.7372071 : 37.7394784]
  --  bounding rectangle polygon: POLYGON ((-122.40809789999999 37.7372071, -122.4077895 37.7372071, -122.4077895 37.7394784, -122.40809789999999 37.7394784, -122.40809789999999 37.7372071))
   -> BR edges: featureCollection
     */
    /*
    Set<String> jamEdgeIds = new HashSet<String>();
    jamEdgeIds.add("san-francisco_california_osm_line.22209");
    */

    ColdRoutes coldRoutes = new ColdRoutes(roadNetwork);

    try {
      //TODO: tmp fields for incremental development verification
      SimpleFeatureCollection jamEdges = getJamEdgesFeatureCollection(jamEdgeIds);
      coldRoutes.jamEdges = jamEdges;
      //TODO: used on incremental verification development
//      coldRoutes.boundingRectangleEdges = getBoundingRectangleEdges(jamEdges);
      coldRoutes.neighborhoodBREdges = getNeighborhoodBREdges(jamEdges);

    } catch (IOException ioException) {
      LOG.error("Exception during bounding rectangle neighborhood processing: " + ioException.getMessage());
    }

    return coldRoutes;
  }

  private SimpleFeatureCollection getJamEdgesFeatureCollection(Set<String> jamEdgeIds)
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
        LOG.debug("edge [" + edge.getID() + "]");
        boundingRectangleEdges = getBoundingRectangleEdges(edge);
        neighborhoodBREdges.addAll(getDirectionEdges(edge, boundingRectangleEdges));
      }
    }
    finally {
      simpleFeatureIterator.close();
    }
    return neighborhoodBREdges;
  }

  /* TODO: used on incremental verification development
  private SimpleFeatureCollection getBoundingRectangleEdges(SimpleFeatureCollection edges)
      throws IOException {
    DefaultFeatureCollection boundingRectangleEdges = new DefaultFeatureCollection();
    SimpleFeatureCollection individualBoundingRectangleEdges;
    SimpleFeatureIterator simpleFeatureIterator =  edges.features();
    try {
      //TODO: tmp comment to verify only 1 jam
      while (simpleFeatureIterator.hasNext()) {
//        simpleFeatureIterator.hasNext();
        SimpleFeature edge = simpleFeatureIterator.next();
        LOG.debug("edge [" + edge.getID() + "]");
        individualBoundingRectangleEdges = getBoundingRectangleEdges(edge);
        LOG.debug(" \t -> BR edges: " + individualBoundingRectangleEdges.getID());
        boundingRectangleEdges.addAll(individualBoundingRectangleEdges);
      }
    }
    finally {
      simpleFeatureIterator.close();
    }
    return boundingRectangleEdges;
  }
  */

  //TODO: define other sizes for BR
  private SimpleFeatureCollection getBoundingRectangleEdges(SimpleFeature edge)
        throws IOException {
    SimpleFeatureSource featureSource = this.roadNetwork.getRoadsFeatureSource();
    FilterFactory2 ffilterFactory = CommonFactoryFinder.getFilterFactory2();

    FeatureType schema = featureSource.getSchema();
    // usually "THE_GEOM" for shapefiles
    String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();

    BoundingBox edgeBoudingBox = edge.getBounds();
    LOG.debug("  --  bounding box: " + edgeBoudingBox);
    ReferencedEnvelope boundingRectangle = new ReferencedEnvelope(edgeBoudingBox);
    //TODO: define other sizes for BR -> parameterization
    double expandX = boundingRectangle.getWidth() * this.expandXBR / 2;
    double expandY = boundingRectangle.getHeight() * this.expandYBR / 2;
    boundingRectangle.expandBy(expandX, expandY);
    LOG.debug("  --  bounding rectangle: " + boundingRectangle);
    Filter bboxFilter = ffilterFactory.bbox(ffilterFactory.property(geometryPropertyName), boundingRectangle);

    //better definition according to (Banaei-Kashani et. al., 2011)
    Polygon boundingRectanglePolygon = JTS.toGeometry(boundingRectangle); //TODO: verify parameters
    LOG.debug("  --  bounding rectangle polygon: " + boundingRectanglePolygon);
    Expression centroidFunction = ffilterFactory.function("centroid", ffilterFactory.property(geometryPropertyName));
    Filter brFilter = ffilterFactory.contains(ffilterFactory.literal(boundingRectanglePolygon), centroidFunction);

    //both filters (time improvement)
    Filter filters = ffilterFactory.and(bboxFilter, brFilter);
    return featureSource.getFeatures(filters);
  }

  private SimpleFeatureCollection getDirectionEdges(SimpleFeature jamEdge, SimpleFeatureCollection edges)
      throws IOException {
    DefaultFeatureCollection directionEdges = new DefaultFeatureCollection();
    Coordinate[] jamCoordinates = ((MultiLineString)jamEdge.getDefaultGeometry()).getCoordinates();
    double jamAngle = Angle.angle(jamCoordinates[0], jamCoordinates[jamCoordinates.length - 1]);
    LOG.debug("jam edge [" + jamEdge.getID() + "] - angle: " + jamAngle);
    SimpleFeatureIterator simpleFeatureIterator =  edges.features();
    try {
        while (simpleFeatureIterator.hasNext()) {
          SimpleFeature edge = simpleFeatureIterator.next();
          if (!edge.getID().equals(jamEdge.getID())) {
            Coordinate[] edgeCoordinates = ((MultiLineString)edge.getDefaultGeometry()).getCoordinates();
            double edgeAngle = Angle.angle(edgeCoordinates[0], edgeCoordinates[edgeCoordinates.length - 1]);
            LOG.debug(" -- br edge [" + edge.getID() + "] - angle: " + edgeAngle);
            double angleDiff = Angle.diff(jamAngle, edgeAngle);
            LOG.debug(" \t -> angle diff: " + angleDiff);
            if (angleDiff <= Math.PI / 4) {
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
