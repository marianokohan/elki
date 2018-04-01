package ar.uba.fi.visualization.geo;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.MapContent;
import org.geotools.swing.JMapFrame;

import ar.uba.fi.roadnetwork.RoadNetwork;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2018
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

public class PointsVisualizer extends MapVisualizer {

  private static final Color POINT_COLOR = new Color(204, 197, 0);
  private static final Color AREA_COLOR = new Color(50, 110, 230);

  private RoadNetwork roadNetwork;
  private List<Point> points;
  Coordinate[] areaCoordinates;

  public PointsVisualizer(RoadNetwork roadNetwork) {
    this.roadNetwork = roadNetwork;
    this.points = new LinkedList<Point>();
  }

  public PointsVisualizer(RoadNetwork roadNetwork, double[] area) {
    this.roadNetwork = roadNetwork;
    this.points = new LinkedList<Point>();
    this.createAreaCoordinates(area);
  }

  private void createAreaCoordinates(double[] area) {
    /*
     * coordinates defined on (Liu et. al., 2017)
     *    * 116.210316, 40.0257582
          * 116.550610, 40.0257582
          * 116.550610, 35.754980 -> 116.550610, 39.754980
          * 116.210316, 35.754980 -> 16.210316, 39.754980
          * 116.210316, 40.0257582

    Coordinate[] coordinates = { new Coordinate(116.210316, 40.0257582),
        new Coordinate(116.550610, 40.0257582),
        new Coordinate(116.550610, 39.754980),
        new Coordinate(116.210316, 39.754980),
        new Coordinate(116.210316, 40.0257582) };
    */
    /*
    //adjustment to consider core area of Beijing = "...region with the fifth ring road Five Rings"
    Coordinate[] coordinates = { new Coordinate(116.20, 40.0257582),
        new Coordinate(116.545, 40.0257582),
        new Coordinate(116.545, 39.754980),
        new Coordinate(116.20, 39.754980),
        new Coordinate(116.20, 40.0257582) };
    */

    this.areaCoordinates = new Coordinate[5];
    //limit x1, limit x2, limit y1, limit y2
    // 116.20 116.545 40.0257582 39.754980
    this.areaCoordinates[0] = new Coordinate(area[0], area[2]);
    this.areaCoordinates[1] = new Coordinate(area[1], area[2]);
    this.areaCoordinates[2] = new Coordinate(area[1], area[3]);
    this.areaCoordinates[3] = new Coordinate(area[0], area[3]);
    this.areaCoordinates[4] = new Coordinate(area[0], area[2]);
  }

  public void add(Coordinate coordinate) {
    this.points.add(this.createPoint(coordinate));
  }

  public void displayPoints() {
    SimpleFeatureSource featureSource = this.roadNetwork.getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle("Points");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createPointsLayer(this.createPointFeatureCollection(this.points), featureSource, PointPositionType.START, POINT_COLOR, 3));

    JMapFrame.showMap(map);
  }

  public void displayPointsWithArea() {
    SimpleFeatureSource featureSource = this.roadNetwork.getRoadsFeatureSource();

    MapContent map = new MapContent();
    map.setTitle("Points");

    map.addLayer(createRoadNetworkLayer(featureSource));
    map.addLayer(createPointsLayer(this.createPointFeatureCollection(this.points), featureSource, PointPositionType.START, POINT_COLOR, 3));
    map.addLayer(createPolygonLayer(this.createPolygonFeatureCollection(this.createAreaPolygon()), featureSource, AREA_COLOR, 3));

    JMapFrame.showMap(map);
  }

  protected Polygon createAreaPolygon() {
    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    Polygon polygon = geometryFactory.createPolygon(this.areaCoordinates);
    return polygon;
  }

}
