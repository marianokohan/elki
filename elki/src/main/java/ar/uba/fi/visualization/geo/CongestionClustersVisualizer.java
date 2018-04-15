package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
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

import ar.uba.fi.result.CongestionCluster;
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
public class CongestionClustersVisualizer extends GridVisualizer implements ResultHandler {

  private static final Color CELL_LINE_COLOR = Color.ORANGE;
  private static final double CELL_LINE_WIDTH = 2;


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
    this.displayCongestionClusters(congestionClusters.getRoadNetwork(), congestionClusters);
  }

  public void displayCongestionClusters(RoadNetwork gridMappedRoadNetwork, CongestionClusters congestionClusters) {
    DefaultFeatureCollection cellsFeatureCollection = new DefaultFeatureCollection();
    for(CongestionCluster cluster : congestionClusters.getClusters()) {
      List<SimpleFeature> clusterCellFeatures = cluster.getCellFeatures();
      cellsFeatureCollection.addAll(clusterCellFeatures);
    }
    this.displayCongestionClustersCells(gridMappedRoadNetwork, cellsFeatureCollection);
  }

  protected void displayCongestionClustersCells(RoadNetwork gridMappedRoadNetwork, SimpleFeatureCollection cells) {
    SimpleFeatureSource featureSource = gridMappedRoadNetwork.getRoadsFeatureSource();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();

    MapContent map = new MapContent();
    map.setTitle("Grid congestion clusters");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createGridLayer(grid, GRID_LINE_COLOR, GRID_LINE_WIDTH));
    map.addLayer(createGridCellsClusterLayer(cells, grid, CELL_LINE_COLOR, CELL_LINE_WIDTH));

    JMapFrame.showMap(map);
  }

  protected FeatureLayer createGridCellsClusterLayer(SimpleFeatureCollection cells, SimpleFeatureSource grid, Color strokeColor, double strokeWitdh) {
    Rule rules[] = {createGridCellsClusterStyleRule(grid, strokeColor, strokeWitdh)};
    return createLayer(cells, rules);
  }

  protected Rule createGridCellsClusterStyleRule(SimpleFeatureSource featureSource, Color strokeColor, double strokeWitdh) {
    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(strokeColor),
            filterFactory.literal(strokeWitdh), filterFactory.literal(0.75));
    Fill markFill = styleFactory.createFill(filterFactory.literal(strokeColor), filterFactory.literal(0.5));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    PolygonSymbolizer polygonSymbolizer = styleFactory.createPolygonSymbolizer(markStroke, markFill, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(polygonSymbolizer);
    return lineRule;
  }

}

