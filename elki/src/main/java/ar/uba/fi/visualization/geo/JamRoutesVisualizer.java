package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.result.JamRoute;
import ar.uba.fi.result.JamRoutes;

import com.vividsolutions.jts.geom.Point;

import de.lmu.ifi.dbs.elki.database.Database;
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
public class JamRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  //internal parameterization
  private static final int MIN_JAM_ROUTE_EDGES = 1;
  private static final boolean DISPLAY_ONLY_ROUTES_WITH_JAMS = false;
  private static final boolean DISPLAY_MAP = true;
  private static final boolean DISPLAY_TRAJECTORIES = false;
  private static final Color JAM_ROUTE_COLOR = new Color(245, 237, 0);
  private static final Color JAM_ROUTE_POINT_COLOR = new Color(204, 197, 0);
  private static final Color JAM_ROUTE_JAM_COLOR = new Color(232, 4, 0);

  static final Logging LOG = Logging.getLogger(JamRoutesVisualizer.class);

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // TODO improve
    JamRoutes jamRoutes = null;
    StaticArrayDatabase database = null;
    for (Hierarchy.Iter<Result> iter = ((BasicResult)newResult).getHierarchy().iterChildren(newResult); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if (result instanceof StaticArrayDatabase) {
        database = (StaticArrayDatabase) result;
      }

      if (result instanceof JamRoutes) {
        jamRoutes = (JamRoutes) result;
      }
    }
    //TODO: consider that the applied validation will not separate the cases of "0 discovered hot routes" from "only trajectories"
    if (jamRoutes.getJamRoutes().isEmpty())
      displayTrajectories(jamRoutes, database);
    else
      displayJamRoutes(jamRoutes, database);
  }

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

}
