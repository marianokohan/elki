package ar.uba.fi.result;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geotools.graph.structure.DirectedEdge;
//TODO: confirm license description
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import org.opengis.feature.simple.SimpleFeature;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * @author Mariano Kohan
 *
 */
public class JamRoute extends HotRoute {

  private static final Logging LOG = Logging.getLogger(JamRoute.class);

  private Set<DirectedEdge> jams;

  public JamRoute(DirectedEdge start, boolean isJam) {
    super(start);
    this.jams = new HashSet<DirectedEdge>();
    if (isJam) {
      this.jams.add(start);
    }
  }

  private JamRoute(List<DirectedEdge> edges, Set<DirectedEdge> jams) {
    super(edges);
    this.jams = new HashSet<DirectedEdge>();
    this.jams.addAll(jams);
  }

  /**
   * creates a copy of this jam route
   * with the given edge appended to the end
   * and marked as "jam" if applied
   * It should be used instead of the method in {@code HotRoute} class
   * @param lastEdge
   * @param isJam
   * @return
   */
  public JamRoute copyWithEdge(DirectedEdge lastEdge, boolean isJam) {
    JamRoute copyJamRoute = new JamRoute(this.edges, this.jams);
    copyJamRoute.edges.add(lastEdge);
    if (isJam) {
      copyJamRoute.jams.add(lastEdge);
    }
    return copyJamRoute;
  }

  public boolean isJam(DirectedEdge edge) {
    return this.jams.contains(edge);
  }

  public List<SimpleFeature>[] getEdgeWithJamsFeatures() {
    List<SimpleFeature> edgeFeatures = new LinkedList<SimpleFeature>();
    List<SimpleFeature> jamFeatures = new LinkedList<SimpleFeature>();
    if (LOG_RULE_EDGES) {
      debugRule = new StringBuffer("jam route (" + edges.size() + " edges): ");
    }
    for(DirectedEdge directedEdge : edges) {
      SimpleFeature feature = (SimpleFeature)directedEdge.getObject();
      if (LOG_RULE_EDGES) debugRule.append(feature.getID());
      if (isJam(directedEdge)) {
        if (LOG_RULE_EDGES) debugRule.append(" (JAM) -> ");
        jamFeatures.add(feature);
      } else {
        if (LOG_RULE_EDGES) debugRule.append(" -> ");
        edgeFeatures.add(feature);
      }
    }
    if (LOG_RULE_EDGES) LOG.debug(debugRule.append("[END]"));
    return new List[] { edgeFeatures, jamFeatures };
  }

  public boolean containsJams() {
    return !this.jams.isEmpty();
  }

  public String toString() {
    StringBuffer  rule = new StringBuffer("jam route (" + edges.size() + " edges): ");
    for(DirectedEdge directedEdge : edges) {
      SimpleFeature feature = (SimpleFeature)directedEdge.getObject();
      rule.append(feature.getID());
      if (isJam(directedEdge)) {
        rule.append(" (JAM) -> ");
      } else {
        rule.append(" -> ");
      }
    }
    rule.append("[END]");
    return rule.toString();
  }

}
