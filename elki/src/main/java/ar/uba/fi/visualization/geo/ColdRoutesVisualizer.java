package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.result.ColdRoute;
import ar.uba.fi.result.ColdRoutes;

import com.vividsolutions.jts.geom.Point;

import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
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
 * @author Mariano Kohan
 *
 */
public class ColdRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  private static final boolean DISPLAY_MAP = true;
  private static final Color COLD_ROUTE_COLD_TRAFFIC_COLOR = new Color(56, 150, 30);
  private static final Color COLD_ROUTE_COLOR = new Color(0, 118, 214);
  private static final Color COLD_ROUTE_POINT_COLOR = new Color(204, 197, 0);

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
    displayColdRoutes(coldRoutes);
  }

  private void displayColdRoutes(ColdRoutes coldRoutes) {
    SimpleFeatureSource featureSource = coldRoutes.getRoadNetwork().getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle(coldRoutes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));

    List<Point> coldRouteStartPoints = new LinkedList<Point>();
    List<Point> coldRouteEndPoints = new LinkedList<Point>();
    DefaultFeatureCollection coldRouteEdges = new DefaultFeatureCollection();
    DefaultFeatureCollection coldRouteColdTrafficEdges = new DefaultFeatureCollection();
    for(ColdRoute coldRoute : coldRoutes.getColdRoutes()) {
      List<SimpleFeature>[] coldRouteEdgeFeatures = coldRoute.getEdgeWithColdFeatures();
      coldRouteEdges.addAll(coldRouteEdgeFeatures[0]);
      coldRouteColdTrafficEdges.addAll(coldRouteEdgeFeatures[1]);
      coldRouteStartPoints.add(coldRoute.getStartPoint());
      coldRouteEndPoints.add(coldRoute.getEndPoint());
    }

    if (coldRouteEdges.size() > 0) {
      map.addLayer(createEdgesLayer(coldRouteEdges, featureSource, COLD_ROUTE_COLOR, 3));
    } else {
      System.out.println("only cold traffic edges");
    }
    if (coldRouteColdTrafficEdges.size() > 0) {
      map.addLayer(createEdgesLayer(coldRouteColdTrafficEdges, featureSource, COLD_ROUTE_COLD_TRAFFIC_COLOR, 4));
    } else {
      System.out.println("no cold traffic edges => no cold routes");
    }
    map.addLayer(createPointsLayer(coldRouteStartPoints, featureSource, PointPositionType.START, COLD_ROUTE_POINT_COLOR, 5));
    map.addLayer(createPointsLayer(coldRouteEndPoints, featureSource, PointPositionType.END, COLD_ROUTE_POINT_COLOR, 8));

    if (DISPLAY_MAP) {
      JMapFrame.showMap(map);
    }
  }

}
