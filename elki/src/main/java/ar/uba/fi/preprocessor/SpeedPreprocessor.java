package ar.uba.fi.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
 *  - calculates speed
 *  - filter rows according to speed values <= 100 km/h (it is already >= 0 km/h)
 *  - validates timestamps order: next timestamp should be greater than previous one
 *      (to allow calculate speed)
 *
 * @author Mariano Kohan
 *
 */
public class SpeedPreprocessor extends Preprocessor {

  private static final int MAX_SPEED_THRESHOLD = 100;

  private static final Logging LOG = Logging.getLogger(SpeedPreprocessor.class);

  public SpeedPreprocessor(File trajectoriesFile, File roadNetworkFile) {
    super(trajectoriesFile, roadNetworkFile);
  }

  protected String getPreprocessSubfix() {
    return "_processed_speed";
  }

  protected void preprocessFile(File trajectories) {
    LOG.debug("preprocessing speed for file: " + trajectories);
    try {
      // Input format: T-Drive (User_guide_T-drive.pdf)
      // taxi id, date time, longitude, latitude
      // example: 1,2008-02-02 15:36:08,116.51172,39.92123
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));

      //processed format
      // trId; timestamp (in milliseconds); longitude; latitude (same as currently implemented); speed (in km/h)
      FileWriter processedTrajectories = new FileWriter(getPreprocessedFileName(trajectories));

      Coordinate previousPositionCoordinate = null;
      long previousTimestamp = -1;
      long timestamp = -1;
      double speed = 0;
      int rowCounter = 0;
      int filteredRowCounter = 0;

      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        rowCounter++;
        String[] trajectoryElements = trajectoryLine.split(",");

        StringBuffer processedTrajectoryRow = new StringBuffer(trajectoryElements[0]).append(";");
        Coordinate positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        if (previousPositionCoordinate == null) {
          previousPositionCoordinate = positionCoordinate;
        }
        timestamp = getTimestampMiliseconds(trajectoryElements[1]);
        if (timestamp > previousTimestamp) {
            speed = this.calculateSpeed(positionCoordinate, timestamp, previousPositionCoordinate, previousTimestamp);
            if (speed <= MAX_SPEED_THRESHOLD) {
              processedTrajectoryRow.append(timestamp).append(";");
              processedTrajectoryRow.append(trajectoryElements[2]).append(";");
              processedTrajectoryRow.append(trajectoryElements[3]).append(";");
              processedTrajectoryRow.append(String.format("%.5f", speed)).append("\n");

              processedTrajectories.write(processedTrajectoryRow.toString());
              previousPositionCoordinate = positionCoordinate;
              previousTimestamp = timestamp;
            } else {
              filteredRowCounter++;
            }
        } else {
          if (timestamp == previousTimestamp) {
            LOG.warning("repeated timestamp for " + trajectoryElements[0] + ": " +  previousTimestamp + " -> " + timestamp + " (" + trajectoryElements[1] + ")");
          } else {
            LOG.warning("small timestamp for " + trajectoryElements[0] + ": " +  previousTimestamp + " -> " + timestamp + " (" + trajectoryElements[1] + ")");
          }
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

   public long getTimestampMiliseconds(String dateTimeString) {
     //example format: '2008-02-02 15:36:08'
     //using joda-time because issue with std java classes
     String input = dateTimeString.replace( " ", "T" );
     DateTime dateTime = new DateTime( input, DateTimeZone.UTC );
     long millisecondsSinceUnixEpoch = dateTime.getMillis();
     return millisecondsSinceUnixEpoch;
   }

  private double calculateSpeed(Coordinate position, long timestamp, Coordinate previousPosition, long previousTimestamp) {
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
   */
  public static void main(String[] args) {
    //TODO: better handling of parameters
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);

    new SpeedPreprocessor(trajectoriesFile, roadNetworkFile).preprocess();

  }


}
