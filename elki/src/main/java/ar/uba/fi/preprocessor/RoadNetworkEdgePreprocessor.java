package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.opengis.feature.simple.SimpleFeature;

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
 *  - applies map-matching to positions
 *    - filter positions that could not be map-matched
 *
 * @author Mariano Kohan
 *
 */
public class RoadNetworkEdgePreprocessor extends Preprocessor {

  private static final Logging LOG = Logging.getLogger(RoadNetworkEdgePreprocessor.class);

  public RoadNetworkEdgePreprocessor(File trajectoriesFile, File roadNetworkFile) {
    super(trajectoriesFile, roadNetworkFile);
  }

  @Override
  protected String getPreprocessSubfix() {
    return "_proc-RNE";
  }

  @Override
  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing map-matching for file: " + trajectories);
    try {
      // Input format from SpeedPreprocessor
      // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); longitude; latitude; speed (in km/h)
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //processed format
      // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); edgeId; longitude; latitude; speed (in km/h)
      FileWriter processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));

      String trajectoryId = null;
      Coordinate positionCoordinate = null;
      int rowCounter = 0;
      int filteredRowCounter = 0;

      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(";");
        trajectoryId = trajectoryElements[0];

        positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        SimpleFeature edgeFeature = roadNetwork.snapPointToEdge(positionCoordinate);
        if (edgeFeature != null) {
          StringBuffer processedTrajectoryRow = new StringBuffer(trajectoryId).append(";");
          processedTrajectoryRow.append(trajectoryElements[1]).append(";");
          processedTrajectoryRow.append(filterPrefixFromEdgeFeatureId(edgeFeature.getID())).append(";");
          processedTrajectoryRow.append(trajectoryElements[2]).append(";");
          processedTrajectoryRow.append(trajectoryElements[3]).append(";");
          processedTrajectoryRow.append(trajectoryElements[4]).append("\n");

          processedTrajectories.write(processedTrajectoryRow.toString());
        } else {
          filteredRowCounter++;
        }

      }

      processedTrajectories.close();
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

  private String filterPrefixFromEdgeFeatureId(String edgeFeatureID) {
    int prefixSeparatorPosition = edgeFeatureID.lastIndexOf(".");
    if (prefixSeparatorPosition > 0) {
      return edgeFeatureID.substring(prefixSeparatorPosition + 1);
    } else {
      return edgeFeatureID;
    }
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);

    new RoadNetworkEdgePreprocessor(trajectoriesFile, roadNetworkFile).preprocess();
  }

}
