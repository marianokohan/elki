package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;

import processing.core.PApplet;
import ar.uba.fi.result.DenseRoute;
import ar.uba.fi.result.DenseRoutes;

import com.vividsolutions.jts.geom.Point;

import de.lmu.ifi.dbs.elki.database.Database;
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
public class DenseRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  //internal parameterization
  private static final int MIN_DENSE_ROUTE_EDGES = 1;
  private static final boolean DISPLAY_MAP = true;
  private static final boolean DISPLAY_TRAJECTORIES = false;
  private static final Color DENSE_ROUTE_COLOR = new Color(245, 237, 0); //TODO: specific value for this
  private static final Color DENSE_ROUTE_POINT_COLOR = new Color(204, 197, 0); //TODO: specific value for this (?)
  static final Logging LOG = Logging.getLogger(DenseRoutesVisualizer.class);

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // TODO improve -> base for cast and child for specific cases ?(to review)
    DenseRoutes denseRoutes = null;
    StaticArrayDatabase database = null;
    for (Hierarchy.Iter<Result> iter = ((BasicResult)newResult).getHierarchy().iterChildren(newResult); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if (result instanceof StaticArrayDatabase) {
        database = (StaticArrayDatabase) result;
      }

      if (result instanceof DenseRoutes) {
        denseRoutes = (DenseRoutes) result;
      }
    }
    //TODO: consider that the applied validation will not separate the cases of "0 discovered hot routes" from "only trajectories"
    if (denseRoutes.getDenseRoutes().isEmpty())
      displayTrajectories(denseRoutes, database);
    else
      displayDenseRoutes(denseRoutes, database);
  }

  private void displayDenseRoutes(final DenseRoutes denseRoutes, Database database) {
    final SimpleFeatureSource featureSource = denseRoutes.getRoadNetwork().getRoadsFeatureSource();
    MapContent map = createMapContent(denseRoutes, database, featureSource);

    if (DISPLAY_MAP) {
      mapFrame = new JMapFrame(map);
      mapFrame.enableToolBar(true);
      mapFrame.enableStatusBar(true);

      JToolBar toolBar = mapFrame.getToolBar();
      JButton btn = new JButton("Map selected");
      toolBar.addSeparator();
      toolBar.add(btn);
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          mapFrame.getMapPane().setCursorTool(
              new CursorTool() {
                  @Override
                  public void onMouseClicked(MapMouseEvent ev) {
                      selectFeatures(ev, featureSource, denseRoutes);
                  }
              });
          }
      });

      btn = new JButton("Map area");
      toolBar.addSeparator();
      toolBar.add(btn);
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
              mapFeatures(featureSource, denseRoutes);
          }
      });

      mapFrame.setSize(1400, 900);
      mapFrame.setVisible(true);
    }
  }

  private MapContent createMapContent(DenseRoutes denseRoutes, Database database, SimpleFeatureSource featureSource) {
    MapContent map = new MapContent();
    map.setTitle(denseRoutes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));
    if (DISPLAY_TRAJECTORIES)
      map.addLayer(createTrajectoriesLayer(featureSource, database));

    Map<String, SimpleFeatureCollection> denseRoutesFeatures = extractFeatures(denseRoutes.getDenseRoutes());

    map.addLayer(createEdgesLayer(denseRoutesFeatures.get("EDGES"), featureSource, DENSE_ROUTE_COLOR, 3));
    map.addLayer(createPointsLayer(denseRoutesFeatures.get("STARTS"), featureSource, PointPositionType.START, DENSE_ROUTE_POINT_COLOR, 5));
    map.addLayer(createPointsLayer(denseRoutesFeatures.get("ENDS"), featureSource, PointPositionType.END, DENSE_ROUTE_POINT_COLOR, 8));
    return map;
  }

  private Map<String, SimpleFeatureCollection> extractFeatures(List<DenseRoute> denseRoutes) {
    Map<String, SimpleFeatureCollection> denseRoutesFeatures = new HashMap<String, SimpleFeatureCollection>();
    List<Point> denseRouteStartPoints = new LinkedList<Point>();
    List<Point> denseRouteEndPoints = new LinkedList<Point>();
    DefaultFeatureCollection denseRouteEdges = new DefaultFeatureCollection();
    for(DenseRoute denseRoute : denseRoutes) {
      if (denseRoute.getLength() >= MIN_DENSE_ROUTE_EDGES) {
        denseRouteEdges.addAll(denseRoute.getEdgeFeatures());
        denseRouteStartPoints.add(denseRoute.getStartPoint());
        denseRouteEndPoints.add(denseRoute.getEndPoint());
      }
    }
    denseRoutesFeatures.put("EDGES", denseRouteEdges);
    denseRoutesFeatures.put("STARTS", this.createPointFeatureCollection(denseRouteStartPoints));
    denseRoutesFeatures.put("ENDS", this.createPointFeatureCollection(denseRouteEndPoints));
    return denseRoutesFeatures;
  }

  /**
   * This method is called by our feature selection tool when
   * the user has clicked on the map.
   *
   * @param ev the mouse event being handled
   */
  void selectFeatures(MapMouseEvent ev, SimpleFeatureSource featureSource, DenseRoutes denseRoutes) {
      SimpleFeatureCollection selectedFeatures = getSelectedFeaturedFromClick(ev, featureSource);
      List<DenseRoute> selectedDenseRoutes = denseRoutes.filterDenseRouteWithEdges(selectedFeatures);
      Map<String, SimpleFeatureCollection> selectedDenseRoutesFeatures = extractFeatures(selectedDenseRoutes);
      if (exportDenseRoutesToGeoJson(selectedDenseRoutesFeatures))
        PApplet.main(new String[] { "--external", "ar.uba.fi.visualization.geo.map.DenseRoutesMapVisualizer"});
  }

  void mapFeatures(SimpleFeatureSource featureSource, DenseRoutes denseRoutes) {
      SimpleFeatureCollection selectedFeatures = getSelectedFeatureFromMap(featureSource);
      List<DenseRoute> mapDenseRoutes = denseRoutes.filterDenseRouteWithEdges(selectedFeatures);
      Map<String, SimpleFeatureCollection> mapDenseRoutesFeatures = extractFeatures(mapDenseRoutes);
      if (exportDenseRoutesToGeoJson(mapDenseRoutesFeatures))
        PApplet.main(new String[] { "--external", "ar.uba.fi.visualization.geo.map.DenseRoutesMapVisualizer"});
  }

  private boolean exportDenseRoutesToGeoJson(Map<String, SimpleFeatureCollection> denseRoutesFeatures) {
    //delete existing file to only have the new ones features exported
    deleteExportedFile("dense_routes_edges.json");
    deleteExportedFile("dense_routes_starts.json");
    deleteExportedFile("dense_routes_ends.json");
    boolean fileExported = false;
    if (denseRoutesFeatures.containsKey("EDGES")) {
      exportToGeoJson(denseRoutesFeatures.get("EDGES"), "dense_routes_edges.json");
      fileExported = true;
    }
    if (denseRoutesFeatures.containsKey("STARTS")) {
      exportToGeoJson(denseRoutesFeatures.get("STARTS"), "dense_routes_starts.json");
      fileExported = true;
    }
    if (denseRoutesFeatures.containsKey("ENDS")) {
      exportToGeoJson(denseRoutesFeatures.get("ENDS"), "dense_routes_ends.json");
      fileExported = true;
    }
    return fileExported;
  }

}
