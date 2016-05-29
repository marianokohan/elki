package ar.uba.fi.result;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.graph.structure.DirectedEdge;
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
import org.opengis.feature.simple.SimpleFeature;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * @author Mariano Kohan
 *
 */
public class ColdRoute extends HotRoute {

  private static final Logging LOG = Logging.getLogger(ColdRoute.class);

  private Set<DirectedEdge> coldEdges;

  public ColdRoute(DirectedEdge start) {
    //always start with a cold edge
    this(start, true);
  }

  public ColdRoute(DirectedEdge start, boolean isCold) {
    super(start);
    this.coldEdges = new HashSet<DirectedEdge>();
    if (isCold) {
      this.coldEdges.add(start);
    }
  }

  private ColdRoute(List<DirectedEdge> edges, Set<DirectedEdge> coldEdges) {
    super(edges);
    this.coldEdges = new HashSet<DirectedEdge>();
    this.coldEdges.addAll(coldEdges);
  }

  /**
   * creates a copy of this cold route
   * with the given edge appended to the end
   * and marked as "cold" if applies
   * It should be used instead of the method in {@code HotRoute} class
   * @param lastEdge
   * @param isCold
   * @return
   */
  public ColdRoute copyWithEdge(DirectedEdge lastEdge, boolean isCold) {
    ColdRoute copyColdRoute = new ColdRoute(this.edges, this.coldEdges);
    copyColdRoute.edges.add(lastEdge);
    if (isCold) {
      copyColdRoute.coldEdges.add(lastEdge);
    }
    return copyColdRoute;
  }

  public boolean isCold(DirectedEdge edge) {
    return this.coldEdges.contains(edge);
  }

  public List<SimpleFeature>[] getEdgeWithColdFeatures() {
    List<SimpleFeature> edgeFeatures = new LinkedList<SimpleFeature>();
    List<SimpleFeature> coldEdgeFeatures = new LinkedList<SimpleFeature>();
    if (LOG_RULE_EDGES) {
      debugRule = new StringBuffer("cold route (" + edges.size() + " edges): ");
    }
    for(DirectedEdge directedEdge : edges) {
      SimpleFeature feature = (SimpleFeature)directedEdge.getObject();
      if (LOG_RULE_EDGES) debugRule.append(feature.getID());
      if (isCold(directedEdge)) {
        if (LOG_RULE_EDGES) debugRule.append(" (COLD) -> ");
        coldEdgeFeatures.add(feature);
      } else {
        if (LOG_RULE_EDGES) debugRule.append(" -> ");
        edgeFeatures.add(feature);
      }
    }
    if (LOG_RULE_EDGES) LOG.debug(debugRule.substring(0, debugRule.length() - 4));
    return new List[] { edgeFeatures, coldEdgeFeatures };
  }

  public boolean containsColdEdges() {
    return !this.coldEdges.isEmpty();
  }

  public String toString() {
    StringBuffer rule = new StringBuffer("cold route (" + edges.size() + " edges): ");
    for(DirectedEdge directedEdge : edges) {
      SimpleFeature feature = (SimpleFeature)directedEdge.getObject();
      rule.append(feature.getID());
      if (isCold(directedEdge)) {
        rule.append(" (COLD) -> ");
      } else {
        rule.append(" -> ");
      }
    }
    return rule.substring(0, rule.length() - 4).toString();
  }

}
