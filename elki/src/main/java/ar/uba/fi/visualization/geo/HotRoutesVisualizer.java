package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;

import ar.uba.fi.result.HotRoute;
import ar.uba.fi.result.HotRoutes;

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
public class HotRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  //internal parameterization
  private static final int MIN_HOT_ROUTE_EDGES = 1;
  private static final boolean DISPLAY_MAP = true;
  private static final boolean DISPLAY_TRAJECTORIES = false;
  private static final Color HOT_ROUTE_COLOR = new Color(245, 237, 0);
  private static final Color HOT_ROUTE_POINT_COLOR = new Color(204, 197, 0);
  static final Logging LOG = Logging.getLogger(HotRoutesVisualizer.class);

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // TODO improve -> base for cast and child for specific cases ?(to review)
    HotRoutes hotRoutes = null;
    StaticArrayDatabase database = null;
    for (Hierarchy.Iter<Result> iter = ((BasicResult)newResult).getHierarchy().iterChildren(newResult); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if (result instanceof StaticArrayDatabase) {
        database = (StaticArrayDatabase) result;
      }

      if (result instanceof HotRoutes) {
        hotRoutes = (HotRoutes) result;
      }
    }
    //TODO: consider that the applied validation will not separate the cases of "0 discovered hot routes" from "only trajectories"
//    displayFirstTrajectory(hotRoutes, database);
    if (hotRoutes.getHotRoutes().isEmpty())
      displayTrajectories(hotRoutes, database);
    else
      displayHotRoutes(hotRoutes, database);
  }

  private void displayHotRoutes(HotRoutes hotRoutes, Database database) {
    SimpleFeatureSource featureSource = hotRoutes.getRoadNetwork().getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle(hotRoutes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));
    if (DISPLAY_TRAJECTORIES)
      map.addLayer(createTrajectoriesLayer(featureSource, database));

    List<Point> hotRouteStartPoints = new LinkedList<Point>();
    List<Point> hotRouteEndPoints = new LinkedList<Point>();
    DefaultFeatureCollection hotRouteEdges = new DefaultFeatureCollection();
    for(HotRoute hotRoute : hotRoutes.getHotRoutes()) {
      if (hotRoute.getLength() >= MIN_HOT_ROUTE_EDGES) {
        hotRouteEdges.addAll(hotRoute.getEdgeFeatures());
        hotRouteStartPoints.add(hotRoute.getStartPoint());
        hotRouteEndPoints.add(hotRoute.getEndPoint());
      }
    }

    map.addLayer(createEdgesLayer(hotRouteEdges, featureSource, HOT_ROUTE_COLOR, 3));
    map.addLayer(createPointsLayer(this.createPointFeatureCollection(hotRouteStartPoints), featureSource, PointPositionType.START, HOT_ROUTE_POINT_COLOR, 5));
    map.addLayer(createPointsLayer(this.createPointFeatureCollection(hotRouteEndPoints), featureSource, PointPositionType.END, HOT_ROUTE_POINT_COLOR, 8));

    if (DISPLAY_MAP) {
      JMapFrame.showMap(map);
    }
  }

}
