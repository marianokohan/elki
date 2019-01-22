package ar.uba.fi.result;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.roadnetwork.RoadNetwork;
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
public class JamRoutes extends Routes {

  private List<JamRoute> jamRoutes;

  public JamRoutes(RoadNetwork roadNetwork) {
    this.roadNetwork = roadNetwork;
    this.jamRoutes = new LinkedList<JamRoute>();
  }

  public List<JamRoute> getJamRoutes() {
    return this.jamRoutes;
  }

  public void addJamRoute(JamRoute JamRoute) {
    this.jamRoutes.add(JamRoute);
  }

  @Override
  public String getLongName() {
    return "Jam routes on road network";
  }

  @Override
  public String getShortName() {
    return "Jam routes";
  }

  /**
   * given a File with representations of the jam route from "toString()"
   *  it extracts the ids of the "jam" edges (for all the jamRoutes)
   * @param jamRouteString
   * @return
   */
  public static Set<String> parseJamEdgeIds(File jamRoutesFile) {
    Set<String> jamEdgeIds = new HashSet<String>();
    Charset charset = Charset.forName("UTF-8");
    try (BufferedReader reader = Files.newBufferedReader(jamRoutesFile.toPath(), charset)) {
        String jamRouteLine = null;
        while ((jamRouteLine = reader.readLine()) != null) {
            jamEdgeIds.addAll(JamRoute.parseJamEdgeIds(jamRouteLine));
        }
    } catch (IOException ioException) {
      System.err.format("IOException on parsing jam routes from file %s: %s%n", jamRoutesFile, ioException);
    }
    return jamEdgeIds;
  }

  public List<JamRoute> filterJamRouteWithEdges(SimpleFeatureCollection edges, boolean jamRoutesWithJams) {
    List<JamRoute> jamRoutesWithEdges = new LinkedList<JamRoute>();
    if (!edges.isEmpty()) {
      for(JamRoute jamRoute : this.jamRoutes) {
        if ( (jamRoutesWithJams && jamRoute.containsJams()) ||
            (!jamRoutesWithJams) ){
          try (SimpleFeatureIterator iter = edges.features()) {
            while (iter.hasNext()) {
                SimpleFeature edgeFeature = iter.next();
                if (jamRoute.containsEdgeFeature(edgeFeature)) {
                  jamRoutesWithEdges.add(jamRoute);
                  break; //TODO: improve to avoid iterate this.jamRoutes
                }
            }
          }
        }
      }
    }
    return jamRoutesWithEdges;
  }

  public Map<Integer, Integer> getJamRoutesSizeByLength() {
    Map<Integer, Integer> sizeByLength = new HashMap<Integer, Integer>();
    Integer size, length;
    for(JamRoute jamRoute : jamRoutes) {
      length = jamRoute.getLength();
      size = sizeByLength.get(length);
      if (size == null) {
        size = new Integer(1);
      } else {
        size = size + 1;
      }
      sizeByLength.put(length, size);
    }
    return sizeByLength;
  }

  @Override
  public Map<Integer, Integer> getRoutesSizeByLength() {
    return getJamRoutesSizeByLength();
  }

}
