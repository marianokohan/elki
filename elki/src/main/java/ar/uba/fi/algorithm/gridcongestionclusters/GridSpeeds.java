package ar.uba.fi.algorithm.gridcongestionclusters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ar.uba.fi.algorithm.gridcongestionclusters.CellSpeeds.TimesliceSpeeds;
import ar.uba.fi.algorithm.gridcongestionclusters.CellSpeeds.TrajectorySpeeds;
import ar.uba.fi.roadnetwork.GridMapping;

import com.vividsolutions.jts.geom.Coordinate;

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

  private Map<String, CellSpeeds> cellTimeSpeeds;

  private int timeSliceLength = 15 * 60; //15 min - TODO: consider parametrize

  public GridSpeeds(Database database, GridMapping grid) {
    mapTrajectorySpeedByCellAndTimeslice(database, grid);
    calculateTrajectoryMeanSpeedByCellAndTimeslice();
  }

  private void calculateTrajectoryMeanSpeedByCellAndTimeslice() {
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
      for(TimesliceSpeeds timesliceSpeeds : cellSpeeds.getTimeslicesSpeeds()) {
        for(TrajectorySpeeds trajectorySpeeds : timesliceSpeeds.getTrajectoriesSpeeds()) {
          trajectorySpeeds.calculateMean();
        }
      }
    }
  }

  private void mapTrajectorySpeedByCellAndTimeslice(Database database, GridMapping grid) {
    this.cellTimeSpeeds = new HashMap<String,CellSpeeds>();
    //processed format
    // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); longitude; latitude; speed (in km/h)
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD , null); //timestamp (in milliseconds); longitude; latitude; speed (in km/h)
    Relation<LabelList> trIdRelation = database.getRelation(TypeUtil.LABELLIST, null); //list with trajectory Id
    DBIDIter trIdIter = trIdRelation.iterDBIDs();
    for(DBIDIter triter = trRelation.iterDBIDs(); triter.valid(); triter.advance()) {
      DoubleVector transationVector = trRelation.get(triter);
      Coordinate positionCoordinate =  new Coordinate(transationVector.doubleValue(1), transationVector.doubleValue(2));
      long positionTimestamp = transationVector.longValue(0); //TODO: VERif (min debug)
      double speed = transationVector.doubleValue(3);
      String trajectoryId = trIdRelation.get(trIdIter).get(0);

      String mappedCell = grid.snapPointToCell(positionCoordinate);
      int mappedTimeslice = this.mapTimestampToSlice(positionTimestamp);

      if (mappedCell != null) { //border cells
        this.addSpeeds(mappedCell, mappedTimeslice, trajectoryId, speed);
      }
      //change of cell, timeslice or trajectory is considered processing the summaries at the end
      trIdIter.advance();
    }
  }

  int mapTimestampToSlice(long timestamp) {
    DateTime timestampDateTime = new DateTime(timestamp, DateTimeZone.UTC);
    return timestampDateTime.getSecondOfDay() / timeSliceLength;
  }

  private void addSpeeds(String cell, int timeslice, String trajectoryId, double speed) {
    CellSpeeds cellSpeeds = this.cellTimeSpeeds.get(cell);
    if (cellSpeeds == null) {
      cellSpeeds = new CellSpeeds(cell);
      this.cellTimeSpeeds.put(cell, cellSpeeds);
    }
    TimesliceSpeeds timesliceSpeeds = cellSpeeds.getTimesliceSpeeds(timeslice);
    TrajectorySpeeds trajectorySpeeds = timesliceSpeeds.getTrajectorySpeeds(trajectoryId);
    trajectorySpeeds.addSpeed(speed);
    cellSpeeds.addSpeed(speed);
  }

  public Map<String, Double> calculateCellsPerformanceIndex() {
    calculateTimeslicesMeanSpeed();
    calculateCellsAverageOperationSpeed();

    DescriptiveStatistics performanceIndexStats = new DescriptiveStatistics();
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
      cellSpeeds.calculateFreeFlowSpeed();
      performanceIndexStats.addValue(cellSpeeds.calculatePerfomanceIndex());
    }

    LOG.debug("performance index stats: ");
    LOG.debug(performanceIndexStats.toString());
    Map<String, Double> cellPerfomanceIndex = new HashMap<String, Double>();
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
        double normalizedPerfomanceIndex = cellSpeeds.normalizePerformanceIndex(performanceIndexStats.getMin(), performanceIndexStats.getMax());
        cellPerfomanceIndex.put(cellSpeeds.getCellId(), normalizedPerfomanceIndex);
    }
    return cellPerfomanceIndex;
  }

  private void calculateCellsAverageOperationSpeed() {
    List<String> cellsWithZeroOperationSpeed = new ArrayList<String>();
    for(CellSpeeds cellSpeeds : this.cellTimeSpeeds.values()) {
      double averageOperationSpeed = cellSpeeds.calculateAverageOperationSpeed();
      if (averageOperationSpeed <= 0) {
        cellsWithZeroOperationSpeed.add(cellSpeeds.getCellId());
      }
    }
    double cellsWithZeroOperationSpeedPercentage = ((double)cellsWithZeroOperationSpeed.size()/this.cellTimeSpeeds.size())*100;
    LOG.debug("total cells: " + this.cellTimeSpeeds.size());
    LOG.debug("removing with zero operational speed: " + cellsWithZeroOperationSpeed.size() + " ("+(cellsWithZeroOperationSpeedPercentage)+"%)");
    cellsWithZeroOperationSpeed.forEach(new Consumer<String>() {
      @Override
      public void accept(String cellId) {
         cellTimeSpeeds.remove(cellId);
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
