package ar.uba.fi.result;

import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;

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
public class ColdRoutes extends Routes {

  //TODO: tmp initial fields for testing -> create specific class for cold route patterns
  //public Set<String> jamEdgeIds;
  public SimpleFeatureCollection jamEdges;
  public SimpleFeatureCollection boundingRectangleEdges;
  public SimpleFeatureCollection neighborhoodBREdges;
  public SimpleFeatureCollection coldEdges;

  public ColdRoutes(RoadNetwork roadNetwork) {
    this.roadNetwork = roadNetwork;
  }

  //TODO: create specific class for cold route patterns
  /*
  public List<JamRoute> getJamRoutes() {
    return this.jamRoutes;
  }

  public void addJamRoute(JamRoute JamRoute) {
    this.jamRoutes.add(JamRoute);
  }
  */

  @Override
  public String getLongName() {
    return "Cold routes on road network";
  }

  @Override
  public String getShortName() {
    return "Cold routes";
  }

}
