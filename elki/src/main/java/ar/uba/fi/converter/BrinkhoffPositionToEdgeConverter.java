package ar.uba.fi.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileHeader;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.roadnetwork.RoadNetwork;

import com.vividsolutions.jts.geom.Coordinate;

import de.lmu.ifi.dbs.elki.logging.Logging;

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
 * Class to convert positions generated from Brinkhoff and snap into edges from shapefile
 *
 * @author Mariano Kohan
 *
 */
public class BrinkhoffPositionToEdgeConverter {

  private static final Logging LOG = Logging.getLogger(BrinkhoffPositionToEdgeConverter.class);

  private static final String TIMESTAMP_PREFIX = "t";
  private static final String TRANSACTION_ID_PREFIX = "Tr_";
  private File trajectories;
  private RoadNetwork roadNetwork;

  /**
   * map limits (from ShapeNetworkFileManager)
   */
  // minimum x-coordinate
  protected double minX;
  // minimum y-coordinate of map.
  protected double maxY;
  // x-extend of map.
  private double dX;
  // y-extend map.
  private double dY;

  /*
   * speed stats values
   */
  private double minSpeed;
  private double maxSpeed;
  private double speedSum;
  private double speedCount;

  /**
   * The resolution of the network map (from ShapeNetworkFileManager)
   */
  protected int resolution = 30000;

  public BrinkhoffPositionToEdgeConverter(File trajectoriesFile, File roadNetworkFile) throws ShapefileException, MalformedURLException, IOException {
    this.trajectories = trajectoriesFile;
    this.roadNetwork = RoadNetwork.getInstance(roadNetworkFile);
    ShapefileReader shapefileReader = new ShapefileReader(new ShpFiles(roadNetworkFile), true, true, null);
    ShapefileHeader shapefileHeader = shapefileReader.getHeader();
    minX = shapefileHeader.minX();
    maxY = shapefileHeader.maxY();
    dX = shapefileHeader.maxX()-minX;
    dY = maxY-shapefileHeader.minY();
    shapefileReader.close();
    this.minSpeed = -1;
    this.maxSpeed = 0;
    this.speedSum = 0;
    this.speedCount = 0;
  }

  public void convertPositions() {
    int notSnapedPositions = 0;
    try {
      FileWriter convertedTrajectories = new FileWriter(getConvertedTrajectoriesFileName());
      BufferedReader trajectoriesReader = new BufferedReader(new FileReader(trajectories));
      for(String trajectoryLine; (trajectoryLine = trajectoriesReader.readLine()) != null; ) {
        /*
         * format ( from 'downloads/ControllingTheGenerator.pdf')
         *  • newpoint (for the first position of a new moving object), point (for the following positions
            of a moving object), or disappearpoint (if a moving object has reached its destination)
            • the id of the point
            • the sequence number (starts with 1)
            • the id of the object class
            • the time stamp (as integer)
            • the x-coordinate (as floating-point number)
            • the y-coordinate (as floating-point number)
            • the current speed (in space units per time unit as a floating-point number; note: the speed may
            change several times between two reported positions)
            • the x-coordinate of the next node that will be passed (as integer)
            • the y-coordinate of the next node that will be passed (as integer)
         */
        String[] trajectoryElements = trajectoryLine.split("\t");
        double longitude = convertXtoLongitude(Double.parseDouble(trajectoryElements[5]));
        double latitude = convertYtoLatitute(Double.parseDouble(trajectoryElements[6]));
        Coordinate positionCoordinate =  new Coordinate( longitude, latitude);
        SimpleFeature edgeFeature = roadNetwork.snapPointToEdge(positionCoordinate);
        NumberFormat formatter = new DecimalFormat("#0.00000");
        String speed = trajectoryElements[7];
        //converted format
        // trId; timestamp; edgeId; longitude; latitude (same as currently implemented); speed (same from file)
        StringBuffer convertedTrajectory = new StringBuffer(trajectoryElements[1]).append(";");
        if (edgeFeature == null) { //in case the point could not be snapped to an edge
                                    // TODO: consider to review to avoid reduce on dataset size -> improve snap or another solution
          LOG.debug("could not snap position (" + longitude + ", " + latitude + ") (long, lat) to edge");
          notSnapedPositions++;
        } else {
          convertedTrajectory.append(trajectoryElements[4]).append(";");
          convertedTrajectory.append(filterPrefixFromEdgeFeatureId(edgeFeature.getID())).append(";");
          convertedTrajectory.append(formatter.format(longitude)).append(";");
          convertedTrajectory.append(formatter.format(latitude)).append(";");
          convertedTrajectory.append(speed).append("\n");
          convertedTrajectories.write(convertedTrajectory.toString());
          this.calculateSpeedStats(speed);
        }
      }
      convertedTrajectories.close();
      trajectoriesReader.close();
      LOG.debug("could not snap " + notSnapedPositions +  " positions to  an edge");
      this.printSpeedStats();
    }
      catch(FileNotFoundException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
      catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
  }

  private void calculateSpeedStats(String speedString) {
    double speed = Double.parseDouble(speedString);

    if (speed > 0) {
      if (this.minSpeed == -1) {
        this.minSpeed = speed;
      } else {
        if (speed < this.minSpeed) {
          this.minSpeed = speed;
        }
      }

      if (this.maxSpeed == 0) {
        this.maxSpeed = speed;
      } else {
        if (speed > this.maxSpeed) {
          this.maxSpeed = speed;
        }
      }
    }

    this.speedSum += speed;
    this.speedCount++;
  }

  private void printSpeedStats() {
    StringBuffer speedStats = new StringBuffer("\nSpeed values: \n-------------");
    speedStats.append("\n").append("min: ").append(this.minSpeed);
    speedStats.append("\n").append("max: ").append(this.maxSpeed);
    speedStats.append("\n").append("mean: ").append(this.speedSum/this.speedCount);
    speedStats.append("\n");
    System.out.println(speedStats);
  }

  //Converts the positive x-value into a longitude
  //opposite from 'intoX()'
  private double convertXtoLongitude (double xValue) {
    return (xValue * dX/resolution) + minX;
  }

  //Converts the positive y-value into a latitude
  //opposite from 'intoY()'
  private double convertYtoLatitute (double yValue) {
    return maxY - (yValue * dY/resolution);
  }

  public static String filterPrefixFromEdgeFeatureId(String edgeFeatureID) {
     int prefixSeparatorPosition = edgeFeatureID.lastIndexOf(".");
     if (prefixSeparatorPosition > 0) {
       return edgeFeatureID.substring(prefixSeparatorPosition + 1);
     } else {
       return edgeFeatureID;
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

    try {
      new BrinkhoffPositionToEdgeConverter(trajectoriesFile, roadNetworkFile).convertPositions();
    }
    catch(ShapefileException e) {
      // TODO Auto-generated catch block
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    catch(MalformedURLException e) {
      // TODO Auto-generated catch block
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    catch(IOException e) {
      // TODO Auto-generated catch block
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }

  }

}
