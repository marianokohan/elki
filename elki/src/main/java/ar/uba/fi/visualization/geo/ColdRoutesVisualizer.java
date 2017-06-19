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
import org.opengis.feature.simple.SimpleFeature;

import processing.core.PApplet;
import ar.uba.fi.result.ColdRoute;
import ar.uba.fi.result.ColdRoutes;

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
public class ColdRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  private static final boolean DISPLAY_MAP = true;
  private static final Color COLD_ROUTE_COLD_TRAFFIC_COLOR = new Color(56, 150, 30);
  private static final Color COLD_ROUTE_COLOR = new Color(0, 118, 214);
  private static final Color COLD_ROUTE_POINT_COLOR = new Color(153,76, 0);

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
    displayColdRoutes(coldRoutes, database);
  }

  private void displayColdRoutes(final ColdRoutes coldRoutes, Database database) {
    final SimpleFeatureSource featureSource = coldRoutes.getRoadNetwork().getRoadsFeatureSource();

    MapContent map = createMapContent(coldRoutes, database, featureSource);

    mapFrame = new JMapFrame(map);
    //mapFrame.enableLayerTable(true); //to allow select and edit layers
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
                    selectFeatures(ev, featureSource, coldRoutes);
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
            mapFeatures(featureSource, coldRoutes);
        }
    });

    mapFrame.setSize(1400, 900);
    mapFrame.setVisible(true);
  }

  private MapContent createMapContent(ColdRoutes coldRoutes, Database database, final SimpleFeatureSource featureSource) {
    MapContent map = new MapContent();
    map.setTitle(coldRoutes.getLongName());

    map.addLayer(createRoadNetworkLayer(featureSource));

    Map<String, SimpleFeatureCollection> coldRoutesFeatures = extractFeatures(coldRoutes.getColdRoutes());
    if (coldRoutesFeatures.containsKey("EDGES")) {
      map.addLayer(createEdgesLayer(coldRoutesFeatures.get("EDGES"), featureSource, COLD_ROUTE_COLOR, 3));
    } else {
      System.out.println("only cold traffic edges");
    }
    if (coldRoutesFeatures.containsKey("COLD")) {
      map.addLayer(createEdgesLayer(coldRoutesFeatures.get("COLD"), featureSource, COLD_ROUTE_COLD_TRAFFIC_COLOR, 4));
    } else {
      System.out.println("no cold traffic edges => no cold routes");
    }
    map.addLayer(createPointsLayer(coldRoutesFeatures.get("STARTS"), featureSource, PointPositionType.START, COLD_ROUTE_POINT_COLOR, 5));
    map.addLayer(createPointsLayer(coldRoutesFeatures.get("ENDS"), featureSource, PointPositionType.END, COLD_ROUTE_POINT_COLOR, 8));

    return map;
  }

  private Map<String, SimpleFeatureCollection> extractFeatures(List<ColdRoute> coldRoutes) {
    Map<String, SimpleFeatureCollection> coldRoutesFeatures = new HashMap<String, SimpleFeatureCollection>();
    if (!coldRoutes.isEmpty()) {
      List<Point> coldRouteStartPointsList = new LinkedList<Point>();
      List<Point> coldRouteEndPointsList = new LinkedList<Point>();
      DefaultFeatureCollection coldRouteEdges = new DefaultFeatureCollection();
      DefaultFeatureCollection coldRouteColdTrafficEdges = new DefaultFeatureCollection();
      for(ColdRoute coldRoute : coldRoutes) {
        List<SimpleFeature>[] coldRouteEdgeFeatures = coldRoute.getEdgeWithColdFeatures();
        coldRouteEdges.addAll(coldRouteEdgeFeatures[0]);
        coldRouteColdTrafficEdges.addAll(coldRouteEdgeFeatures[1]);
        coldRouteStartPointsList.add(coldRoute.getStartPoint());
        coldRouteEndPointsList.add(coldRoute.getEndPoint());
      }
      if (!coldRouteEdges.isEmpty()) {
        coldRoutesFeatures.put("EDGES", coldRouteEdges);
      }
      if (!coldRouteColdTrafficEdges.isEmpty()) {
        coldRoutesFeatures.put("COLD", coldRouteColdTrafficEdges);
      }
      coldRoutesFeatures.put("STARTS", this.createPointFeatureCollection(coldRouteStartPointsList));
      coldRoutesFeatures.put("ENDS", this.createPointFeatureCollection(coldRouteEndPointsList));
    }
    return coldRoutesFeatures;
  }

  /**
   * This method is called by our feature selection tool when
   * the user has clicked on the map.
   *
   * @param ev the mouse event being handled
   */
  void selectFeatures(MapMouseEvent ev, SimpleFeatureSource featureSource, ColdRoutes coldRoutes) {
      SimpleFeatureCollection selectedFeatures = getSelectedFeaturedFromClick(ev, featureSource);
      List<ColdRoute> selectedColdRoutes = coldRoutes.filterColdRouteWithEdges(selectedFeatures);
      Map<String, SimpleFeatureCollection> selectedJamRoutesFeatures = extractFeatures(selectedColdRoutes);
      exportColdRoutesGeoJson(selectedJamRoutesFeatures);
      PApplet.main(new String[] { "--external", "ar.uba.fi.visualization.geo.map.ColdRoutesMapVisualizer"});
  }

  void mapFeatures(SimpleFeatureSource featureSource, ColdRoutes coldRoutes) {
      SimpleFeatureCollection selectedFeatures = getSelectedFeatureFromMap(featureSource);
      List<ColdRoute> selectedColdRoutes = coldRoutes.filterColdRouteWithEdges(selectedFeatures);
      Map<String, SimpleFeatureCollection> selectedJamRoutesFeatures = extractFeatures(selectedColdRoutes);
      exportColdRoutesGeoJson(selectedJamRoutesFeatures);
      PApplet.main(new String[] { "--external", "ar.uba.fi.visualization.geo.map.ColdRoutesMapVisualizer"});
  }

  private void exportColdRoutesGeoJson(Map<String, SimpleFeatureCollection> coldRoutesFeatures) {
    if (coldRoutesFeatures.containsKey("EDGES"))
      exportToGeoJson(coldRoutesFeatures.get("EDGES"), "cold_routes_edges.json");
    if (coldRoutesFeatures.containsKey("COLD"))
      exportToGeoJson(coldRoutesFeatures.get("COLD"), "cold_routes_cold_traffic.json");
    exportToGeoJson(coldRoutesFeatures.get("STARTS"), "cold_routes_starts.json");
    exportToGeoJson(coldRoutesFeatures.get("ENDS"), "cold_routes_ends.json");
  }

}
