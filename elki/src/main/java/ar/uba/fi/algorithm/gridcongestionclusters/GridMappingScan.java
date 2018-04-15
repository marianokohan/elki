package ar.uba.fi.algorithm.gridcongestionclusters;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.result.Cell;
import ar.uba.fi.result.CongestionCluster;
import ar.uba.fi.result.CongestionClusters;
import ar.uba.fi.roadnetwork.GridMapping;
import ar.uba.fi.roadnetwork.RoadNetwork;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
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
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * implementation discovery of congestion clusters from grid mapping of (Liu et. al., 2017)
 *
 * @author mariano kohan
 *
 */
public class GridMappingScan implements Algorithm {

  protected RoadNetwork roadNetwork;
  protected GridSpeeds gridSpeeds;

  private int eps = 2; //TODO parametrizar
  //private double minPts = 2;  //TODO parametrizar
  private double minPts = 150; //TODO parametrizar
  //private double minPts = 25; //sino cuarta parte  //TODO parametrizar

  public GridMappingScan() {
    // TODO parametrizar;
    File roadNetworkFile = new File("/media/data/doctorado_fiuba/datasets/reales/openstreetmap/mapzen_metro-extracts/Beijing/20170226/osm2pgsl-shapefiles/beijing_china_osm_line.shp");
    double[] area = { 116.201203, 116.545, 40.0257582, 39.754980};
    double sideLen = 0.001;
    //processed format
    // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); longitude; latitude; speed (in km/h)
    //File data = new File("/media/data/doctorado_fiuba/datasets/reales/ms_research/t-drive/sample_5/processed_speed/v4/1_processed_sampling-rate-cutted_proc-SRF_proc-S.txt");
    //viene automagico de elki

    roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    roadNetwork.setGridMapping(area, sideLen);

  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY); //TODO: validar si se ajusta impl. DBSCAN con existente elki
  }

  @Override
  public Result run(Database database) {
    this.gridSpeeds = new GridSpeeds(database, this.roadNetwork.getGridMapping());
    //Map<String, Double> cellsPerformanceIndex = this.gridSpeeds.calculateCellsPerformanceIndex();
    /*
     * TODO: si se define solo armado celdas
     *  - rename ?
     *  result MappedCells con listado MappedCells
     *    -> id feature, id attribute, minX, maxX, miny, maxY, performanceIndex
     *    TEMPORALMENTE para probar esto se genera en archivo (para validar uso DBSCAN directo)
     *    (sig. llamada)
     */
    //this.gridSpeeds.dumpCells();
    this.gridSpeeds.calculateCellsPerformanceIndex();

    CongestionClusters result = new CongestionClusters(this.roadNetwork);
    this.dbScan(this.roadNetwork.getGridMapping(), result);
    //result.cellsPerformanceIndex = cellsPerformanceIndex;
    return result;
  }

  private void dbScan(GridMapping grid, CongestionClusters result) {
    Set<Integer> processedCellsId = new HashSet<Integer>();
    Set<Integer> noiseCellsId = new HashSet<Integer>();

    for(SimpleFeatureIterator iterator = grid.getGridCellFeatures().features(); iterator.hasNext();) {
      SimpleFeature featureCell = iterator.next();
      Integer cellId = (Integer)featureCell.getAttribute("id");
      if (shouldProcessCell(processedCellsId, noiseCellsId, cellId)) {
        processedCellsId.add(cellId);
        SimpleFeatureCollection neighboorhood = grid.getRangeForCell(featureCell, eps);
        double cellSCI = this.sumPerformanceIndex(neighboorhood, processedCellsId, noiseCellsId, null);
        double cellPerformanceIndex = this.gridSpeeds.getPerformanceIndex(cellId);
        if (cellSCI >= minPts) {
          Cell coreCell = new Cell(cellId, featureCell,
                                    cellSCI, cellPerformanceIndex,
                                    true);
          CongestionCluster currentCluster = new CongestionCluster(coreCell);
          noiseCellsId.remove(noiseCellsId); //required to add 'border' cells
          System.out.println(String.format("expanding cluster on cell: %d", cellId));
          expandCluster(currentCluster, neighboorhood, grid, processedCellsId, noiseCellsId);
          System.out.println(String.format("cluster on cell %d expanded - size: %d, performance index sum: %f", cellId, currentCluster.size(), currentCluster.performanceIndexSum()));
          result.addCluster(currentCluster);
        } else {
          noiseCellsId.add(cellId);
        }
      }
      displayCellIdProcessedCells(cellId, processedCellsId, noiseCellsId);
    }
    displayTotalProcessed(grid.getGridCellFeatures().size(), processedCellsId, noiseCellsId, result.getClusters());
  }

  private boolean shouldProcessCell(Set<Integer> processedCellsId, Set<Integer> noiseCellsId, Integer cellId) {
    return (!processedCellsId.contains(cellId) || noiseCellsId.contains(cellId));
    //return (!processedCellsId.contains(cellId) || noiseCellsId.contains(cellId)) && ( cellId >= 47000 && cellId <= 50000 ); //para trabajar viz mas rapido
  }

  protected double sumPerformanceIndex(SimpleFeatureCollection neighboorhood, Set<Integer> processedCellsId, Set<Integer> noiseCellsId, CongestionCluster currentCluster) {
    double sci = 0;
    for(SimpleFeatureIterator iterator = neighboorhood.features(); iterator.hasNext();) {
      SimpleFeature neighboordCellFeature = iterator.next();
      Integer cellId = (Integer)neighboordCellFeature.getAttribute("id");
      if ((currentCluster != null && currentCluster.contains(cellId))
          || shouldProcessCell(processedCellsId, noiseCellsId, cellId))  {
        sci += this.gridSpeeds.getPerformanceIndex(cellId);
      }
    }
    return sci;
  }

  private void displayCellIdProcessedCells(Integer cellId, Set<Integer> processedCellsId, Set<Integer> noiseCellsId) {
    if (cellId % 500 == 0) {
      int processedCells = processedCellsId.size();
      int noiseCells = noiseCellsId.size();
      System.out.println(String.format("cell id: %d -> processed %d cells - detected as noise %d cells", cellId, processedCells, noiseCells));
    }
  }

  private void displayTotalProcessed(int total, Set<Integer> processedCellsId, Set<Integer> noiseCellsId, List<CongestionCluster> clustersDiscovered) {
    int processedCells = processedCellsId.size();
    int noiseCells = noiseCellsId.size();
    System.out.println(String.format("completed %d cells: processed %d cells - detected as noise %d cells", total, processedCells, noiseCells));
    System.out.println(String.format("Total number of clusters: %d", clustersDiscovered.size()));
  }

  private void expandCluster(CongestionCluster currentCluster, SimpleFeatureCollection neighboorhood, GridMapping grid, Set<Integer> processedCellsId, Set<Integer> noiseCellsId) {
    for(SimpleFeatureIterator neighboorhoodIterator = neighboorhood.features(); neighboorhoodIterator.hasNext();) {
      SimpleFeature neighboordCellFeature = neighboorhoodIterator.next();
      Integer neighboordCellId = (Integer)neighboordCellFeature.getAttribute("id");
      if (shouldProcessCell(processedCellsId, noiseCellsId, neighboordCellId)) {
        SimpleFeatureCollection neighboorhoodL2 = grid.getRangeForCell(neighboordCellFeature, eps);
        double neighboordCellSCI = this.sumPerformanceIndex(neighboorhoodL2, processedCellsId, noiseCellsId, currentCluster);
        double neighboordCellPerformanceIndex = this.gridSpeeds.getPerformanceIndex(neighboordCellId);
        Cell neighboordCell = new Cell(neighboordCellId, neighboordCellFeature, neighboordCellSCI, neighboordCellPerformanceIndex);
        currentCluster.addCell(neighboordCell);
        processedCellsId.add(neighboordCellId);
        noiseCellsId.remove(neighboordCellId); //required to add 'border' cells
        if (neighboordCellSCI >= minPts) {
          neighboordCell.setAsCoreCell();
          expandCluster(currentCluster, neighboorhoodL2, grid, processedCellsId, noiseCellsId);
        }
      }
    }
  }

}
