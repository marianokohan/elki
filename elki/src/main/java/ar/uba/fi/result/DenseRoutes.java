package ar.uba.fi.result;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.graph.structure.DirectedEdge;
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
 * Representation of the discovered set of dense routes
 *
 * @author Mariano Kohan
 *
 */
public class DenseRoutes extends Routes {

  private List<DenseRoute> denseRoutes;
  private Map<String,List<DenseRoute>> denseRoutesByEdge;

  public DenseRoutes(RoadNetwork roadNetwork) {
    this.roadNetwork = roadNetwork;
    this.denseRoutes = new LinkedList<DenseRoute>();
  }

  public List<DenseRoute> getDenseRoutes() {
    return this.denseRoutes;
  }

  public void addDenseRoutes(Collection<DenseRoute> denseRoutes) {
    this.denseRoutes.addAll(denseRoutes);
  }

  @Override
  public String getLongName() {
    return "Dense routes on road network";
  }

  @Override
  public String getShortName() {
    return "Dense routes";
  }

  private void indexDenseRoutes() {
    if (this.denseRoutesByEdge == null) {
      this.denseRoutesByEdge = new HashMap<String, List<DenseRoute>>();
      for(DenseRoute denseRoute : this.denseRoutes) {
        for(DirectedEdge denseRouteEdge : denseRoute.edges) {
          String edgeId = ((SimpleFeature)denseRouteEdge.getObject()).getID();
          List<DenseRoute> edgeDenseRoutes = this.denseRoutesByEdge.get(edgeId);
          if (edgeDenseRoutes == null) {
            edgeDenseRoutes = new LinkedList<DenseRoute>();
            this.denseRoutesByEdge.put(edgeId, edgeDenseRoutes);
          }
          edgeDenseRoutes.add(denseRoute);
        }
      }
    }
  }

  public List<DenseRoute> filterDenseRouteWithEdges(SimpleFeatureCollection edges) {
    List<DenseRoute> denseRoutesWithEdges = new LinkedList<DenseRoute>();
    if (!edges.isEmpty()) {
      this.indexDenseRoutes();
      try (SimpleFeatureIterator iter = edges.features()) {
        while (iter.hasNext()) {
            SimpleFeature edgeFeature = iter.next();
            List<DenseRoute> edgeJamRoutes = this.denseRoutesByEdge.getOrDefault(edgeFeature.getID(), new LinkedList<DenseRoute>());
            denseRoutesWithEdges.addAll(edgeJamRoutes);
        }
      }
    }
    return denseRoutesWithEdges;
  }

  public Map<Integer, Integer> getDenseRoutesSizeByLength() {
    Map<Integer, Integer> sizeByLength = new HashMap<Integer, Integer>();
    Integer size, length;
    for(DenseRoute denseRoute : denseRoutes) {
      length = denseRoute.getLength();
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
    return getDenseRoutesSizeByLength();
  }

}
