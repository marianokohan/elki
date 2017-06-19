package ar.uba.fi.result;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import ar.uba.fi.roadnetwork.RoadNetwork;
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
 * @author Mariano Kohan
 *
 */
public class ColdRoutes extends Routes {

  private List<ColdRoute> coldRoutes;

  public ColdRoutes(RoadNetwork roadNetwork) {
    this.roadNetwork = roadNetwork;
    this.coldRoutes = new LinkedList<ColdRoute>();
  }

  public List<ColdRoute> getColdRoutes() {
    return this.coldRoutes;
  }

  public void addColdRoute(ColdRoute coldRoute) {
    this.coldRoutes.add(coldRoute);
  }

  public void addColdRoutes(Collection<ColdRoute> coldRoutes) {
    this.coldRoutes.addAll(coldRoutes);
  }

  @Override
  public String getLongName() {
    return "Cold routes on road network";
  }

  @Override
  public String getShortName() {
    return "Cold routes";
  }

  public List<ColdRoute> filterColdRouteWithEdges(SimpleFeatureCollection edges) {
    List<ColdRoute> coldRoutesWithEdges = new LinkedList<ColdRoute>();
    if (!edges.isEmpty()) {
      for(ColdRoute coldRoute : this.coldRoutes) {
        try (SimpleFeatureIterator iter = edges.features()) {
          while (iter.hasNext()) {
              SimpleFeature edgeFeature = iter.next();
              if (coldRoute.containsEdgeFeature(edgeFeature)) {
                coldRoutesWithEdges.add(coldRoute);
                break;
              }
          }
        }
      }
    }
    return coldRoutesWithEdges;
  }

}
