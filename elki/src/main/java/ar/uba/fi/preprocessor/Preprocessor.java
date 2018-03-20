package ar.uba.fi.preprocessor;

import java.io.File;
import java.io.FilenameFilter;

import ar.uba.fi.roadnetwork.RoadNetwork;
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
 * Base preprocessor for T-Drive dataset
 *
 * @author Mariano Kohan
 *
 */
public abstract class Preprocessor {

  protected File trajectories;
  protected RoadNetwork roadNetwork;

  private int rowCounter;
  private int filteredRowCounter;
  private static final Logging LOG = Logging.getLogger(SpeedPreprocessor.class);

  public Preprocessor(File trajectoriesFile, File roadNetworkFile) {
    this.trajectories = trajectoriesFile;
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
  }

  public void preprocess() {
    this.preprocessInit();
    this.rowCounter = 0;
    this.filteredRowCounter = 0;
    if (this.trajectories.isDirectory()) {
      final String preprocessSubfix = this.getPreprocessSubfix();
      File[] trajectoriesFiles = this.trajectories.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          if ( name.endsWith(".txt") && !name.endsWith( preprocessSubfix + ".txt") ) {
            return true;
          }
          return false;
        }
      });
      for(int i = 0; i < trajectoriesFiles.length; i++) {
        File trajectoryFile = trajectoriesFiles[i];
        if (trajectoryFile.isFile()) {
          this.preprocessFile(trajectoryFile);
        }
      }
    } else {
      this.preprocessFile(this.trajectories);
    }
    LOG.debug("Total filtered " + filteredRowCounter + " rows from " + rowCounter + " (" + (float)filteredRowCounter/rowCounter * 100 + "%).");
    this.preprocessEnd();
  }

  protected void preprocessInit() {
    // nothing for base class
  }

  protected void preprocessEnd() {
    // nothing for base class
  }

  protected String getPreprocessedFileName(File trajectories) {
    String trajectoriesFileName = trajectories.getAbsolutePath();
    if (trajectoriesFileName.charAt(trajectoriesFileName.length()-4) == '.' ) {
      return trajectoriesFileName.substring(0, trajectoriesFileName.length()-4) + this.getPreprocessSubfix() + trajectoriesFileName.substring(trajectoriesFileName.length()-4);
    } else {
      return trajectoriesFileName + this.getPreprocessSubfix();
    }
  }

  protected void updateRowCounter(int rows) {
    this.rowCounter += rows;
  }

  protected void updateFilteredRowCounter(int filteredRows) {
    this.filteredRowCounter += filteredRows;
  }

  protected abstract String getPreprocessSubfix();

  protected abstract void preprocessFile(File trajectories);

}
