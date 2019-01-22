package ar.uba.fi.result;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
public class HotRoutes extends Routes {

  private List<HotRoute> hotRoutes;

  public HotRoutes(RoadNetwork roadNetwork) {
    this.roadNetwork = roadNetwork;
    this.hotRoutes = new LinkedList<HotRoute>();
  }

  public List<HotRoute> getHotRoutes() {
    return this.hotRoutes;
  }

  public void addHotRoute(HotRoute hotRoute) {
    this.hotRoutes.add(hotRoute);
  }

  @Override
  public String getLongName() {
    return "Density-based hot routes on road network";
  }

  @Override
  public String getShortName() {
    return "Density-based hot routes";
  }

  public Map<Integer, Integer> getHotRoutesSizeByLength() {
    Map<Integer, Integer> sizeByLength = new HashMap<Integer, Integer>();
    Integer size, length;
    for(HotRoute hotRoute : hotRoutes) {
      length = hotRoute.getLength();
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
    return getHotRoutesSizeByLength();
  }

}
