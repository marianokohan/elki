package ar.uba.fi.visualization.geo;

import java.awt.Color;
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
public class GridCellsVisualizer extends GridVisualizer implements ResultHandler {

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
    //this.displayCells(congestionClusters.getRoadNetwork(), congestionClusters.mappedCells);
    //this.displayCellsId(congestionClusters.getRoadNetwork(), congestionClusters.cellsPerformanceIndex.keySet());
    this.displayCellsPerformanceIndex(congestionClusters.getRoadNetwork(), congestionClusters.cellsPerformanceIndex);
  }

  public void displayCellsList(RoadNetwork gridMappedRoadNetwork, List<SimpleFeature> cells) {
    DefaultFeatureCollection cellsFeatureCollection = new DefaultFeatureCollection();
    cellsFeatureCollection.addAll(cells);
    this.displayCells(gridMappedRoadNetwork, cellsFeatureCollection);
  }

  public void displayCellsId(RoadNetwork gridMappedRoadNetwork, Set<String> cellsId) {
    SimpleFeatureCollection cells = this.getCellFeatures(cellsId, gridMappedRoadNetwork);
    this.displayCells(gridMappedRoadNetwork, cells);
  }

  protected SimpleFeatureCollection getCellFeatures(Set<String> cellsId, RoadNetwork gridMappedRoadNetwork) {
    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();

    Set<FeatureId> fids = new HashSet<>();
    for (String id : cellsId) {
        FeatureId fid = ff.featureId(id);
        fids.add(fid);
    }
    Filter filter = ff.id(fids);
    try {
      return grid.getFeatures(filter);
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return null;
  }

  protected void displayCells(RoadNetwork gridMappedRoadNetwork, SimpleFeatureCollection cells) {
    SimpleFeatureSource featureSource = gridMappedRoadNetwork.getRoadsFeatureSource();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();

    MapContent map = new MapContent();
    map.setTitle("Mapped grid cells");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createGridLayer(grid, GRID_LINE_COLOR, GRID_LINE_WIDTH));
    map.addLayer(createGridCellsLayer(cells, grid, CELL_LINE_COLOR, CELL_LINE_WIDTH));

    JMapFrame.showMap(map);
  }

  protected class CellCategory {
    double min;
    double max;
    Color fill;
    Set<String> cells;
    SimpleFeatureCollection features;

    public CellCategory(double min, double max, Color fill) {
      this.min = min;
      this.max = max;
      this.fill = fill;
      this.cells = new HashSet<String>();
    }

    public boolean contains(double index) {
      return (index >= this.min) && (index < this.max);
    }

    public void add(String cellId) {
      this.cells.add(cellId);
    }

  }

  protected List<CellCategory>  categorizeCells(Map<String, Double> cellsPerformanceIndex) {
    List<CellCategory> cellCategories = new LinkedList<GridCellsVisualizer.CellCategory>();
    cellCategories.add(new CellCategory(0, 10, new Color(240, 200, 200)));
    cellCategories.add(new CellCategory(10, 20, new Color(240, 180, 180)));
    cellCategories.add(new CellCategory(20, 30, new Color(240, 160, 160)));
    cellCategories.add(new CellCategory(30, 40, new Color(240, 140, 140)));
    cellCategories.add(new CellCategory(40, 50, new Color(240, 120, 120)));
    cellCategories.add(new CellCategory(50, 60, new Color(240, 100, 100)));
    cellCategories.add(new CellCategory(60, 70, new Color(240, 80, 80)));
    cellCategories.add(new CellCategory(70, 80, new Color(240, 60, 60)));
    cellCategories.add(new CellCategory(80, 90, new Color(240, 40, 40)));
    cellCategories.add(new CellCategory(90, 101, new Color(240, 20, 20)));

    int nanIndexes = 0;
    for(Entry<String, Double> cellPerformanceIndex : cellsPerformanceIndex.entrySet()) {
      //System.out.println("cell: " + cellPerformanceIndex.getKey() + " - performance index: " + cellPerformanceIndex.getValue());
      for(CellCategory cellCategory : cellCategories) {
        if (cellPerformanceIndex.getValue().isNaN()) {
          nanIndexes++;
        } else {
          if (cellCategory.contains(cellPerformanceIndex.getValue())) {
            cellCategory.add(cellPerformanceIndex.getKey());
          }
        }
      }
    }
    int totalCells = cellsPerformanceIndex.size();
    double nanIndexesPercentage = ((double)nanIndexes/(double)totalCells)*100;
    System.out.println("cells performance indexes:  total "+ totalCells +" - NaN: " + nanIndexes + " ("+ nanIndexesPercentage + "%)" );

    return cellCategories;
  }

  public void displayCellsPerformanceIndex(RoadNetwork gridMappedRoadNetwork, Map<String, Double> cellsPerformanceIndex) {
    List<CellCategory> cellCategories = categorizeCells(cellsPerformanceIndex);
    for(CellCategory cellCategory : cellCategories) {
      cellCategory.features = getCellFeatures(cellCategory.cells, gridMappedRoadNetwork);
    }

    SimpleFeatureSource featureSource = gridMappedRoadNetwork.getRoadsFeatureSource();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();

    MapContent map = new MapContent();
    map.setTitle("Mapped grid cells");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createGridLayer(grid, GRID_LINE_COLOR, GRID_LINE_WIDTH));
    for(CellCategory cellCategory : cellCategories) {
      getCellFeatures(cellCategory.cells, gridMappedRoadNetwork);
      map.addLayer(createGridCellsLayer(cellCategory.features, grid, cellCategory.fill, CELL_LINE_WIDTH));
    }

    JMapFrame.showMap(map);
  }

  protected FeatureLayer createGridCellsLayer(SimpleFeatureCollection cells, SimpleFeatureSource grid, Color strokeColor, double strokeWitdh) {
    Rule rules[] = {createGridCellsStyleRule(grid, strokeColor, strokeWitdh)};
    return createLayer(cells, rules);
  }

  protected Rule createGridCellsStyleRule(SimpleFeatureSource featureSource, Color strokeColor, double strokeWitdh) {
    Stroke markStroke = styleFactory.createStroke(filterFactory.literal(strokeColor),
            filterFactory.literal(strokeWitdh));
    Fill markFill = styleFactory.createFill(filterFactory.literal(strokeColor));

    GeometryDescriptor geomDescriptor = featureSource.getSchema().getGeometryDescriptor();
    String geometryAttributeName = geomDescriptor.getLocalName();
    PolygonSymbolizer polygonSymbolizer = styleFactory.createPolygonSymbolizer(markStroke, markFill, geometryAttributeName);

    Rule lineRule = styleFactory.createRule();
    lineRule.symbolizers().add(polygonSymbolizer);
    return lineRule;
  }

}
