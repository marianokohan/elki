package ar.uba.fi.roadnetwork;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.graph.build.GraphGenerator;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.DirectedLineStringGraphGenerator;
import org.geotools.graph.structure.Graph;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

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
 * @author Mariano Kohan
 *
 */
public class RoadNetwork {

  private final SpatialIndex index = new STRtree();
  private double MAX_SEARCH_DISTANCE = 1;
  private FileDataStore dataStore;
  private Graph roadNetworkGraph;
  int processedEdges;
  private String edgeFeaturePrefix;

  private static Map<File, RoadNetwork> roadNetworkInstanceMap;

  public static RoadNetwork getInstance(File roadNetworkFile) {
    if (roadNetworkInstanceMap == null) {
      roadNetworkInstanceMap = new HashMap<File, RoadNetwork>();
    }
    RoadNetwork roadNetworkInstance = roadNetworkInstanceMap.get(roadNetworkFile);
    if (roadNetworkInstance == null) {
      roadNetworkInstance = new RoadNetwork(roadNetworkFile);
      roadNetworkInstanceMap.put(roadNetworkFile, roadNetworkInstance);
    }
    return roadNetworkInstance;
  }

  private RoadNetwork(File roadNetworkFile) {
    SimpleFeatureSource featureSource = null;
    FeatureCollection features = null;
    try {
      dataStore = FileDataStoreFinder.getDataStore(roadNetworkFile);
      featureSource = dataStore.getFeatureSource();
      features = featureSource.getFeatures();
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }

    /*
     * We defined the maximum distance that a line can be from a point
     * to be a candidate for snapping (1% of the width of the feature
     * bounds for this example).
     */
    ReferencedEnvelope bounds = features.getBounds();
    MAX_SEARCH_DISTANCE = bounds.getSpan(0) / 100.0;

    initEdgesIndexAndPrefix(features);
    initGraph(features);
  }

  private void initEdgesIndexAndPrefix(FeatureCollection features) {
    try {
      features.accepts(new FeatureVisitor() {
        @Override
        public void visit(Feature feature) {
          SimpleFeature simpleFeature = (SimpleFeature) feature;
          if (isRoad(simpleFeature)) {
            Geometry geometry = (MultiLineString) simpleFeature.getDefaultGeometry();
            if (geometry != null) {
              Envelope envelope = geometry.getEnvelopeInternal();
              if (!envelope.isNull()) {
                index.insert(envelope, simpleFeature);
              }
            }
            if (edgeFeaturePrefix == null) {
              initEdgeFeaturePrefix(simpleFeature);
            }
          }
        }
      }, new NullProgressListener());
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
  }

  private void initEdgeFeaturePrefix(SimpleFeature simpleFeature) {
    String featureID = simpleFeature.getID();
    int prefixSeparatorPosition = featureID.lastIndexOf(".");
    if (prefixSeparatorPosition > 0) {
      this.edgeFeaturePrefix = featureID.substring(0, prefixSeparatorPosition + 1);
    }
  }

  private void initGraph(FeatureCollection features) {
    final GraphGenerator generator = new FeatureGraphGenerator(new DirectedLineStringGraphGenerator());
    processedEdges = 0;
    try {
      features.accepts(new FeatureVisitor() {
          public void visit(Feature feature) {
            if (isRoad((SimpleFeature) feature)) {
              generator.add(feature);
              processedEdges++;
            }
          }
      }, null);
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    roadNetworkGraph = generator.getGraph();
  }

  private boolean isRoad(SimpleFeature simpleFeature) {
    /*
     * base implementation -> https://help.openstreetmap.org/questions/30692/export-only-roads --> http://overpass-turbo.eu/s/2vM
    /*
    String[] notRoadsArray = new String[] { "", "footway", "pedestrian", "path"};
    List notRoads = Arrays.asList(notRoadsArray);
    String highwayAttribute = (String)simpleFeature.getAttribute("highway");
    if (notRoads.contains(highwayAttribute)) {
      return false;
    }
    return true;
    */
    /*
     * v1 - using only "highway" attribute (https://help.openstreetmap.org/questions/30692/export-only-roads)
     */
    String[] roadsArray = new String[] { "motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link",
        "secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified", "unclassified_link",
        "residential", "residential_link", "service", "service_link", "living_street"};
    List roadsTypes = Arrays.asList(roadsArray);
    String highwayAttribute = (String)simpleFeature.getAttribute("highway");
    if (roadsTypes.contains(highwayAttribute)) {
      return true;
    }
    return false;
  }

  public Graph getGraph() {
    return roadNetworkGraph;
  }

  public SimpleFeature snapPointToEdge(Coordinate point) {
    Envelope search = new Envelope(point);
    search.expandBy(MAX_SEARCH_DISTANCE);

    /*
     * Query the spatial index for objects within the search envelope.
     * Note that this just compares the point envelope to the line envelopes
     * so it is possible that the point is actually more distant than
     * MAX_SEARCH_DISTANCE from a line.
     */
    List<SimpleFeature> lineFeatures = index.query(search);

    // Initialize the minimum distance found to our maximum acceptable
    // distance plus a little bit
    double minDistance = MAX_SEARCH_DISTANCE + 1.0e-6;
    SimpleFeature minDistanceEdge = null;

    for (SimpleFeature lineFeature : lineFeatures) {
      Geometry lineGeometry = (Geometry)lineFeature.getDefaultGeometry();
      LocationIndexedLine indexedLine = new LocationIndexedLine(lineGeometry);
      LinearLocation here = indexedLine.project(point);
      Coordinate indexedLinePoint = indexedLine.extractPoint(here);
      double distance = indexedLinePoint.distance(point);
      if (distance < minDistance) {
          minDistance = distance;
          minDistanceEdge = lineFeature;
      }
    }
    return minDistanceEdge;
  }

  /**
   * @deprecated
   * use getRoadsFeatureSource instead to obtain only the Features specific to roads
   */
  public SimpleFeatureSource getFeatureSource() {
    SimpleFeatureSource featureSource = null;
    try {
      featureSource = dataStore.getFeatureSource();
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return featureSource;
  }

  public SimpleFeatureSource getRoadsFeatureSource() {
    SimpleFeatureSource featureSource = null;
    FeatureCollection features = null;
    final List<SimpleFeature> roadFeatures = new LinkedList<SimpleFeature>();
    try {
      featureSource = dataStore.getFeatureSource();
      features = featureSource.getFeatures();
      features.accepts(new FeatureVisitor() {
        @Override
        public void visit(Feature feature) {
          SimpleFeature simpleFeature = (SimpleFeature) feature;
          if (isRoad(simpleFeature)) {
            roadFeatures.add(simpleFeature);
          }
        }
      }, new NullProgressListener());
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
    }
    return DataUtilities.source(DataUtilities.collection(roadFeatures));
  }

  public String getEdgeFeaturePrefix() {
    return edgeFeaturePrefix;
  }

  /**
   * created to verify the procesing of only "roads" from shapefile
   * @param args
   */
  public static void main(String[] args) {
    //TODO: better handling of parameters
    File roadNetworkFile = new File(args[0]);
    RoadNetwork roadNetwork = new RoadNetwork(roadNetworkFile);
    System.out.println("road network graph with " + roadNetwork.getGraph().getNodes().size() + " nodes and " + roadNetwork.getGraph().getEdges().size() + " edges.");
    System.out.println("processed " + roadNetwork.processedEdges + " edges");
  }

}
