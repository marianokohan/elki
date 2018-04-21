package ar.uba.fi.algorithm.gridcongestionclusters;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.algorithm.gridcongestionclusters.CellSpeeds.TimesliceSpeeds;
import ar.uba.fi.roadnetwork.GridMapping;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;

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

/**
 * calculates speed values and perfomances indexes, according to (Liu et. al., 2017)
 *
 * @author mariano kohan
 *
 */
public class GridSpeeds {

  private static final Logging LOG = Logging.getLogger(GridSpeeds.class);

  private Map<Integer, CellSpeeds> cellTimeSpeeds;

  public GridSpeeds(Database database, GridMapping grid) {
    LOG.info(String.format("[%s] Creating trajectory speed counters (with trajectory mean and cell free flow speeds) by cell and timeslice ...", new Date()));
    createTrajectorySpeedCountersByCellAndTimeslice(database, grid);
  }

  private void createTrajectorySpeedCountersByCellAndTimeslice(Database database, GridMapping grid) {
    this.cellTimeSpeeds = new HashMap<Integer,CellSpeeds>();
    //processed format
    // trajectory id (from sampling rate preprocessor); timeslice index (start from 0); cell Id (feature attribute); speed (in km/h)
    // order by cellId, timeslice index, trajectory id
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD , null); //timeslice index (start from 0); cell Id (feature attribute); speed (in km/h)
    Relation<LabelList> trIdRelation = database.getRelation(TypeUtil.LABELLIST, null); //list with trajectory Id
    DBIDIter trIdIter = trIdRelation.iterDBIDs();
    int row = 0;

    int mappedCellId = -1;
    int mappedTimeslice = -1;
    String trajectoryId = null;
    double speed;

    int currentCellId = -1;
    int currentTimeSlice = -1;
    String currentTrajectoryId = null;

    DescriptiveStatistics trajectorySpeedStats = new DescriptiveStatistics();
    DescriptiveStatistics cellSpeedStats = new DescriptiveStatistics();

    for(DBIDIter triter = trRelation.iterDBIDs(); triter.valid(); triter.advance()) {
      DoubleVector transationVector = trRelation.get(triter);

      mappedTimeslice = transationVector.intValue(0);
      mappedCellId = transationVector.intValue(1);
      speed = transationVector.doubleValue(2);

      trajectoryId = trIdRelation.get(trIdIter).get(0);

      if (currentTrajectoryId == null) {
        currentTrajectoryId = trajectoryId;
        currentTimeSlice = mappedTimeslice;
        currentCellId = mappedCellId;
      }

      this.dumpRow(row++, trajectoryId);

      if (currentCellId != mappedCellId) {
        this.addTrajectoryMeanSpeed(currentCellId, currentTimeSlice, currentTrajectoryId, trajectorySpeedStats);
        trajectorySpeedStats = new DescriptiveStatistics();
        trajectorySpeedStats.addValue(speed);
        this.addCellFreeFlowSpeed(currentCellId, cellSpeedStats);
        cellSpeedStats = new DescriptiveStatistics();
        cellSpeedStats.addValue(speed);
      } else {
        cellSpeedStats.addValue(speed);
        if ( (mappedTimeslice != currentTimeSlice) ||  (!currentTrajectoryId.equals(trajectoryId)) ) {
          this.addTrajectoryMeanSpeed(currentCellId, currentTimeSlice, currentTrajectoryId, trajectorySpeedStats);
          trajectorySpeedStats = new DescriptiveStatistics();
          trajectorySpeedStats.addValue(speed);
        } else {
          trajectorySpeedStats.addValue(speed);
        }
      }

      //change of cell, timeslice or trajectory is considered processing the summaries at the end
      trIdIter.advance();
      currentTrajectoryId = trajectoryId;
      currentTimeSlice = mappedTimeslice;
      currentCellId = mappedCellId;
    }

    this.addTrajectoryMeanSpeed(currentCellId, currentTimeSlice, currentTrajectoryId, trajectorySpeedStats);
    this.addCellFreeFlowSpeed(currentCellId, cellSpeedStats);

  }

  private void dumpRow(int row, String trajectoryId) {
    if (row % 100000 == 0) {
      LOG.debug(String.format("processing row %d for trajectory %s ...", row, trajectoryId));
    }
  }

  private void addTrajectoryMeanSpeed(int cellId, int timeslice, String trajectoryId, DescriptiveStatistics trajectorySpeedStats) {
    CellSpeeds cellSpeeds = this.cellTimeSpeeds.get(cellId);
    if (cellSpeeds == null) {
      cellSpeeds = new CellSpeeds(cellId);
      this.cellTimeSpeeds.put(cellSpeeds.getCellAttributeId(), cellSpeeds);
    }
    TimesliceSpeeds timesliceSpeeds = cellSpeeds.getTimesliceSpeeds(timeslice);
    timesliceSpeeds.addTrajectoryMeanSpeeds(trajectoryId, trajectorySpeedStats);
  }

  private void addCellFreeFlowSpeed(int cellId, DescriptiveStatistics cellSpeedStats) {
    this.cellTimeSpeeds.get(cellId).calculateFreeFlowSpeed(cellSpeedStats);
  }

  public void calculateCellsPerformanceIndex() {
    LOG.info(String.format("[%s] Calculating Timeslices Mean Speed ...", new Date()));
    calculateTimeslicesMeanSpeed();
    LOG.info(String.format("[%s] Calculating Cells Average Operation Speed ...", new Date()));
    calculateCellsAverageOperationSpeed();

    DescriptiveStatistics performanceIndexStats = new DescriptiveStatistics();
    LOG.info(String.format("[%s] Calculating performance index ...", new Date()));
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
      performanceIndexStats.addValue(cellSpeeds.calculatePerfomanceIndex());
    }

    LOG.debug("performance index stats: ");
    LOG.debug(performanceIndexStats.toString());
    LOG.info(String.format("[%s] Normalizing performance index ...", new Date()));
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
        cellSpeeds.normalizePerformanceIndex(performanceIndexStats.getMin(), performanceIndexStats.getMax());
    }
  }

  private void calculateCellsAverageOperationSpeed() {
    List<Integer> cellsWithZeroOperationSpeed = new ArrayList<Integer>();
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
      double averageOperationSpeed = cellSpeeds.calculateAverageOperationSpeed();
      if (averageOperationSpeed <= 0) {
        cellsWithZeroOperationSpeed.add(cellSpeeds.getCellAttributeId());
      }
    }
    double cellsWithZeroOperationSpeedPercentage = ((double)cellsWithZeroOperationSpeed.size()/this.cellTimeSpeeds.size())*100;
    LOG.debug("total cells: " + this.cellTimeSpeeds.size());
    LOG.debug("removing with zero operational speed: " + cellsWithZeroOperationSpeed.size() + " ("+(cellsWithZeroOperationSpeedPercentage)+"%)");
    cellsWithZeroOperationSpeed.forEach(new Consumer<Integer>() {
      @Override
      public void accept(Integer cellAttributeId) {
         cellTimeSpeeds.remove(cellAttributeId);
      }
    });
  }

  private void calculateTimeslicesMeanSpeed() {
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
      for(TimesliceSpeeds timesliceSpeeds : cellSpeeds.getTimeslicesSpeeds()) {
        timesliceSpeeds.calculateMean();
      }
    }
  }

  public double sumPerformanceIndex(SimpleFeatureCollection featureCells) {
    double sci = 0;
    for(SimpleFeatureIterator iterator = featureCells.features(); iterator.hasNext();) {
      SimpleFeature featureCell = iterator.next();
      CellSpeeds cellSpeeds = this.cellTimeSpeeds.get(featureCell.getAttribute("id"));
      if (cellSpeeds != null) { //exists mapped cell
        sci += cellSpeeds.getPerformanceIndex();
      }
    }
    return sci;
  }

  public double getPerformanceIndex(Integer cellAttributeId) {
    CellSpeeds cellSpeeds = this.cellTimeSpeeds.get(cellAttributeId);
    if (cellSpeeds != null) { //exists mapped cell
      return cellSpeeds.getPerformanceIndex();
    }
    return 0;
  }

  /*
   * build cells dump to be used by adapted DBSCAN
   * --> id attribute, (normalized) performanceIndex
   */
  public void dumpCells() {
    try {
      FileWriter cellsDump = new FileWriter("mapped_cells.txt");
      for(CellSpeeds cellSpeed : this.cellTimeSpeeds.values()) {
        cellsDump.append(cellSpeed.toDumpString()).append("\n");
      }
      cellsDump.close();
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
  }

  //used to test calculations
  public static void main(String[] args) {

    //time slice matching

    //1_7;1202207913000;116.45678;39.91749;2008-02-05 10:38:33;600;2
    DateTime timestamp1 = new DateTime(1202207913000L, DateTimeZone.UTC);
    //System.out.println("timestamp 1: " + timestamp1);

    //1_4;1202040029000;116.45551;39.88319;2008-02-03 12:00:29;600;6
    DateTime timestamp2 = new DateTime(1202040029000L, DateTimeZone.UTC);
    //System.out.println("timestamp 2: " + timestamp2);

    //10000_1;1201959890000;116.35929;39.93688;2008-02-02 13:44:50;302;2
    DateTime timestamp3 = new DateTime(1201959890000L, DateTimeZone.UTC);

    //100_2;1201998208000;116.45516;39.95009;2008-02-03 00:23:28;1361;1
    DateTime timestamp4 = new DateTime(1201998208000L, DateTimeZone.UTC);


    int timeSliceLength = 15 * 60; //15 min
    /*
     * timestamp 1 -> 10 * 4 + 3 = 43
     * timestamp 2 -> 12 * 4 + 1 = 49
     * timestamp 3 -> 13 * 4 + 3 = 55
     * timestamp 4 -> 2
     */

    int timeSlice1 = timestamp1.getSecondOfDay() / timeSliceLength + 1;
    DateTime timeInterval1Left = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay((timeSlice1 - 1) * timeSliceLength * 1000);
    DateTime timeInterval1Right = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay(timeSlice1 * timeSliceLength * 1000);
    System.out.println("timestamp 1: " + timestamp1);
    System.out.println("timestamp 1 -> time slice: " + timeSlice1 + " [" + timeInterval1Left + "; " + timeInterval1Right + "]");

    int timeSlice2 = timestamp2.getSecondOfDay() / timeSliceLength + 1;
    DateTime timeInterval2Left = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay((timeSlice2 - 1) * timeSliceLength * 1000);
    DateTime timeInterval2Right = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay(timeSlice2 * timeSliceLength * 1000);
    System.out.println("timestamp 2: " + timestamp2);
    System.out.println("timestamp 2 -> time slice: " + timeSlice2 + " [" + timeInterval2Left + "; " + timeInterval2Right + "]");

    int timeSlice3 = timestamp3.getSecondOfDay() / timeSliceLength + 1;
    DateTime timeInterval3Left = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay((timeSlice3 - 1) * timeSliceLength * 1000);
    DateTime timeInterval3Right = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay(timeSlice3 * timeSliceLength * 1000);
    System.out.println("timestamp 3: " + timestamp3);
    System.out.println("timestamp 3 -> time slice: " + timeSlice3 + " [" + timeInterval3Left + "; " + timeInterval3Right + "]");

    int timeSlice4 = timestamp4.getSecondOfDay() / timeSliceLength + 1;
    DateTime timeInterval4Left = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay((timeSlice4 - 1) * timeSliceLength * 1000);
    DateTime timeInterval4Right = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay(timeSlice4 * timeSliceLength * 1000);
    System.out.println("timestamp 4: " + timestamp4);
    System.out.println("timestamp 4 -> time slice: " + timeSlice4 + " [" + timeInterval4Left + "; " + timeInterval4Right + "]");

    DateTime timestamp5 = new DateTime("2008-02-03T01:00:00", DateTimeZone.UTC); //start of timeslice #5 (comenzando en 1)
    int timeSlice5 = timestamp5.getSecondOfDay() / timeSliceLength + 1;
    DateTime timeInterval5Left = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay((timeSlice5 - 1) * timeSliceLength * 1000);
    DateTime timeInterval5Right = new DateTime().withZone(DateTimeZone.UTC).withMillisOfDay(timeSlice5 * timeSliceLength * 1000);
    System.out.println("timestamp 5: " + timestamp5);
    System.out.println(" -> time slice: " + timeSlice5 + " [" + timeInterval5Left + "; " + timeInterval5Right + "]");


  }

}
