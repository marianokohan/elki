package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
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

import ar.uba.fi.result.CongestionClusters;
import ar.uba.fi.roadnetwork.GridMapping;
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

  public void displayCellsFeatureId(RoadNetwork gridMappedRoadNetwork, Set<String> cellsId) {
    SimpleFeatureCollection cells = gridMappedRoadNetwork.getGridMapping().getCellFeatures(cellsId);
    this.displayCells(gridMappedRoadNetwork, cells);
  }

  public void displayCellsAttributeId(RoadNetwork gridMappedRoadNetwork, Set<Integer> cellsId) {
    SimpleFeatureCollection cells = gridMappedRoadNetwork.getGridMapping().getCellFeatureFromAttributesId(cellsId);
    SimpleFeatureIterator featureIterator = cells.features();
    this.calculateGridDistance(featureIterator.next(), featureIterator.next(), gridMappedRoadNetwork.getGridMapping());
    this.displayCells(gridMappedRoadNetwork, cells);
  }

  protected void calculateGridDistance(SimpleFeature cell1, SimpleFeature cell2, GridMapping gridMapping) {
    double distance = gridMapping.calculateDistance(cell1, cell2);
    System.out.println("distance = " + Math.round(distance));
    System.out.println(" -> " + cell1.getAttribute("id") + " - " + cell1);
    System.out.println(" -> " + cell2.getAttribute("id") + " - " + cell2);
  }

  public void displayCellAttributeId(RoadNetwork gridMappedRoadNetwork, Integer cellsId) {
    SimpleFeatureCollection cells = gridMappedRoadNetwork.getGridMapping().getCellFeatureFromAttributeId(cellsId);
    this.displayCells(gridMappedRoadNetwork, cells);
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


  protected void displayCellNeighborhood(RoadNetwork gridMappedRoadNetwork, Integer cellAttributeId, double eps) throws NoSuchElementException, IOException {
    SimpleFeatureSource featureSource = gridMappedRoadNetwork.getRoadsFeatureSource();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();
    //Set<Integer> selectedCellId = new HashSet<Integer>();
    //selectedCellId.add(cellAttributeId);
    //SimpleFeatureCollection cell = gridMappedRoadNetwork.getGridMapping().getCellFeatureFromAttributesId(selectedCellId);
    //SimpleFeatureCollection neighborhood = gridMappedRoadNetwork.getGridMapping().getRangeForCell(cell.features().next(), eps);
    SimpleFeatureCollection cell = gridMappedRoadNetwork.getGridMapping().getCellFeatureFromAttributeId(cellAttributeId);
    SimpleFeatureCollection neighborhood = gridMappedRoadNetwork.getGridMapping().getRangeForCell(cellAttributeId, eps);

    MapContent map = new MapContent();
    map.setTitle("Mapped grid cells");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createGridLayer(grid, GRID_LINE_COLOR, GRID_LINE_WIDTH));
    map.addLayer(createGridCellsLayer(neighborhood, grid, Color.PINK , CELL_LINE_WIDTH));
    map.addLayer(createGridCellsLayer(cell, grid, CELL_LINE_COLOR, CELL_LINE_WIDTH));

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
    nanIndexes = nanIndexes / 10; //counter for each category
    int totalCells = cellsPerformanceIndex.size();
    double nanIndexesPercentage = ((double)nanIndexes/(double)totalCells)*100;
    System.out.println("cells performance indexes:  total "+ totalCells +" - NaN: " + nanIndexes + " ("+ nanIndexesPercentage + "%)" );

    return cellCategories;
  }

  public void displayCellsPerformanceIndex(RoadNetwork gridMappedRoadNetwork, Map<String, Double> cellsPerformanceIndex) {
    List<CellCategory> cellCategories = categorizeCells(cellsPerformanceIndex);
    for(CellCategory cellCategory : cellCategories) {
      cellCategory.features = gridMappedRoadNetwork.getGridMapping().getCellFeatures(cellCategory.cells);
    }

    SimpleFeatureSource featureSource = gridMappedRoadNetwork.getRoadsFeatureSource();
    SimpleFeatureSource grid = gridMappedRoadNetwork.getGridMapping().getGrid();

    MapContent map = new MapContent();
    map.setTitle("Mapped grid cells");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createGridLayer(grid, GRID_LINE_COLOR, GRID_LINE_WIDTH));
    for(CellCategory cellCategory : cellCategories) {
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

  //test distance function
  public static void main(String[] args) throws NoSuchElementException, IOException {
    File roadNetworkFile = new File(args[0]);
    double[] area = { Double.parseDouble(args[1]),
        Double.parseDouble(args[2]),
        Double.parseDouble(args[3]),
        Double.parseDouble(args[4])
    };
    double sideLen = Double.parseDouble(args[5]);
    RoadNetwork gridMappedRoadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    gridMappedRoadNetwork.setGridMapping(area, sideLen);
    //new GridVisualizer().displayGrid(gridMappedRoadNetwork);
    //total number of grid cells: 92610
    Integer[] cellIds = { 1, 5, 10, 300, 23152, 46305, 90000, 92610 };
    //Integer[] cellIds = { 5, 10, 300, 23152, 46305, 90000, 92610 };
    //Integer[] cellIds = {1, 300, 600};
    //Integer[] cellIds = {1, 350}; //6 - 350 en segunda fila
    //Integer[] cellIds = {1000, 1002}; //2
    //Integer[] cellIds = {1000, 320}; //6
    //Integer[] cellIds = {320, 1000}; //6 -> symmetric
    //Integer[] cellIds = {1320, 1000}; //23e
    //Integer[] cellIds = {1000, 1340}; //3
    //Integer[] cellIds = {320, 1345}; //4
    ////Integer[] cellIds = {1320, 1660}; //3
    //Integer[] cellIds = {2000, 3020}; //9
    //Integer[] cellIds = {4050, 3020}; //3
    //Integer[] cellIds = {4050, 10220}; //18
    Set<Integer> cellsIdSet = new HashSet<Integer>();
    cellsIdSet.addAll(Arrays.asList(cellIds));
    //new GridCellsVisualizer().displayCellsAttributeId(gridMappedRoadNetwork, cellsIdSet);
    //new GridCellsVisualizer().displayCellAttributeId(gridMappedRoadNetwork, 92610);

    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 1, 1);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 1, 2);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 1, 3);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 10, 1);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 10, 2);
    new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 10, 3);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 1000, 1);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 1000, 2);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 1000, 3);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 10000, 1);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 10000, 2);
    //new GridCellsVisualizer().displayCellNeighborhood(gridMappedRoadNetwork, 10000, 3);

  }

}

