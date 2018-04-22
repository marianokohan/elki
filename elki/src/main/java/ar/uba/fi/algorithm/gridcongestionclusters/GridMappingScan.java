package ar.uba.fi.algorithm.gridcongestionclusters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import de.lmu.ifi.dbs.elki.data.DoubleVector;
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * implementation discovery of congestion clusters from grid mapping of (Liu et. al., 2017)
 *
 * @author mariano kohan
 *
 */
public class GridMappingScan implements Algorithm {

  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GridMappingScan.class);

  protected RoadNetwork roadNetwork;

  private int eps;
  private double minPts;

  private Map<Integer, Double> cellsPerfomanceIndex;

  public GridMappingScan(File roadNetworkFile, double areaXMin, double areaXMax, double areaYMax, double areaYMin,  double sideLen, int eps, double minPts) {
    double[] area = { areaXMin, areaXMax, areaYMax, areaYMin};
    roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    roadNetwork.setGridMapping(area, sideLen);

    this.eps = eps;
    this.minPts = minPts;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY); //TODO: validar si se ajusta impl. DBSCAN con existente elki
  }

  @Override
  public Result run(Database database) {
    LOG.info(String.format("[%s] Load cells performance index ...", new Date()));
    this.buildCellsPerformanceIndexMap(database);

    CongestionClusters result = new CongestionClusters(this.roadNetwork);
    LOG.info(String.format("[%s] Discovering congestion clusters ...", new Date()));
    this.dbScan(this.roadNetwork.getGridMapping(), result);
    return result;
  }

  private void buildCellsPerformanceIndexMap(Database database) {
    cellsPerfomanceIndex = new HashMap<Integer, Double>();
    //id cell, (normalized) performanceIndex
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD , null);
    for(DBIDIter triter = trRelation.iterDBIDs(); triter.valid(); triter.advance()) {
      DoubleVector transationVector = trRelation.get(triter);
      int cellId = transationVector.intValue(0);
      double performanceIndex = transationVector.doubleValue(1);
      cellsPerfomanceIndex.put(cellId, performanceIndex);
    }
  }

  public double getPerformanceIndex(Integer cellAttributeId) {
    Double performanceIndex = this.cellsPerfomanceIndex.get(cellAttributeId);
    if (performanceIndex != null) { //exists mapped cell
      return performanceIndex;
    }
    return 0;
  }

  public boolean isMappedCell(Integer cellAttributeId) {
    return this.cellsPerfomanceIndex.containsKey(cellAttributeId);
  }

  private void dbScan(GridMapping grid, CongestionClusters result) {
    Set<Integer> processedCellsId = new HashSet<Integer>();
    Set<Integer> noiseCellsId = new HashSet<Integer>();
    try {
      FileWriter clustersDump = new FileWriter("congestion_clusters.txt");
      for(SimpleFeatureIterator iterator = grid.getGridCellFeatures().features(); iterator.hasNext();) {
        SimpleFeature featureCell = iterator.next();
        Integer cellId = (Integer)featureCell.getAttribute("id");
        if (shouldProcessCell(processedCellsId, noiseCellsId, cellId)) {
          processedCellsId.add(cellId);
          SimpleFeatureCollection neighboorhood = grid.getRangeForCell(featureCell, eps);
          double cellSCI = this.sumPerformanceIndex(neighboorhood, processedCellsId, noiseCellsId, null);
          double cellPerformanceIndex = this.getPerformanceIndex(cellId);
          if (cellSCI >= minPts) {
            Cell coreCell = new Cell(cellId, featureCell,
                                      cellSCI, cellPerformanceIndex,
                                      true);
            CongestionCluster currentCluster = new CongestionCluster(coreCell);
            noiseCellsId.remove(noiseCellsId); //required to add 'border' cells
            LOG.debug(String.format("expanding cluster on cell: %d", cellId));
            expandCluster(currentCluster, neighboorhood, grid, processedCellsId, noiseCellsId);
            clustersDump.append(String.format("%d;%d;%f", cellId, currentCluster.size(), currentCluster.performanceIndexSum())).append("\n");
            result.addCluster(currentCluster);
          } else {
            noiseCellsId.add(cellId);
          }
        }
        displayCellIdProcessedCells(cellId, processedCellsId, noiseCellsId);
      }
      clustersDump.close();
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }

    displayTotalProcessed(grid.getGridCellFeatures().size(), processedCellsId, noiseCellsId, result.getClusters());
  }

  private boolean shouldProcessCell(Set<Integer> processedCellsId, Set<Integer> noiseCellsId, Integer cellId) {
    return (this.isMappedCell(cellId)) && (!processedCellsId.contains(cellId) || noiseCellsId.contains(cellId));
    //return (this.isMappedCell(cellId)) && (!processedCellsId.contains(cellId) || noiseCellsId.contains(cellId)) && ( cellId >= 40000 && cellId <= 43000 ); //para trabajar viz mas rapido
  }

  protected double sumPerformanceIndex(SimpleFeatureCollection neighboorhood, Set<Integer> processedCellsId, Set<Integer> noiseCellsId, CongestionCluster currentCluster) {
    double sci = 0;
    for(SimpleFeatureIterator iterator = neighboorhood.features(); iterator.hasNext();) {
      SimpleFeature neighboordCellFeature = iterator.next();
      Integer cellId = (Integer)neighboordCellFeature.getAttribute("id");
      if ((currentCluster != null && currentCluster.contains(cellId))
          || shouldProcessCell(processedCellsId, noiseCellsId, cellId))  {
        sci += this.getPerformanceIndex(cellId);
      }
    }
    return sci;
  }

  private void displayCellIdProcessedCells(Integer cellId, Set<Integer> processedCellsId, Set<Integer> noiseCellsId) {
    if (cellId % 1000 == 0) {
      int processedCells = processedCellsId.size();
      int noiseCells = noiseCellsId.size();
      LOG.info(String.format("cell id: %d -> processed %d cells - detected as noise %d cells", cellId, processedCells, noiseCells));
    }
  }

  private void displayTotalProcessed(int total, Set<Integer> processedCellsId, Set<Integer> noiseCellsId, List<CongestionCluster> clustersDiscovered) {
    int processedCells = processedCellsId.size();
    int noiseCells = noiseCellsId.size();
    LOG.info(String.format("completed %d cells: processed %d cells - detected as noise %d cells", total, processedCells, noiseCells));
    LOG.info(String.format("[%s] Total number of clusters: %d", new Date(), clustersDiscovered.size()));
  }

  private void expandCluster(CongestionCluster currentCluster, SimpleFeatureCollection neighboorhood, GridMapping grid, Set<Integer> processedCellsId, Set<Integer> noiseCellsId) {
    for(SimpleFeatureIterator neighboorhoodIterator = neighboorhood.features(); neighboorhoodIterator.hasNext();) {
      SimpleFeature neighboordCellFeature = neighboorhoodIterator.next();
      Integer neighboordCellId = (Integer)neighboordCellFeature.getAttribute("id");
      if (shouldProcessCell(processedCellsId, noiseCellsId, neighboordCellId)) {
        SimpleFeatureCollection neighboorhoodL2 = grid.getRangeForCell(neighboordCellFeature, eps);
        double neighboordCellSCI = this.sumPerformanceIndex(neighboorhoodL2, processedCellsId, noiseCellsId, currentCluster);
        double neighboordCellPerformanceIndex = this.getPerformanceIndex(neighboordCellId);
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

  /**
   * Parameterization class.
   *
   * @author Mariano Kohan
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {

    /**
     * Parameter that specifies the name of the file with the road network data
     * <p>
     * Key: {@code -gridmappingscan.roadnetwork}
     * </p>
     */
    public static final OptionID ROAD_NETWORK_FILE_ID = new OptionID("gridmappingscan.roadnetwork", "The file with the road network (shapefile format with line strings).");


    /**
     * Parameter to specify the area bound x1
     * Default value: 116.201203
     */
    public static final OptionID AREA_X_MIN_ID = new OptionID("gridmappingscan.areaX1", "Area bound x1");

    /**
     * Parameter to specify the area bound x2
     * Default value: 116.545
     */
    public static final OptionID AREA_X_MAX_ID = new OptionID("gridmappingscan.areaX2", "Area bound x2");

    /**
     * Parameter to specify the area bound y1
     * Default value: 40.0257582
     */
    public static final OptionID AREA_Y_MAX_ID = new OptionID("gridmappingscan.areaY1", "Area bound y1");

    /**
     * Parameter to specify the area bound y1
     * Default value: 39.754980
     */
    public static final OptionID AREA_Y_MIN_ID = new OptionID("gridmappingscan.areaY2", "Area bound y2");

    /**
     * Parameter to specify the side length of the grid mapping cells
     * Default value: 0.001
     */
    public static final OptionID SIDE_LEN = new OptionID("gridmappingscan.sideLen", "Side length of the grid mapping cells");


    /**
     * Parameter to specify the epsilon parameter for (adapted) dbscan.
     * Default value: 2
     */
    public static final OptionID EPSILON_ID = new OptionID("gridmappingscan.eps", "Parameter epsilon for (adapted) dbscan.");

    /**
     * Parameter to specify the minPts parameter for (adapted) dbscan.
     * Must be an double greater than 0.
     */
    public static final OptionID MIN_PTS_ID = new OptionID("gridmappingscan.minPts", "Parameter minPts for (adapted) dbscan.");



    protected File roadNetworkFile;
    protected double areaXMin = 116.201203;
    protected double areaXMax = 116.545;
    protected double areaYMax = 40.0257582;
    protected double areaYMin = 39.754980;
    protected double sideLen = 0.001;

    protected int eps = 2;
    protected double minPts;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter fileP = new FileParameter(ROAD_NETWORK_FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        roadNetworkFile = fileP.getValue();
      }

      DoubleParameter areaXMinParameter = new DoubleParameter(AREA_X_MIN_ID, 116.201203);
      if(config.grab(areaXMinParameter)) {
        areaXMin = areaXMinParameter.getValue();
      }
      DoubleParameter areaXMaxParameter = new DoubleParameter(AREA_X_MAX_ID, 116.545);
      if(config.grab(areaXMaxParameter)) {
        areaXMax = areaXMaxParameter.getValue();
      }
      DoubleParameter areaYMaxParameter = new DoubleParameter(AREA_Y_MAX_ID, 40.0257582);
      if(config.grab(areaYMaxParameter)) {
        areaYMax = areaYMaxParameter.getValue();
      }
      DoubleParameter areaYMinParameter = new DoubleParameter(AREA_Y_MIN_ID, 39.754980);
      if(config.grab(areaYMinParameter)) {
        areaYMin = areaYMinParameter.getValue();
      }
      DoubleParameter sideLenParameter = new DoubleParameter(SIDE_LEN, 0.001);
      if(config.grab(sideLenParameter)) {
        sideLen = sideLenParameter.getValue();
      }

      IntParameter epsilonParameter = new IntParameter(EPSILON_ID, 2);
      if(config.grab(epsilonParameter)) {
        eps = epsilonParameter.getValue();
      }
      DoubleParameter minPtsParameter = new DoubleParameter(MIN_PTS_ID);
      minPtsParameter.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(minPtsParameter)) {
        minPts = minPtsParameter.getValue();
      }

    }

    @Override
    protected GridMappingScan makeInstance() {
      return new GridMappingScan(roadNetworkFile, areaXMin, areaXMax, areaYMax, areaYMin, sideLen, eps, minPts);
    }
  }

}
