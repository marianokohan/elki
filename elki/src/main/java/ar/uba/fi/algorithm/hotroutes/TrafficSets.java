package ar.uba.fi.algorithm.hotroutes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ar.uba.fi.algorithm.jamroutes.JamFlowScan;
import ar.uba.fi.converter.BrinkhoffPositionToEdgeConverter;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * calculate de the Traffic (def. 4) for some given edges
 *
 * @author Mariano Kohan
 *
 */
public class TrafficSets {

  private static final Logging LOG = Logging.getLogger(TrafficSets.class);
  private static final String EDGES_TRAFFIC_SETS_DUMP = "edges__traffic_sets.csv";
  //internal parameterization
  protected static final boolean DUMP_TRAFFIC_SETS = true;

  private Map<Integer, Set<Integer>> edgeTransactionsMap;


  public TrafficSets(Database database) {
    edgeTransactionsMap = new HashMap<Integer, Set<Integer>>();
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD , null);
    Integer edgeId = null;
    Set<Integer> trafficSet = null;
    for(DBIDIter iditer = trRelation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleVector transationVector = trRelation.get(iditer);
      edgeId = transationVector.intValue(2);
      trafficSet = edgeTransactionsMap.get(edgeId);
      if (trafficSet == null) {
        trafficSet = new HashSet<Integer>();
        edgeTransactionsMap.put(edgeId, trafficSet);
      }
      trafficSet.add(transationVector.intValue(0));
    }
    dumpTrafficSets();
  }

  /**
   * dump traffic sets values to a csv file
   */
  protected void dumpTrafficSets() {
    if (DUMP_TRAFFIC_SETS) {
      FileWriter trafficSetsDump;
      try {
        trafficSetsDump = new FileWriter(EDGES_TRAFFIC_SETS_DUMP);
        StringBuffer edgeTrafficSets;
        for(Map.Entry<Integer, Set<Integer>> edgeTransactions : this.edgeTransactionsMap.entrySet()) {
          edgeTrafficSets = new StringBuffer().append(edgeTransactions.getKey()).append(";");
          Set<Integer> traffic = edgeTransactions.getValue();
          edgeTrafficSets.append(traffic).append(";");
          edgeTrafficSets.append(traffic.size()).append("\n");
          trafficSetsDump.write(edgeTrafficSets.toString());
        }
        trafficSetsDump.close();
        LOG.debug("Created traffic sets dump '" + EDGES_TRAFFIC_SETS_DUMP + "' from " + this.edgeTransactionsMap.size() + " edges.");
      }
      catch(IOException e) {
        LOG.debug("exception on traffic sets dump", e);
      }
    }
  }

  /**
   * return the traffic for the given edge id
   * @param edgeId
   * @return
   */
  public Set<Integer> traffic(String edgeId) {
    int parsedEdgeId = Integer.parseInt(BrinkhoffPositionToEdgeConverter.filterPrefixFromEdgeFeatureId(edgeId));
    Set<Integer> trafficSet = edgeTransactionsMap.get(parsedEdgeId);
    if (trafficSet == null) {
      trafficSet = new HashSet<Integer>();
    }
    return trafficSet;
  }

  /**
   * return the union set for the traffic of the given edges
   * @param edgeIds
   * @return
   */
  public Set<Integer> trafficUnion(String...edgeIds ) {
    if (edgeIds.length == 0) {
      return new HashSet<Integer>();
    }
    Set<Integer> trafficSet, trafficSetUnion = null;
    for(String edgeId : edgeIds) {
      trafficSet = traffic(edgeId);
      if (trafficSetUnion == null) {
        trafficSetUnion = new HashSet<Integer>(trafficSet);
      } else {
        trafficSetUnion.addAll(trafficSet);
      }
    }
    return trafficSetUnion;
  }

  /**
   * return the intersection set for the traffic of the given edges
   * @param edgeIds
   * @return
   */
  public Set<Integer> trafficInteresection(String...edgeIds ) {
    if (edgeIds.length == 0) {
      return new HashSet<Integer>();
    }
    Set<Integer> trafficSet, trafficSetIntersection = null;
    for(String edgeId : edgeIds) {
      trafficSet = traffic(edgeId);
      if (trafficSetIntersection == null) {
        trafficSetIntersection = new HashSet<Integer>(trafficSet);
      } else {
        trafficSetIntersection.retainAll(trafficSet);
      }
    }
    return trafficSetIntersection;
  }

  /**
   * return the difference between the given traffic sets
   * @param trafficSetA
   * @param trafficSetB
   * @return
   */
  public Set<Integer> trafficDifference(Set<Integer> trafficSetA, Set<Integer> trafficSetB) {
    Set<Integer> trafficSetDifference = null;
    if ( (trafficSetA != null) && (trafficSetB != null) ) {
      trafficSetDifference = new HashSet<Integer>(trafficSetA);
      trafficSetDifference.removeAll(trafficSetB);
    } else {
      trafficSetDifference = new HashSet<Integer>();
    }
    return trafficSetDifference;
  }

}
