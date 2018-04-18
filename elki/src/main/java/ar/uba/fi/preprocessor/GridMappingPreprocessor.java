package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.roadnetwork.RoadNetwork;

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
 *
 *  Preprocessor for T-Drive dataset
 *  - maps speed into a cell (indentified by its id attribute)
 *    - if a cell can not be mapped, it is filtered
 *  - map a timestamp into its interval index (starting from 0)
 *
 * @author Mariano Kohan
 *
 */
public class GridMappingPreprocessor extends Preprocessor {

  private static final Logging LOG = Logging.getLogger(GridMappingPreprocessor.class);

  public GridMappingPreprocessor(File trajectoriesFile, File roadNetworkFile, double[] area, double sideLen) {
    super(trajectoriesFile, roadNetworkFile);
    roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    roadNetwork.setGridMapping(area, sideLen);
  }

  @Override
  protected String getPreprocessSubfix() {
    return "_proc-GM";
  }

  @Override
  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing speed for file: " + trajectories);
    try {
      // Input format from SpeedPreprocessor
      // trajectory id (from sampling rate preprocessor); timestamp (in milliseconds); longitude; latitude; speed (in km/h)
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //processed format
      // trajectory id (from sampling rate preprocessor); timeslice index (start from 0); cell Id (feature attribute); speed (in km/h)
      FileWriter processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));

      long timestamp = -1;
      SimpleFeature mappedCell;

      String trajectoryId = null;
      int timeslice;
      String speed;

      int rowCounter = 0;
      int filteredRowCounter = 0;

      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(";");
        trajectoryId = trajectoryElements[0];
        speed = trajectoryElements[4];
        this.dumpRow(rowCounter, trajectoryId);

        timestamp = Long.valueOf(trajectoryElements[1]);
        timeslice = this.roadNetwork.getGridMapping().mapTimestampToSlice(timestamp);

        Coordinate positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        mappedCell = this.roadNetwork.getGridMapping().snapPointToCell(positionCoordinate);

        if (validCell(mappedCell)) {
            StringBuffer processedTrajectoryRow = new StringBuffer(trajectoryId).append(";");
            processedTrajectoryRow.append(timeslice).append(";");
            processedTrajectoryRow.append(mappedCell.getAttribute("id")).append(";");
            processedTrajectoryRow.append(speed).append("\n");

            processedTrajectories.write(processedTrajectoryRow.toString());
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

  private boolean validCell(SimpleFeature mappedCell) {
    return (mappedCell != null);
  }

  private void dumpRow(int row, String trajectoryId) {
    if (row % 10000 == 0) {
      LOG.debug(String.format("preprocessing row %d for trajectory %s ...", row, trajectoryId));
    }
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);
    //limit x1, limit x2, limit y1, limit y2
    double[] area = { Double.parseDouble(args[2]),
        Double.parseDouble(args[3]),
        Double.parseDouble(args[4]),
        Double.parseDouble(args[5])
    };
    double sideLen = Double.parseDouble(args[6]);

    new GridMappingPreprocessor(trajectoriesFile, roadNetworkFile, area, sideLen).preprocess();
  }

}
