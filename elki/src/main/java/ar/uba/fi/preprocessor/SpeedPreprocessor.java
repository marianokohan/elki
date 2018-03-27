package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

import de.lmu.ifi.dbs.elki.logging.Logging;

/*
 This file is developed to be used as part of ELKI:
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
 * Preprocessor for T-Drive dataset
 *  - calculates speed (in km/h)
 *  - filter rows according to speed values
 *  - params:
 *    - value of max speed to filter (it is already >= 0 km/h)
 *    - or "speeds": only calculates speeds values, dumped to file "speeds.txt"
 *
 * @author Mariano Kohan
 *
 */
public class SpeedPreprocessor extends Preprocessor {

  private static final String SPEEDS_PARAM = "speeds";
  private static final String SPEEDS_DUMP_FILENAME = "speeds.txt";

  private static final Logging LOG = Logging.getLogger(SpeedPreprocessor.class);

  private boolean filterSpeeds;
  private double maxSpeedThreshold;
  private FileWriter speeds;

  public SpeedPreprocessor(File trajectoriesFile, File roadNetworkFile) {
    super(trajectoriesFile, roadNetworkFile);
    this.filterSpeeds = false;
  }

  public SpeedPreprocessor(File trajectoriesFile, File roadNetworkFile, double maxSpeedThreshold) {
    super(trajectoriesFile, roadNetworkFile);
    this.filterSpeeds = true;
    this.maxSpeedThreshold = maxSpeedThreshold;
  }

  @Override
  protected String getPreprocessSubfix() {
    return "_proc-S";
  }

  @Override
  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing speed for file: " + trajectories);
    try {
      // Input format from SamplingRateFilterPreprocessor
      // trajectory id (taxi id + sequential number); timestamp (in milliseconds); longitude; latitude
      //  additional for verification-> [; date time (original value from T-Drive); sampling rate (original value without considering cuts, in seconds); trajectory length]
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //processed format
      // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); longitude; latitude; speed (in km/h)
      FileWriter processedTrajectories = null;
      if (this.filterSpeeds) {
        processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));
      }

      Coordinate previousPositionCoordinate = null;
      long previousTimestamp = -1;
      long timestamp = -1;
      double speed = 0;
      String trajectoryId = null, previousTrajectoryId = null;
      int rowCounter = 0;
      int filteredRowCounter = 0;

      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(";");
        trajectoryId = trajectoryElements[0];
        Coordinate positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        if (previousPositionCoordinate == null) {
          previousPositionCoordinate = positionCoordinate;
        }
        timestamp = Long.valueOf(trajectoryElements[1]);
        if (!trajectoryId.equals(previousTrajectoryId)) {
          previousTimestamp = -1;
        }
        speed = this.calculateSpeed(positionCoordinate, timestamp, previousPositionCoordinate, previousTimestamp);
        if (this.filterSpeeds) {
          if (validSpeed(speed)) {
            StringBuffer processedTrajectoryRow = new StringBuffer(trajectoryId).append(";");
            processedTrajectoryRow.append(timestamp).append(";");
            processedTrajectoryRow.append(trajectoryElements[2]).append(";");
            processedTrajectoryRow.append(trajectoryElements[3]).append(";");
            processedTrajectoryRow.append(String.format("%.5f", speed)).append("\n");

            processedTrajectories.write(processedTrajectoryRow.toString());
            //speed calculated with respect to last valid position to avoid calculate with incorrect positions
            // in case big difference is observed, consider to calculate always with respect to last position (marked bellow)
            previousPositionCoordinate = positionCoordinate;
            previousTimestamp = timestamp;
            previousTrajectoryId = trajectoryId;
          } else {
            filteredRowCounter++;
          }
          /*
          //uncomment to calculated speed with respect to last position (referenced up)
          previousPositionCoordinate = positionCoordinate;
          previousTimestamp = timestamp;
          */
        } else {
          speeds.append(String.format("%.5f", speed));
          speeds.append("\n");
          previousPositionCoordinate = positionCoordinate;
          previousTimestamp = timestamp;
          previousTrajectoryId = trajectoryId;
        }
      }

      if (this.filterSpeeds) {
        processedTrajectories.close();
      }
      trajectoriesReader.close();
      this.updateRowCounter(rowCounter);
      this.updateFilteredRowCounter(filteredRowCounter);
      LOG.debug("Filtered " + filteredRowCounter + " rows from " + rowCounter + " (" + (float)filteredRowCounter/rowCounter * 100 + "%).");
    }
      catch(FileNotFoundException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
      catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
  }

  @Override
  protected void preprocessInit()  throws Exception {
    if (!this.filterSpeeds) {
      this.speeds = new FileWriter(SPEEDS_DUMP_FILENAME);
    }
  }

  @Override
  protected void preprocessEnd() throws Exception {
    if (!this.filterSpeeds) {
      this.speeds.close();
    }
  }

  private boolean validSpeed(double speed) {
    return (speed <= this.maxSpeedThreshold);
  }

  private double calculateSpeed(Coordinate position, long timestamp, Coordinate previousPosition, long previousTimestamp) {
    if (previousTimestamp < 0) { //no previous position => speed = 0
      return 0;
    }
    double velocity = 0;
    double distante;
    long time;
    try {
      distante = this.roadNetwork.calculateDistance(previousPosition, position);
      time = timestamp - previousTimestamp;
      velocity = (distante * 1000 * 60 * 60 ) / (time * 1000);
    }
    catch(TransformException e) {
      LOG.warning("could not calculate distance for positions: " + position + ", " + previousPosition);
    }
    return velocity;
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);
    String speed = args[2];
    SpeedPreprocessor preprocessor;
    if (SPEEDS_PARAM.equals(speed)) {
      preprocessor = new SpeedPreprocessor(trajectoriesFile, roadNetworkFile);
    } else {
      preprocessor = new SpeedPreprocessor(trajectoriesFile, roadNetworkFile, Double.parseDouble(speed));
    }
    preprocessor.preprocess();
  }

}
