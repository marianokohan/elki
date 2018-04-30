package ar.uba.fi.algorithm.hotroutes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ar.uba.fi.converter.BrinkhoffPositionToEdgeConverter;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
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
  protected static final boolean DUMP_TRAFFIC_SETS = false;

  private Map<Integer, Set<String>> edgeTransactionsMap;


  public TrafficSets(Database database) {
    edgeTransactionsMap = new HashMap<Integer, Set<String>>();
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD , null); //timestamp (in milliseconds); edgeId; longitude; latitude; speed (in km/h)
    Relation<LabelList> trIdRelation = database.getRelation(TypeUtil.LABELLIST, null); //list with trajectory Id
    DBIDIter trIdIter = trIdRelation.iterDBIDs();
    Integer edgeId = null;
    Set<String> trafficSet = null;
    for(DBIDIter trIter = trRelation.iterDBIDs(); trIter.valid(); trIter.advance()) {
      DoubleVector transationVector = trRelation.get(trIter);
      edgeId = transationVector.intValue(1);
      trafficSet = edgeTransactionsMap.get(edgeId);
      if (trafficSet == null) {
        trafficSet = new HashSet<String>();
        edgeTransactionsMap.put(edgeId, trafficSet);
      }
      trafficSet.add(trIdRelation.get(trIdIter).get(0));
      trIdIter.advance();
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
        for(Map.Entry<Integer, Set<String>> edgeTransactions : this.edgeTransactionsMap.entrySet()) {
          edgeTrafficSets = new StringBuffer().append(edgeTransactions.getKey()).append(";");
          Set<String> traffic = edgeTransactions.getValue();
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
  public Set<String> traffic(String edgeId) {
    int parsedEdgeId = Integer.parseInt(BrinkhoffPositionToEdgeConverter.filterPrefixFromEdgeFeatureId(edgeId));
    Set<String> trafficSet = edgeTransactionsMap.get(parsedEdgeId);
    if (trafficSet == null) {
      trafficSet = new HashSet<String>();
    }
    return trafficSet;
  }

  /**
   * return the union set for the traffic of the given edges
   * @param edgeIds
   * @return
   */
  public Set<String> trafficUnion(String...edgeIds ) {
    if (edgeIds.length == 0) {
      return new HashSet<String>();
    }
    Set<String> trafficSet, trafficSetUnion = null;
    for(String edgeId : edgeIds) {
      trafficSet = traffic(edgeId);
      if (trafficSetUnion == null) {
        trafficSetUnion = new HashSet<String>(trafficSet);
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
  public Set<String> trafficInteresection(String...edgeIds ) {
    if (edgeIds.length == 0) {
      return new HashSet<String>();
    }
    Set<String> trafficSet, trafficSetIntersection = null;
    for(String edgeId : edgeIds) {
      trafficSet = traffic(edgeId);
      if (trafficSetIntersection == null) {
        trafficSetIntersection = new HashSet<String>(trafficSet);
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
  public Set<String> trafficDifference(Set<String> trafficSetA, Set<String> trafficSetB) {
    Set<String> trafficSetDifference = null;
    if ( (trafficSetA != null) && (trafficSetB != null) ) {
      trafficSetDifference = new HashSet<String>(trafficSetA);
      trafficSetDifference.removeAll(trafficSetB);
    } else {
      trafficSetDifference = new HashSet<String>();
    }
    return trafficSetDifference;
  }

}
