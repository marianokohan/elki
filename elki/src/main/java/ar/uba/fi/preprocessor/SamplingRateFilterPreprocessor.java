package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 *  - filters trajectories according to is length
 *  - use data preprocessed by @{SamplingRateCutterPreprocessor}
 *  - params:
 *    - min trajectory length
 *
 * @author Mariano Kohan
 *
 */
public class SamplingRateFilterPreprocessor extends Preprocessor {

  private static final Logging LOG = Logging.getLogger(SamplingRateFilterPreprocessor.class);

  private long minTrajectoryLength;

  public SamplingRateFilterPreprocessor(File trajectoriesFile, File roadNetworkFile, long minTrajectoryLength) {
    super(trajectoriesFile, roadNetworkFile);
    this.minTrajectoryLength = minTrajectoryLength;
  }

  @Override
  protected String getPreprocessSubfix() {
    return "_proc-SR-F";
  }

  @Override
  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing sampling rate (cuts) for file: " + trajectories);
    try {
      // Input format from SamplingRateCutterPreprocessor
      // trajectory id (taxi id + sequential number), timestamp (in milliseconds), longitude, latitude
      //  additional for verification-> [, date time (original value from T-Drive), sampling rate (original value without considering cuts, in seconds), trajectory length]
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //same processed format - only filters rows
      FileWriter processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));;

      String trajectoryId = null, previousTrajectorytId = null;
      int trajectoryLength = 0;
      List<String> validTrajectoryFromLengths = new ArrayList<String>();

      //counts trajectories lengths
      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        String[] trajectoryElements = trajectoryLine.split(";");
        trajectoryId = trajectoryElements[0];
        if (previousTrajectorytId == null) {
          previousTrajectorytId = trajectoryId;
        }
        if (trajectoryId.equals(previousTrajectorytId)) {
          trajectoryLength++;
        } else {
          if (trajectoryLength >= this.minTrajectoryLength) {
            validTrajectoryFromLengths.add(previousTrajectorytId);
          }
          trajectoryLength = 0;
        }
        previousTrajectorytId = trajectoryId;
      }
      //last trajectoryId
      if (trajectoryLength >= this.minTrajectoryLength) {
        validTrajectoryFromLengths.add(trajectoryId);
      }


      int rowCounter = 0;
      int filteredRowCounter = 0;
      //filter rows according to length
      trajectoriesReader.close();
      trajectoriesReader = new BufferedReader(new FileReader(trajectories));
      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(";");
        trajectoryId = trajectoryElements[0];
        if (validTrajectoryFromLengths.contains(trajectoryId)) {
            processedTrajectories.write(trajectoryLine);
            processedTrajectories.write("\n");
        } else {
          filteredRowCounter++;
        }
      }

      trajectoriesReader.close();
      processedTrajectories.close();
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
  }

  @Override
  protected void preprocessEnd() throws Exception {
  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);
    String minTrajectoryLength = args[2];
    new SamplingRateFilterPreprocessor(trajectoriesFile, roadNetworkFile, Long.parseLong(minTrajectoryLength)).preprocess();
  }


}
