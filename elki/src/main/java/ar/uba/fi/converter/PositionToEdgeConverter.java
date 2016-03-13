package ar.uba.fi.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.roadnetwork.RoadNetwork;

import com.vividsolutions.jts.geom.Coordinate;

//TODO: confirm license description
/*
 This file is developed to be used as part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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
 * @author mariano
 *
 */
public class PositionToEdgeConverter {

  private File trajectories;
  private RoadNetwork roadNetwork;

  public PositionToEdgeConverter(File trajectoriesFile, File roadNetworkFile) {
    this.trajectories = trajectoriesFile;
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
  }

  public void convertPositions() {
    try {
      FileWriter convertedTrajectories = new FileWriter(getConvertedTrajectoriesFileName());
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));
      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        // tr-id; datetime; lat; long (from Trucks denorm) --> no
        // --> correct format: tr-id (number only); datetime (format 'dd/MM/YYYYThh:mm:ss(Z)'; long (number format); lat (number format)
        // (example: https://www.google.com.ar/maps/place/37%C2%B059'00.3%22N+23%C2%B044'03.6%22E/@37.9834272,23.7321323,17z/data=!4m2!3m1!1s0x0:0x0)
        String[] trajectoryElements = trajectoryLine.split(";");
        Coordinate positionCoordinate =  new Coordinate( Double.parseDouble(trajectoryElements[2]), Double.parseDouble(trajectoryElements[3]));
        SimpleFeature edgeFeature = roadNetwork.snapPointToEdge(positionCoordinate);
        StringBuffer convertedTrajectory = new StringBuffer(trajectoryElements[0]).append(";");
        if (edgeFeature != null) { //in case the point could not be snapped to an edge
                                    // TODO: consider to review to avoid reduce on dataset size -> improve snap or another solution
          convertedTrajectory.append(getTimestampMiliseconds(trajectoryElements[1])).append(";");
          convertedTrajectory.append(filterPrefixFromEdgeFeatureId(edgeFeature.getID())).append(";");
          convertedTrajectory.append(trajectoryElements[2]).append(";");
          convertedTrajectory.append(trajectoryElements[3]).append("\n");
          convertedTrajectories.write(convertedTrajectory.toString());
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

   private long getTimestampMiliseconds(String dateTimeString) {
     //TOOD: to improve
     //DateFormat formatter = new SimpleDateFormat("dd/MM/YYYY'T'hh:mm:ss"); //Trucks
     DateFormat formatter = new SimpleDateFormat("dd/MM/YYYY'T'hh:mm:ssZ"); //crawdad/roma-taxi
     try {
      return formatter.parse(dateTimeString).getTime();
    }
    catch(ParseException parseException) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(parseException);
      return 0;
    }
   }

  private String getConvertedTrajectoriesFileName() {
    String trajectoriesFileName = trajectories.getAbsolutePath();
    if (trajectoriesFileName.charAt(trajectoriesFileName.length()-4) == '.' ) {
      return trajectoriesFileName.substring(0, trajectoriesFileName.length()-4) + "_converted_edges" + trajectoriesFileName.substring(trajectoriesFileName.length()-4);
    } else {
      return trajectoriesFileName + "_converted_edges";
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    //TODO: better handling of parameters
    File trajectoriesFile = new File(args[0]);
    File roadNetworkFile = new File(args[1]);

    new PositionToEdgeConverter(trajectoriesFile, roadNetworkFile).convertPositions();

  }

}
