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

  private String cellId;
  private DescriptiveStatistics speedStats;
  private double freeFlowSpeed;
  private double performaceIndex;
  private double normalizedPerfomanceIndex;

  private double averageOperationSpeed;

  public CellSpeeds(String cellId) {
    this.cellId = cellId;
    this.timeslicesSpeeds = new HashMap<Integer, CellSpeeds.TimesliceSpeeds>();
    this.speedStats = new DescriptiveStatistics();
  }

  public String getCellId() {
    return this.cellId;
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
    this.normalizedPerfomanceIndex = ((performaceIndex - minIndex) / (maxIndex - minIndex)) * 100;
    return normalizedPerfomanceIndex;
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
