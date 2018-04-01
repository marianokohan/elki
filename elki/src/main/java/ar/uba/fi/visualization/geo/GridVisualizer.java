package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import ar.uba.fi.result.CongestionClusters;
import ar.uba.fi.roadnetwork.RoadNetwork;
/*
 This file is developed to run as part of ELKI:
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
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;

/**
 * @author mariano kohan
 *
 */
public class GridVisualizer extends MapVisualizer implements ResultHandler {

  protected static final Color GRID_LINE_COLOR = Color.YELLOW;
  protected static final double GRID_LINE_WIDTH = 0.0001;

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // TODO improve -> base for cast and child for specific cases ?(to review)
    CongestionClusters congestionClusters = null;
    StaticArrayDatabase database = null;
    for (Hierarchy.Iter<Result> iter = ((BasicResult)newResult).getHierarchy().iterChildren(newResult); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if (result instanceof StaticArrayDatabase) {
        database = (StaticArrayDatabase) result;
      }

      if (result instanceof CongestionClusters) {
        congestionClusters = (CongestionClusters) result;
      }
    }
    this.displayGrid(congestionClusters.getRoadNetwork());
  }

  public void displayGrid(RoadNetwork gridMappedRoadNetwork) {
    SimpleFeatureSource featureSource = gridMappedRoadNetwork.getRoadsFeatureSource();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();

    MapContent map = new MapContent();
    map.setTitle("Grid");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createGridLayer(grid, GRID_LINE_COLOR, GRID_LINE_WIDTH));

    JMapFrame.showMap(map);
  }

  //based initial testing
  public static void main(String[] args) {
    File roadNetworkFile = new File(args[0]);
    double[] area = { Double.parseDouble(args[1]),
        Double.parseDouble(args[2]),
        Double.parseDouble(args[3]),
        Double.parseDouble(args[4])
    };
    double sideLen = Double.parseDouble(args[5]);
    RoadNetwork gridMappedRoadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    gridMappedRoadNetwork.setGridMapping(area, sideLen);
    new GridVisualizer().displayGrid(gridMappedRoadNetwork);
  }


}
