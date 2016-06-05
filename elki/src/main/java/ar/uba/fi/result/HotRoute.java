package ar.uba.fi.result;

import java.util.LinkedList;
import java.util.List;

import org.geotools.graph.structure.DirectedEdge;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import de.lmu.ifi.dbs.elki.logging.Logging;

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
public class HotRoute {

  //internal parameterization
  protected static final boolean LOG_RULE_EDGES = true;
  protected StringBuffer debugRule;

  private static final Logging LOG = Logging.getLogger(HotRoute.class);

  protected List<DirectedEdge> edges;

  public HotRoute(DirectedEdge start) {
    this.edges = new LinkedList<DirectedEdge>();
    this.edges.add(start);
  }

  protected HotRoute(List<DirectedEdge> edges) {
    this.edges = new LinkedList<DirectedEdge>();
    this.edges.addAll(edges);
  }

  public List<SimpleFeature> getEdgeFeatures() {
    List<SimpleFeature> edgeFeatures = new LinkedList<SimpleFeature>();
    if (LOG_RULE_EDGES) {
      debugRule = new StringBuffer("rule (" + edges.size() + " edges): ");
    }
    for(DirectedEdge directedEdge : edges) {
      SimpleFeature feature = (SimpleFeature)directedEdge.getObject();
      if (LOG_RULE_EDGES) debugRule.append(feature.getID() + " -> ");
      edgeFeatures.add(feature);
    }
    if (LOG_RULE_EDGES) LOG.debug(debugRule.append("[END]"));
    return edgeFeatures;
  }

  public DirectedEdge getLastEdge() {
    return this.edges.get(this.edges.size()-1);
  }

  public DirectedEdge getStartEdge() {
    return this.edges.get(0);
  }

  public Point getStartPoint() {
    SimpleFeature startFeature = (SimpleFeature) edges.get(0).getObject();
    MultiLineString startGeometry = (MultiLineString) startFeature.getDefaultGeometry();
    return ((LineString)startGeometry.getGeometryN(0)).getStartPoint();
  }

  public Point getEndPoint() {
    SimpleFeature startFeature = (SimpleFeature) edges.get(this.edges.size()-1).getObject();
    MultiLineString startGeometry = (MultiLineString) startFeature.getDefaultGeometry();
    return ((LineString)startGeometry.getGeometryN(0)).getEndPoint();
  }

  public SimpleFeature getStartEdgeFeature() {
    return (SimpleFeature)this.edges.get(0).getObject();
  }

  public List<String> getLastEdgesIds(int numberOfLastEdges) {
    List<String> lastEdgesIds = new LinkedList<String>();
    int startEdgeIndex = 0;
    int numberOfEdges = this.edges.size();
    if (numberOfEdges > numberOfLastEdges) {
      startEdgeIndex = numberOfEdges - numberOfLastEdges;
    }
    List<DirectedEdge> lastEdges = this.edges.subList(startEdgeIndex, numberOfEdges);
    for(DirectedEdge directedEdge : lastEdges) {
      lastEdgesIds.add(((SimpleFeature)directedEdge.getObject()).getID());
    }
    return lastEdgesIds;
  }

  /**
   * creates a copy of this hot route
   * with the given edge appended to the end
   * @param lastEdge
   * @return
   */
  public HotRoute copyWithEdge(DirectedEdge lastEdge) {
    HotRoute copyHotRoute = new HotRoute(this.edges);
    copyHotRoute.edges.add(lastEdge);
    return copyHotRoute;
  }

  public boolean contains(DirectedEdge edge) {
    return this.edges.contains(edge);
  }

  public int getLength() {
    return this.edges.size();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof HotRoute) {
      HotRoute otherHotRoute = (HotRoute)obj;
      return this.edges.equals(otherHotRoute.edges);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return this.edges.hashCode();
  }

}
