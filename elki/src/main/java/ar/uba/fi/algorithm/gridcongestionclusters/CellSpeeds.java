package ar.uba.fi.algorithm.gridcongestionclusters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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

import org.opengis.feature.simple.SimpleFeature;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * speed values for a grid's cell
 *
 * @author mariano kohan
 *
 */
public class CellSpeeds {

  private static final Logging LOG = Logging.getLogger(CellSpeeds.class);

  private Map<Integer,TimesliceSpeeds> timeslicesSpeeds;

  private SimpleFeature cell;
  private Integer cellId;
  private DescriptiveStatistics speedStats;
  private double freeFlowSpeed;
  private double performaceIndex;
  private double normalizedPerformanceIndex;

  private double averageOperationSpeed;

  public CellSpeeds(SimpleFeature cell) {
    this.cell = cell;
    this.timeslicesSpeeds = new HashMap<Integer, CellSpeeds.TimesliceSpeeds>();
    this.speedStats = new DescriptiveStatistics();
  }

  public CellSpeeds(Integer cellId) {
    this.cellId = cellId;
    this.timeslicesSpeeds = new HashMap<Integer, CellSpeeds.TimesliceSpeeds>();
    this.speedStats = new DescriptiveStatistics();
  }

  public String getCellId() {
    return this.cell.getID();
  }

  public Integer getCellAttributeId() {
    if (cell != null) {
      return (Integer)cell.getAttribute("id");
    } else {
      return this.cellId;
    }
  }

  public TimesliceSpeeds getTimesliceSpeeds(int timeslice) {
    TimesliceSpeeds timesliceSpeeds = this.timeslicesSpeeds.get(timeslice);
    if (timesliceSpeeds == null) {
      timesliceSpeeds = new TimesliceSpeeds();
      timeslicesSpeeds.put(timeslice, timesliceSpeeds);
    }
    return timesliceSpeeds;
  }

  public Collection<TimesliceSpeeds> getTimeslicesSpeeds() {
    return this.timeslicesSpeeds.values();
  }

  public void addSpeed(double speed) {
    this.speedStats.addValue(speed);
  }

  public void calculateFreeFlowSpeed() {
    this.freeFlowSpeed = this.speedStats.getPercentile(95);
    if (Double.isNaN(freeFlowSpeed)) {
      LOG.warning("cell with free flow speed NaN!");
    }
  }

  public double calculateAverageOperationSpeed() {
    averageOperationSpeed = 0;
    for(TimesliceSpeeds timesliceSpeeds : this.timeslicesSpeeds.values()) {
      averageOperationSpeed += timesliceSpeeds.getMean();
    }
    if (Double.isNaN(averageOperationSpeed)) {
      LOG.warning("cell with average operation speed NaN!");
    }
    return averageOperationSpeed;
  }

  public double calculatePerfomanceIndex() {
    this.performaceIndex = this.freeFlowSpeed / averageOperationSpeed;
    if (Double.isNaN(performaceIndex)) {
      LOG.warning("cell with performance index speed NaN: " + freeFlowSpeed + "/" + averageOperationSpeed);
    }
    return this.performaceIndex;
  }

  public double normalizePerformanceIndex(double minIndex, double maxIndex) {
    if (minIndex == maxIndex) {
      this.normalizedPerformanceIndex = performaceIndex;
    } else {
      this.normalizedPerformanceIndex = ((performaceIndex - minIndex) / (maxIndex - minIndex)) * 100;
    }
    return normalizedPerformanceIndex;
  }

  public double getPerformanceIndex() {
    return this.normalizedPerformanceIndex;
  }

  /*
   * build dump to be used by adapted DBSCAN
   * (-> id attribute, minX, maxX, miny, maxY, (normalized) performanceIndex[, original performance index])
   * --> id attribute, (normalized) performanceIndex
   */
  public String toDumpString() {
    StringBuffer cellDump = new StringBuffer();
    cellDump.append(getCellAttributeId()).append(";");
    cellDump.append(normalizedPerformanceIndex).append(";");
    return cellDump.toString();
  }

  public class TimesliceSpeeds {

    private Map<String,TrajectorySpeeds> trajectoriesSpeeds;
    private DescriptiveStatistics speedStats;
    private double meanSpeed;

    public TimesliceSpeeds() {
      this.trajectoriesSpeeds = new HashMap<String, CellSpeeds.TrajectorySpeeds>();
    }

    public double getMean() {
      return this.meanSpeed;
    }

    public TrajectorySpeeds getTrajectorySpeeds(String trajectoryId) {
      TrajectorySpeeds trajectorySpeeds = trajectoriesSpeeds.get(trajectoryId);
      if (trajectorySpeeds == null) {
        trajectorySpeeds = new TrajectorySpeeds();
        trajectoriesSpeeds.put(trajectoryId, trajectorySpeeds);
      }
      return trajectorySpeeds;
    }

    public Collection<TrajectorySpeeds> getTrajectoriesSpeeds() {
      return this.trajectoriesSpeeds.values();
    }

    public void calculateMean() {
      this.speedStats = new DescriptiveStatistics();
      for(TrajectorySpeeds trajectorySpeed : this.trajectoriesSpeeds.values()) {
        this.speedStats.addValue(trajectorySpeed.getMean());
      }
      this.meanSpeed = this.speedStats.getMean();
      if (Double.isNaN(meanSpeed)) {
        LOG.warning("timeslice with mean speed NaN");
      }
    }

  }

  //list of speed values for a moving object trajectory inside a cell and timestamp
  public class TrajectorySpeeds {
      private DescriptiveStatistics speedStats;
      private double meanSpeed;

      public TrajectorySpeeds() {
        this.speedStats = new DescriptiveStatistics();
      }

      public void addSpeed(double speed) {
        this.speedStats.addValue(speed);
      }

      public void calculateMean() {
        this.meanSpeed = this.speedStats.getMean();
        if (Double.isNaN(meanSpeed)) {
          LOG.warning("trajectory with mean speed NaN!");
        }
      }

      public double getMean() {
        return this.meanSpeed;
      }
  }

}
