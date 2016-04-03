package ar.uba.fi.algorithm.jamroutes;

import java.util.HashMap;
import java.util.Map;

import ar.uba.fi.converter.BrinkhoffPositionToEdgeConverter;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
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
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * calculate de the Speed (def. 1) for a given edge
 *
 * @author Mariano Kohan
 *
 */
public class Speeds {

  private static final Logging LOG = Logging.getLogger(Speeds.class);
  //internal parameterization
  protected static final boolean LOG_SPEED_STATS = false;

  private Map<Integer, Speed> edgeSpeedMap;

  public Speeds(Database database) {
    edgeSpeedMap = new HashMap<Integer, Speed>();
    Relation<DoubleVector> trRelation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD , null);
    Integer edgeId = null;
    Float transactionSpeed = null;
    Speed speed = null;
    for(DBIDIter iditer = trRelation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DoubleVector transationVector = trRelation.get(iditer);
      edgeId = transationVector.intValue(2);
      transactionSpeed = transationVector.floatValue(5);
      speed = edgeSpeedMap.get(edgeId);
      if (speed == null) {
        speed = new Speed(transactionSpeed);
        edgeSpeedMap.put(edgeId, speed);
      } else {
        speed.upddate(transactionSpeed);
      }
    }
    if (LOG_SPEED_STATS) {
      logSpeedStats();
    }
  }

  private void logSpeedStats() {
    double minSpeed = -1;
    double maxSpeed = 0;
    double speedSum = 0;
    double speedCount = 0;

    for(Speed speed : this.edgeSpeedMap.values()) {
      float speedValue = speed.get();
      if (speedValue > 0) {
        if (minSpeed == -1) {
          minSpeed = speedValue;
        } else {
          if (speedValue < minSpeed) {
            minSpeed = speedValue;
          }
        }

        if (maxSpeed == 0) {
          maxSpeed = speedValue;
        } else {
          if (speedValue > maxSpeed) {
            maxSpeed = speedValue;
          }
        }
      }
      speedSum += speedValue;
      speedCount++;
    }

    LOG.debug("Edge speed stats\n----------------");
    LOG.debug("min: " + minSpeed);
    LOG.debug("max: " + maxSpeed);
    LOG.debug("mean: " + speedSum/speedCount + "\n\n");
  }

  /**
   * return the speed (def. 1) for the given edge id
   * @param edgeId
   * @return
   */
  public float speed(String edgeId) {
    int parsedEdgeId = Integer.parseInt(BrinkhoffPositionToEdgeConverter.filterPrefixFromEdgeFeatureId(edgeId));
    Speed edgeSpeed = this.edgeSpeedMap.get(parsedEdgeId);
    if (edgeSpeed == null) {
      edgeSpeed = new Speed(0);
    }
    return edgeSpeed.get();
  }

}
