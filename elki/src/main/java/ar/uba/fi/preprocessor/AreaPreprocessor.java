package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.geometry.jts.ReferencedEnvelope;

import ar.uba.fi.visualization.geo.PointsVisualizer;

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
 *  - filter according to position and given area
 *  - params
 *    - "no-filter": only visualizes point with the given area
 *    - "viz": displays a visualization of the filtered dataset
 *
 * @author Mariano Kohan
 *
 */
public class AreaPreprocessor extends Preprocessor {

  private static final Logging LOG = Logging.getLogger(AreaPreprocessor.class);

  private PointsVisualizer pointsVisualizer;
  private double[] area;
  private Mode mode;

  private ReferencedEnvelope areaBox;

  public AreaPreprocessor(File trajectoriesFile, File roadNetworkFile, double[] area) {
    super(trajectoriesFile, roadNetworkFile);
    this.area = area;
  }

  public AreaPreprocessor(File trajectoriesFile, File roadNetworkFile, double[] area, Mode mode) {
    super(trajectoriesFile, roadNetworkFile);
    this.area = area;
    this.mode = mode;
  }

  protected String getPreprocessSubfix() {
    return "_processed_area";
  }

  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing area for file: " + trajectories);
    try {
      // Input format: T-Drive (User_guide_T-drive.pdf)
      // taxi id, date time, longitude, latitude
      // example: 1,2008-02-02 15:36:08,116.51172,39.92123
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //processed format: same (only filter according to position)
      FileWriter processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));

      int rowCounter = 0;
      int filteredRowCounter = 0;

      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(",");
        Coordinate positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        if (noFilterApplied()) {
          pointsVisualizer.add(positionCoordinate);
        } else {
          if (this.filterArea(positionCoordinate)) {
            filteredRowCounter++;
          } else {
            pointsVisualizer.add(positionCoordinate);
            processedTrajectories.write(trajectoryLine);
            processedTrajectories.write("\n");
          }
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

  private boolean noFilterApplied() {
    return Mode.NO_FILTER.equals(this.mode);
  }

  @Override
  protected void preprocessInit() {
    pointsVisualizer = new PointsVisualizer(this.roadNetwork, this.area);
    areaBox = this.roadNetwork.createBox(this.area);
  }

  @Override
  protected void preprocessEnd() {
    if (this.mode != null) {
      this.mode.displayPoints(pointsVisualizer);
    }
  }

  private boolean filterArea(Coordinate positionCoordinate) {
    return !areaBox.contains(positionCoordinate);
  }

  enum Mode {
    NO_FILTER {
      @Override
      public void displayPoints(PointsVisualizer pointsVisualizer) {
        pointsVisualizer.displayPointsWithArea();
      }
    },
    VIZ {
      @Override
      public void displayPoints(PointsVisualizer pointsVisualizer) {
       pointsVisualizer.displayPoints();
      }
    };

    public abstract void displayPoints(PointsVisualizer pointsVisualizer);

    public static Mode fromOption(String option) {
      String modeName = option.replace('-', '_').toUpperCase();
      return Mode.valueOf(modeName);
    }

  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);
    //limit x1, limit x2, limit y1, limit y2
    double[] area = { Double.parseDouble(args[2]),
        Double.parseDouble(args[3]),
        Double.parseDouble(args[4]),
        Double.parseDouble(args[5])
    };
    Mode filterMode = null;
    if (args.length >= 7) {
      filterMode = Mode.fromOption(args[6]);
    }

    new AreaPreprocessor(trajectoriesFile, roadNetworkFile, area, filterMode).preprocess();

  }


}
