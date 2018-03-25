package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
 *  - cut trajectories according to calculated sampling interval
 *    - also validates timestamps order: next timestamp should be greater than previous one
 *    - length of trajectories dumped to file "trajectory_lengths.txt"
 *  - use data preprocessed by @{AreaPreprocessor}
 *  - params:
 *    - value of max sampling rate threshold (to cut trajectories),
 *    - or "sampling-rates": only calculates sampling rate into dump file (name "sampling_rates.txt")
 *
 * @author Mariano Kohan
 *
 */
public class SamplingRateCutterPreprocessor extends Preprocessor {

  public static final String SAMPLING_RATE_PARAM = "sampling-rates";

  private static final String TRAJECTORY_LENGTHS_DUMP_FILENAME = "trajectory_lengths.txt";
  private static final String TRAJECTORIES_BY_OBJECT_DUMP_FILENAME = "trajectories_by_object.txt";
  private static final String SAMPLING_RATES_DUMP_FILENAME = "sampling_rates.txt";

  private static final Logging LOG = Logging.getLogger(SamplingRateCutterPreprocessor.class);

  private boolean cutTrajectories;
  private long maxSamplingRate;

  private FileWriter samplingRates;
  private FileWriter trajectoryLengths;
  private FileWriter trajectoriesByObject;


  //only to generate "sampling_rates.txt"
  public SamplingRateCutterPreprocessor(File trajectoriesFile, File roadNetworkFile) {
    super(trajectoriesFile, roadNetworkFile);
    this.cutTrajectories = false;
  }

  //to cut trajectories
  public SamplingRateCutterPreprocessor(File trajectoriesFile, File roadNetworkFile, long maxSamplingRate) {
    super(trajectoriesFile, roadNetworkFile);
    this.cutTrajectories = true;
    this.maxSamplingRate = maxSamplingRate;
  }

  @Override
  protected String getPreprocessSubfix() {
    return "_proc-SR-C";
  }

  @Override
  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing sampling rate for file: " + trajectories);
    try {
      // Input format from AreaPreprocessor: T-Drive (User_guide_T-drive.pdf)
      // taxi id, date time, longitude, latitude
      // example: 1,2008-02-02 15:36:08,116.51172,39.92123
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //processed format:
      // trajectory id (taxi id + sequential number), timestamp (in milliseconds), longitude, latitude
      //  additional for verification-> [, date time (original value from T-Drive), sampling rate (original value without considering cuts, in seconds), trajectory length]
      FileWriter processedTrajectories = null;
      if (this.cutTrajectories) {
        processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));
      }

      int rowCounter = 0;
      int filteredRowCounter = 0;
      long timestamp = -1;
      long previousTimestamp = 0;
      long samplingRate = 0;
      String objectId, previousObjectId = null;
      int trajectoryCounter = 1, trajectoryLength = 0;

      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(",");
        objectId = trajectoryElements[0];
        if (previousObjectId == null) {
          previousObjectId = objectId;
        }
        timestamp = getTimestampMilliseconds(trajectoryElements[1]);
        if (timestamp > previousTimestamp) { //suppose objects on differents files => not consider object change (to avoid complex condition)
          samplingRate = this.calculateInterval(timestamp, previousTimestamp);
          if (this.cutTrajectories) {
            StringBuffer processedTrajectoryRow = new StringBuffer();
            if (objectId.equals(previousObjectId)) {
              if (samplingRate > this.maxSamplingRate) {
                trajectoryCounter = trajectoryCounter + 1;
                //samplingRate = 0;
                this.trajectoryLengths.append(String.format("%d", trajectoryLength));
                this.trajectoryLengths.append("\n");
                trajectoryLength = 1;
              } else {
                trajectoryLength = trajectoryLength + 1;
              }
            } else { //just in case trajectories for different objects are in the same file
              trajectoryCounter = 1;
              //samplingRate = 0;
              trajectoryLength = 1;
              previousTimestamp = 0;
            }
            processedTrajectoryRow.append(objectId).append("_").append(trajectoryCounter).append(";");
            processedTrajectoryRow.append(timestamp).append(";");
            processedTrajectoryRow.append(trajectoryElements[2]).append(";");
            processedTrajectoryRow.append(trajectoryElements[3]).append(";"); //.append("\n");
            //for verification of cuts (consider to comment is file size is an issue)
            processedTrajectoryRow.append(trajectoryElements[1]).append(";");
            processedTrajectoryRow.append(String.format("%d", samplingRate)).append(";");
            processedTrajectoryRow.append(trajectoryLength).append("\n");
            //for verification of cuts
            processedTrajectories.write(processedTrajectoryRow.toString());
          } else {
            samplingRates.append(String.format("%d", samplingRate));
            samplingRates.append("\n");
          }
        } else {
          if (timestamp == previousTimestamp) {
            LOG.warning("repeated timestamp for " + objectId + ": " +  previousTimestamp + " -> " + timestamp + " (" + trajectoryElements[1] + ")");
          } else {
            LOG.warning("small timestamp for " + objectId + ": " +  previousTimestamp + " -> " + timestamp + " (" + trajectoryElements[1] + ")");
          }
          filteredRowCounter++;
        }
        previousObjectId = objectId;
        previousTimestamp = timestamp;
      }
      if (this.cutTrajectories) {
        processedTrajectories.close();
        this.trajectoryLengths.append(String.format("%d", trajectoryLength));
        this.trajectoryLengths.append("\n");
        trajectoriesByObject.append(String.format("%d", trajectoryCounter));
        trajectoriesByObject.append("\n");
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
    if (this.cutTrajectories) {
      this.trajectoryLengths = new FileWriter(TRAJECTORY_LENGTHS_DUMP_FILENAME);
      this.trajectoriesByObject = new FileWriter(TRAJECTORIES_BY_OBJECT_DUMP_FILENAME);
    } else {
      this.samplingRates = new FileWriter(SAMPLING_RATES_DUMP_FILENAME);
    }

  }

  @Override
  protected void preprocessEnd() throws Exception {
    if (this.cutTrajectories) {
      this.trajectoryLengths.close();
      this.trajectoriesByObject.close();
    } else {
      this.samplingRates.close();
    }
  }

  protected long getTimestampMilliseconds(String dateTimeString) {
    //example format: '2008-02-02 15:36:08'
    //using joda-time because issue with std java classes
    String input = dateTimeString.replace( " ", "T" );
    DateTime dateTime = new DateTime( input, DateTimeZone.UTC );
    long millisecondsSinceUnixEpoch = dateTime.getMillis();
    return millisecondsSinceUnixEpoch;
  }

  /**
   * calculates interval in seconds
   * @param timestamp in milliseconds
   * @param previousTimestamp in milliseconds
   * @return
   */
  public long calculateInterval(long timestamp, long previousTimestamp) {
    if (previousTimestamp == 0) {
      return 0; //to recognize init trajectory values
    }
    return (timestamp - previousTimestamp)/1000;
  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);
    String samplingRate = args[2];
    SamplingRateCutterPreprocessor processor;
    if (SAMPLING_RATE_PARAM.equals(samplingRate)) {
      processor = new SamplingRateCutterPreprocessor(trajectoriesFile, roadNetworkFile);
    } else {
      processor = new SamplingRateCutterPreprocessor(trajectoriesFile, roadNetworkFile, Long.parseLong(samplingRate));
    }
    processor.preprocess();
  }


}
