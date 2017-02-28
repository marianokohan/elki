package ar.uba.fi.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.operation.TransformException;

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
 * Class to convert positions from T-Drive dataset
 *  - map-matching
 *  - velocity calculation
 *  - filtering: timestamp ordering, velocity max value
 *
 * @author Mariano Kohan
 *
 */
public class TDrivePositionToEdgeConverter {

  private static final int MAX_VELOCITY_THRESHOLD = 200;

  private static final Logging LOG = Logging.getLogger(TDrivePositionToEdgeConverter.class);

  private File trajectories;
  private RoadNetwork roadNetwork;

  public TDrivePositionToEdgeConverter(File trajectoriesFile, File roadNetworkFile) {
    this.trajectories = trajectoriesFile;
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
  }

  public void convertPositions() {
    if (this.trajectories.isDirectory()) {
      File[] trajectoriesFiles = this.trajectories.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          if ( name.endsWith(".txt") && !name.endsWith("_converted_edges.txt") ) {
            return true;
          }
          return false;
        }
      });
      for(int i = 0; i < trajectoriesFiles.length; i++) {
        File trajectoryFile = trajectoriesFiles[i];
        if (trajectoryFile.isFile()) {
          this.convertPositions(trajectoryFile);
        }
      }
    } else {
      this.convertPositions(this.trajectories);
    }
  }

  public void convertPositions(File trajectories) {
    LOG.debug("converting positions for file: " + trajectories);
    try {
      FileWriter convertedTrajectories = new FileWriter(getConvertedTrajectoriesFileName(trajectories));
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));
      Coordinate previousPositionCoordinate = null;
      long previousTimestamp = -1;
      long timestamp = -1;
      double velocity = 0;
      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        // T-Drive format (User_guide_T-drive.pdf)
        // taxi id, date time, longitude, latitude
        // example: 1,2008-02-02 15:36:08,116.51172,39.92123
        String[] trajectoryElements = trajectoryLine.split(",");
        Coordinate positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        SimpleFeature edgeFeature = roadNetwork.snapPointToEdge(positionCoordinate);
        //converted format
        // trId; timestamp; edgeId; longitude; latitude (same as currently implemented); speed (calculated from file - TODO)
        StringBuffer convertedTrajectory = new StringBuffer(trajectoryElements[0]).append(";");
        if (edgeFeature != null) {
          if (previousPositionCoordinate == null) {
            previousPositionCoordinate = positionCoordinate;
          }
          timestamp = getTimestampMiliseconds(trajectoryElements[1]);
          if (timestamp > previousTimestamp) {
              velocity = this.calculateVelocity(positionCoordinate, timestamp, previousPositionCoordinate, previousTimestamp);
              if (velocity <= MAX_VELOCITY_THRESHOLD) {
                convertedTrajectory.append(timestamp).append(";");
                convertedTrajectory.append(filterPrefixFromEdgeFeatureId(edgeFeature.getID())).append(";");
                convertedTrajectory.append(trajectoryElements[2]).append(";");
                convertedTrajectory.append(trajectoryElements[3]).append(";");
                convertedTrajectory.append(String.format("%.5f", velocity)).append("\n");
                convertedTrajectories.write(convertedTrajectory.toString());
                previousPositionCoordinate = positionCoordinate;
                previousTimestamp = timestamp;
              }
          } else {
            if (timestamp == previousTimestamp) {
              LOG.warning("repeated timestamp for " + trajectoryElements[0] + ": " +  previousTimestamp + " -> " + timestamp + "(" + trajectoryElements[1] + ")");
            } else {
              LOG.warning("small timestamp for " + trajectoryElements[0] + ": " +  previousTimestamp + " -> " + timestamp + "(" + trajectoryElements[1] + ")");
            }
          }
        }
      }
      convertedTrajectories.close();
      trajectoriesReader.close();
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

   public long getTimestampMiliseconds(String dateTimeString) {
     //example format: '2008-02-02 15:36:08'
     //using joda-time because issue with std java classes
     String input = dateTimeString.replace( " ", "T" );
     DateTime dateTime = new DateTime( input, DateTimeZone.UTC );
     long millisecondsSinceUnixEpoch = dateTime.getMillis();
     return millisecondsSinceUnixEpoch;
   }

  private String getConvertedTrajectoriesFileName(File trajectories) {
    String trajectoriesFileName = trajectories.getAbsolutePath();
    if (trajectoriesFileName.charAt(trajectoriesFileName.length()-4) == '.' ) {
      return trajectoriesFileName.substring(0, trajectoriesFileName.length()-4) + "_converted_edges" + trajectoriesFileName.substring(trajectoriesFileName.length()-4);
    } else {
      return trajectoriesFileName + "_converted_edges";
    }
  }

  private double calculateVelocity(Coordinate position, long timestamp, Coordinate previousPosition, long previousTimestamp) {
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

    new TDrivePositionToEdgeConverter(trajectoriesFile, roadNetworkFile).convertPositions();

    /* prueba problema calculo timestamp con std java */
    /*
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);

    TDrivePositionToEdgeConverter c = new TDrivePositionToEdgeConverter(trajectoriesFile, roadNetworkFile);

    String d1 = "2008-02-02 23:59:33";
    long t1 = c.getTimestampMiliseconds(d1);
    String d2 = "2008-02-03 00:04:35";
    long t2 = c.getTimestampMiliseconds(d2);
    String d3 = "2008-02-03 00:59:56";
    long t3 = c.getTimestampMiliseconds(d3);
    String d4 = "2008-02-03 01:04:58";
    long t4 = c.getTimestampMiliseconds(d4);
    System.out.println("date 1: " + d1 + " -> " + t1);
    System.out.println("date 2: " + d2 + " -> " + t2);
    System.out.println("t2 > t1 ? " + (t2 > t1));
    System.out.println("date 3: " + d3 + " -> " + t3);
    System.out.println("date 4: " + d4 + " -> " + t4);
    System.out.println("t4 > t3 ? " + (t4 > t3));
    */
  }

}
