package ar.uba.fi.algorithm.denseroutes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ar.uba.fi.converter.BrinkhoffPositionToEdgeConverter;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * calculate the Density for some given edges
 *
 * @author Mariano Kohan
 *
 */
public class EdgeDensities {

  private static final Logging LOG = Logging.getLogger(EdgeDensities.class);
  private static final String EDGES_DENSITIES_DUMP = "edges__densities.csv";
  //internal parameterization
  protected static final boolean DUMP_EDGE_DENSITIES = false;

  private Map<Integer, Integer> edgeDensityMap;


  public EdgeDensities(Database database) {
    edgeDensityMap = new HashMap<Integer, Integer>();
    Map<Integer, Set<Integer>> edgeTransactionsMap = createEdgeTransactionsMap(database);
    for(Entry<Integer, Set<Integer>> edgeTransaction : edgeTransactionsMap.entrySet()) {
      edgeDensityMap.put(edgeTransaction.getKey(), edgeTransaction.getValue().size());
    }

    dumpTrafficDensities();
  }

  private Map<Integer, Set<Integer>> createEdgeTransactionsMap(Database database) {
    Map<Integer, Set<Integer>> edgeTransactionsMap = new HashMap<Integer, Set<Integer>>();
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
    return edgeTransactionsMap;
  }

  /**
   * dump traffic density values to a csv file
   */
  protected void dumpTrafficDensities() {
    if (DUMP_EDGE_DENSITIES) {
      FileWriter edgeDensitiesDump;
      try {
        edgeDensitiesDump = new FileWriter(EDGES_DENSITIES_DUMP);
        StringBuffer edgeDensity;
        for(Map.Entry<Integer, Integer> edgeDensities : this.edgeDensityMap.entrySet()) {
          edgeDensity = new StringBuffer().append(edgeDensities.getKey()).append(";");
          edgeDensity.append(edgeDensities.getValue()).append("\n");
          edgeDensitiesDump.write(edgeDensity.toString());
        }
        edgeDensitiesDump.close();
        LOG.debug("Created edge densities dump '" + EDGES_DENSITIES_DUMP + "' from " + this.edgeDensityMap.size() + " edges.");
      }
      catch(IOException e) {
        LOG.debug("exception on edge densities dump", e);
      }
    }
  }

  /**
   * return the density for the given edge id
   * @param edgeId
   * @return
   */
  public Integer density(String edgeId) {
    int parsedEdgeId = Integer.parseInt(BrinkhoffPositionToEdgeConverter.filterPrefixFromEdgeFeatureId(edgeId));
    return edgeDensityMap.getOrDefault(parsedEdgeId, 0);
  }

  /**
   * return the difference between the density values for the given edges
   * @param edgeIdA
   * @param edgeIdB
   * @return
   */
  public Integer densityDifference(String edgeIdA, String edgeIdB) {
    Integer densityDifference = density(edgeIdA) - density(edgeIdB);
    return Math.abs(densityDifference);
  }


}
