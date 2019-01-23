package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.opengis.feature.simple.SimpleFeature;

import processing.core.PApplet;
import ar.uba.fi.result.JamRoute;
import ar.uba.fi.result.JamRoutes;

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
public class JamRoutesVisualizer extends RoutesVisualizer implements ResultHandler {

  //internal parameterization
  private static final int MIN_JAM_ROUTE_EDGES = 1;
  private static final boolean DISPLAY_ONLY_ROUTES_WITH_JAMS = false;
  private static final boolean DISPLAY_MAP = true;
  private static final boolean DISPLAY_TRAJECTORIES = false;
  private static final boolean CREATE_JAM_ROUTES_FILE = true;
  private static final Color JAM_ROUTE_COLOR = new Color(255, 153, 51);
  private static final Color JAM_ROUTE_POINT_COLOR = new Color(153,76, 0);
  private static final Color JAM_ROUTE_JAM_COLOR = new Color(232, 4, 0);

  static final Logging LOG = Logging.getLogger(JamRoutesVisualizer.class);

  private JamRoutes jamRoutes;
  private List<JamRoute> filteredJamRoutes;

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    jamRoutes = null;
    database = null;
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
      displayJamRoutes();
    logJamRoutesFile(jamRoutes);
  }

  private void displayJamRoutes() {
    featureSource = jamRoutes.getRoadNetwork().getRoadsFeatureSource();
    MapContent map = createMapContent();

    if (DISPLAY_MAP) {
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
                      SimpleFeatureCollection selectedFeatures = getSelectedFeaturedFromClick(ev, featureSource);
                      List<JamRoute> selectedJamRoutes = jamRoutes.filterJamRouteWithEdges(selectedFeatures, DISPLAY_ONLY_ROUTES_WITH_JAMS);
                      exportJamRoutes(selectedJamRoutes);
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
              SimpleFeatureCollection selectedFeatures = getSelectedFeatureFromMap(featureSource);
              List<JamRoute> mapJamRoutes = jamRoutes.filterJamRouteWithEdges(selectedFeatures, DISPLAY_ONLY_ROUTES_WITH_JAMS);
              exportJamRoutes(mapJamRoutes);
          }
      });

      btn = new JButton("Filter selected");
      toolBar.addSeparator();
      toolBar.add(btn);
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          mapFrame.getMapPane().setCursorTool(
              new CursorTool() {
                  @Override
                  public void onMouseClicked(MapMouseEvent ev) {
                      SimpleFeatureCollection selectedFeatures = getSelectedFeaturedFromClick(ev, featureSource);
                      filteredJamRoutes = jamRoutes.filterJamRouteWithEdges(selectedFeatures, DISPLAY_ONLY_ROUTES_WITH_JAMS);
                      if (filteredJamRoutes.isEmpty()) {
                        System.out.println("Filtered jam routes empty");
                        filteredJamRoutes = jamRoutes.getJamRoutes();
                      } else {
                        removeMapLayers();
                        createAndAddMapLayers(mapFrame.getMapContent());
                      }
                  }
              });
          }
      });

      btn = new JButton("Show all");
      toolBar.addSeparator();
      toolBar.add(btn);
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
              removeMapLayers();
              filteredJamRoutes = jamRoutes.getJamRoutes();
              createAndAddMapLayers(mapFrame.getMapContent());
          }
      });

      btn = new JButton("Map filtered");
      toolBar.addSeparator();
      toolBar.add(btn);
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
              exportJamRoutes(filteredJamRoutes);
          }
      });

      mapFrame.setSize(1400, 900);
      mapFrame.setVisible(true);
    }
  }

  private MapContent createMapContent() {
    MapContent map = new MapContent();
    map.setTitle(jamRoutes.getLongName());
    filteredJamRoutes = jamRoutes.getJamRoutes();
    createAndAddMapLayers(map);
    return map;
  }

  private void createAndAddMapLayers(MapContent map) {
    this.layers = new LinkedList<Layer>();
    layers.add(createRoadNetworkLayer(featureSource));

    if (DISPLAY_TRAJECTORIES)
      layers.add(createTrajectoriesLayer(featureSource, database));

    Map<String, SimpleFeatureCollection> jamRoutesFeatures = extractFeatures(filteredJamRoutes);
    if (jamRoutesFeatures.containsKey("EDGES")) {
      layers.add(createEdgesLayer(jamRoutesFeatures.get("EDGES"), featureSource, JAM_ROUTE_COLOR, 3));
    } else {
      System.out.println("only edges with jams ?!?");
    }
    if (jamRoutesFeatures.containsKey("JAMS")) {
      layers.add(createEdgesLayer(jamRoutesFeatures.get("JAMS"), featureSource, JAM_ROUTE_JAM_COLOR, 4));
    } else {
      System.out.println("no edges with jams");
    }
    layers.add(createPointsLayer(jamRoutesFeatures.get("STARTS"), featureSource, PointPositionType.START, JAM_ROUTE_POINT_COLOR, 5));
    layers.add(createPointsLayer(jamRoutesFeatures.get("ENDS"), featureSource, PointPositionType.END, JAM_ROUTE_POINT_COLOR, 8));

    for(Layer layer : layers) {
      map.addLayer(layer);
    }
  }

  private Map<String, SimpleFeatureCollection> extractFeatures(List<JamRoute> jamRoutes) {
    Map<String, SimpleFeatureCollection> jamRoutesFeatures = new HashMap<String, SimpleFeatureCollection>();
    if (!jamRoutes.isEmpty()) {
      List<Point> jamRouteStartPointsList = new LinkedList<Point>();
      List<Point> jamRouteEndPointsList = new LinkedList<Point>();
      DefaultFeatureCollection jamRouteEdges = new DefaultFeatureCollection();
      DefaultFeatureCollection jamRouteJamEdges = new DefaultFeatureCollection();
      for(JamRoute jamRoute : jamRoutes) {
        if (jamRoute.getLength() >= MIN_JAM_ROUTE_EDGES) {
          List<SimpleFeature>[] jamRouteEdgeFeatures = jamRoute.getEdgeWithJamsFeatures();
          if ( ((DISPLAY_ONLY_ROUTES_WITH_JAMS && (jamRouteEdgeFeatures[1].size() > 0))) || (!DISPLAY_ONLY_ROUTES_WITH_JAMS)) {
            jamRouteEdges.addAll(jamRouteEdgeFeatures[0]);
            jamRouteJamEdges.addAll(jamRouteEdgeFeatures[1]);
            jamRouteStartPointsList.add(jamRoute.getStartPoint());
            jamRouteEndPointsList.add(jamRoute.getEndPoint());
          }
        }
      }
      if (!jamRouteEdges.isEmpty()) {
        jamRoutesFeatures.put("EDGES", jamRouteEdges);
      }
      if (!jamRouteJamEdges.isEmpty()) {
        jamRoutesFeatures.put("JAMS", jamRouteJamEdges);
      }
      jamRoutesFeatures.put("STARTS", this.createPointFeatureCollection(jamRouteStartPointsList));
      jamRoutesFeatures.put("ENDS", this.createPointFeatureCollection(jamRouteEndPointsList));
    }
    return jamRoutesFeatures;
  }

  private void exportJamRoutes(List<JamRoute> jamRoutes) {
    Map<String, SimpleFeatureCollection> jamRoutesFeatures = extractFeatures(jamRoutes);
    if (exportJamRoutesToGeoJson(jamRoutesFeatures))
      PApplet.main(new String[] { "--external", "ar.uba.fi.visualization.geo.map.JamRoutesMapVisualizer"});
  }

  private boolean exportJamRoutesToGeoJson(Map<String, SimpleFeatureCollection> jamRoutesFeatures) {
    //delete existing file to only have the new ones features exported
    deleteExportedFile("jam_routes_edges.json");
    deleteExportedFile("jam_routes_jams.json");
    deleteExportedFile("jam_routes_starts.json");
    deleteExportedFile("jam_routes_ends.json");
    boolean fileExported = false;
    if (jamRoutesFeatures.containsKey("EDGES")) {
      exportToGeoJson(jamRoutesFeatures.get("EDGES"), "jam_routes_edges.json");
      fileExported = true;
    }
    if (jamRoutesFeatures.containsKey("JAMS")) {
      exportToGeoJson(jamRoutesFeatures.get("JAMS"), "jam_routes_jams.json");
      fileExported = true;
    }
    if (jamRoutesFeatures.containsKey("STARTS")) {
      exportToGeoJson(jamRoutesFeatures.get("STARTS"), "jam_routes_starts.json");
      fileExported = true;
    }
    if (jamRoutesFeatures.containsKey("ENDS")) {
      exportToGeoJson(jamRoutesFeatures.get("ENDS"), "jam_routes_ends.json");
      fileExported = true;
    }
    return fileExported;
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

}
