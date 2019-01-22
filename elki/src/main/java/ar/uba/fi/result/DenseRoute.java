package ar.uba.fi.result;

import java.util.List;

import org.geotools.graph.structure.DirectedEdge;
/*
 This file is part of ELKI:
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

public class DenseRoute extends HotRoute {

  public DenseRoute(DirectedEdge start) {
    super(start);
  }

  public DenseRoute(List<DirectedEdge> edges) {
    super(edges);
  }

  /**
   * creates a copy of this dense route
   * with the given edge appended to the end/start
   *  (according to endLocation)
   * It should be used instead of the method in {@code HotRoute} class
   * @param newEdge
   * @param endLocation
   * @return
   */
  public DenseRoute copyWithEdge(DirectedEdge newEdge, boolean endLocation) {
    DenseRoute copyDenseRoute = new DenseRoute(this.edges);
    if (endLocation) {
      copyDenseRoute.edges.add(newEdge);
    } else {
      copyDenseRoute.edges.add(0, newEdge);
    }
    return copyDenseRoute;
  }

}
