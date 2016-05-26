package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import ar.uba.fi.result.ColdRoutes;

import com.vividsolutions.jts.geom.Geometry;

import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
//TODO: confirm license description
/*
 This file is developed to be used as part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
 * @author Mariano Kohan
 *
 */
public class ColdRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  //internal parameterization - TODO: review required
  /*
  private static final int MIN_JAM_ROUTE_EDGES = 1;
  private static final boolean DISPLAY_ONLY_ROUTES_WITH_JAMS = false;
  */
  private static final boolean DISPLAY_MAP = true;
  private static final Color JAM_COLOR = new Color(232, 4, 0);
  private static final Color BR_COLOR = new Color(0, 118, 214);
  private static final Color COLD_COLOR = new Color(56, 150, 30);

  /*
  private static final boolean DISPLAY_TRAJECTORIES = false;
  private static final boolean CREATE_JAM_ROUTES_FILE = true;
  private static final Color JAM_ROUTE_COLOR = new Color(245, 237, 0);
  private static final Color JAM_ROUTE_POINT_COLOR = new Color(204, 197, 0);
  private static final Color JAM_ROUTE_JAM_COLOR = new Color(232, 4, 0);
  */
  static final Logging LOG = Logging.getLogger(ColdRoutesVisualizer.class);

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // TODO improve
    ColdRoutes coldRoutes = null;
    StaticArrayDatabase database = null;
    for (Hierarchy.Iter<Result> iter = ((BasicResult)newResult).getHierarchy().iterChildren(newResult); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if (result instanceof StaticArrayDatabase) {
        database = (StaticArrayDatabase) result;
      }

      if (result instanceof ColdRoutes) {
        coldRoutes = (ColdRoutes) result;
      }
    }
    //TODO: consider that the applied validation will not separate the cases of "0 discovered hot routes" from "only trajectories"
    /*
    if (jamRoutes.getJamRoutes().isEmpty())
      displayTrajectories(jamRoutes, database);
    else
      displayJamRoutes(jamRoutes, database);
      logJamRoutesFile(jamRoutes);
    */

    displayJamRoutesJams(coldRoutes);
  }

  /*
   * TODO: base tmp method for debug
   */

  private void displayJamRoutesJams(ColdRoutes coldRoutes) {
    SimpleFeatureSource featureSource = coldRoutes.getRoadNetwork().getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle(coldRoutes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));

    //TODO: base impl for v1-initial testing
    //List<Geometry> jamEdgesGeometries = this.createJamEdgesGeometries(coldRoutes.jamEdgeIds, featureSource);
    //map.addLayer(createEdgesLayer(edgeGeometriesToFeatureCollection(jamEdgesGeometries), featureSource, JAM_COLOR, 3));

    //TODO: base impl for v2 verification
    //map.addLayer(createEdgesLayer(coldRoutes.boundingRectangleEdges, featureSource, BR_COLOR, 2));
    //map.addLayer(createEdgesLayer(coldRoutes.jamEdges, featureSource, JAM_COLOR, 3));

    //TODO: base impl for v3 verification
    //map.addLayer(createEdgesLayer(coldRoutes.neighborhoodBREdges, featureSource, BR_COLOR, 2));
    //map.addLayer(createEdgesLayer(coldRoutes.jamEdges, featureSource, JAM_COLOR, 3));

    //TODO: base impl for v4 verification
    map.addLayer(createEdgesLayer(coldRoutes.neighborhoodBREdges, featureSource, BR_COLOR, 2));
    map.addLayer(createEdgesLayer(coldRoutes.coldEdges, featureSource, COLD_COLOR, 3));
    map.addLayer(createEdgesLayer(coldRoutes.jamEdges, featureSource, JAM_COLOR, 3));

    if (DISPLAY_MAP) {
      JMapFrame.showMap(map);
    }
  }

  /*
   * TODO: initial tmp method for incremental development verification
  private List<Geometry> createJamEdgesGeometries(Set<String> jamEdgeIds, SimpleFeatureSource featureSource) {
    List<Geometry> edgeGeometries = new LinkedList<Geometry>();
    List<FeatureIdImpl> jamEdgeFeatureIds = new LinkedList<FeatureIdImpl>();
    for(String jamEdgeId : jamEdgeIds) {
      jamEdgeFeatureIds.add(new FeatureIdImpl(jamEdgeId));
    }

    FilterFactory2 ffilterFactory = CommonFactoryFinder.getFilterFactory2();
    SimpleFeatureCollection selectedFeatures = null;
    SimpleFeatureIterator simpleFeatureIterator = null;
    try {
      selectedFeatures = featureSource.getFeatures(ffilterFactory.id(jamEdgeFeatureIds.toArray(new FeatureId[] {})));
      simpleFeatureIterator = selectedFeatures.features();
      while (simpleFeatureIterator.hasNext()) {
        edgeGeometries.add((Geometry)simpleFeatureIterator.next().getDefaultGeometry());
      }
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    finally {
      if (simpleFeatureIterator != null)
        simpleFeatureIterator.close();
    }
    return edgeGeometries;
  }
   */

  /*
  private void displayJamRoutes(JamRoutes jamRoutes, Database database) {
    SimpleFeatureSource featureSource = jamRoutes.getRoadNetwork().getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle(jamRoutes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));
    if (DISPLAY_TRAJECTORIES)
      map.addLayer(createTrajectoriesLayer(featureSource, database));

    List<Point> jamRouteStartPoints = new LinkedList<Point>();
    List<Point> jamRouteEndPoints = new LinkedList<Point>();
    DefaultFeatureCollection jamRouteEdges = new DefaultFeatureCollection();
    DefaultFeatureCollection jamRouteJamEdges = new DefaultFeatureCollection();
    for(JamRoute jamRoute : jamRoutes.getJamRoutes()) {
      if (jamRoute.getLength() >= MIN_JAM_ROUTE_EDGES) {
        List<SimpleFeature>[] jamRouteEdgeFeatures = jamRoute.getEdgeWithJamsFeatures();
        if ( ((DISPLAY_ONLY_ROUTES_WITH_JAMS && (jamRouteEdgeFeatures[1].size() > 0))) || (!DISPLAY_ONLY_ROUTES_WITH_JAMS)) {
          jamRouteEdges.addAll(jamRouteEdgeFeatures[0]);
          jamRouteJamEdges.addAll(jamRouteEdgeFeatures[1]);
          jamRouteStartPoints.add(jamRoute.getStartPoint());
          jamRouteEndPoints.add(jamRoute.getEndPoint());
        }
      }
    }

    if (jamRouteEdges.size() > 0) {
      map.addLayer(createEdgesLayer(jamRouteEdges, featureSource, JAM_ROUTE_COLOR, 3));
    } else {
      System.out.println("only edges with jams ?!?");
    }
    if (jamRouteJamEdges.size() > 0) {
      map.addLayer(createEdgesLayer(jamRouteJamEdges, featureSource, JAM_ROUTE_JAM_COLOR, 4));
    } else {
      System.out.println("no edges with jams");
    }
    map.addLayer(createPointsLayer(jamRouteStartPoints, featureSource, PointPositionType.START, JAM_ROUTE_POINT_COLOR, 5));
    map.addLayer(createPointsLayer(jamRouteEndPoints, featureSource, PointPositionType.END, JAM_ROUTE_POINT_COLOR, 8));

    if (DISPLAY_MAP) {
      JMapFrame.showMap(map);
    }
  }

  private void logJamRoutesFile(JamRoutes jamRoutes) {
    if (CREATE_JAM_ROUTES_FILE) {
      Path jamRoutesFilePath = FileSystems.getDefault().getPath("jam_routes.txt");
      Charset charset = Charset.forName("UTF-8");
      try (BufferedWriter writer = Files.newBufferedWriter(jamRoutesFilePath, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        for(JamRoute jamRoute : jamRoutes.getJamRoutes()) {
          if (jamRoute.getLength() >= MIN_JAM_ROUTE_EDGES) {
            List<SimpleFeature>[] jamRouteEdgeFeatures = jamRoute.getEdgeWithJamsFeatures();
            if ( ((DISPLAY_ONLY_ROUTES_WITH_JAMS && (jamRouteEdgeFeatures[1].size() > 0))) || (!DISPLAY_ONLY_ROUTES_WITH_JAMS)) {
              writer.write(jamRoute.toString());
              writer.newLine();
            }
          }
        }
      } catch (IOException ioException) {
          System.err.format("IOException on logging jam routes to file %s: %s%n", jamRoutesFilePath, ioException);
      }
    }
  }
  */

}
