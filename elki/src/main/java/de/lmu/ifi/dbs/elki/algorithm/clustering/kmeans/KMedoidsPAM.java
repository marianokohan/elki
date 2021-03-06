package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.PAMInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The original PAM algorithm or k-medoids clustering, as proposed by Kaufman
 * and Rousseeuw in "Partitioning Around Medoids".
 * 
 * Reference:
 * <p>
 * Clustering my means of Medoids<br />
 * Kaufman, L. and Rousseeuw, P.J.<br />
 * in: Statistical Data Analysis Based on the L1-Norm and Related Methods
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 * 
 * @param <V> vector datatype
 */
@Title("Partioning Around Medoids")
@Reference(title = "Clustering by means of Medoids", //
authors = "Kaufman, L. and Rousseeuw, P.J.", //
booktitle = "Statistical Data Analysis Based on the L1-Norm and Related Methods")
public class KMedoidsPAM<V> extends AbstractDistanceBasedAlgorithm<V, Clustering<MedoidModel>> implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPAM.class.getName();

  /**
   * The number of clusters to produce.
   */
  protected int k;

  /**
   * The maximum number of iterations.
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMedoidsInitialization<V> initializer;

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsPAM(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    super(distanceFunction);
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  /**
   * Run k-medoids
   * 
   * @param database Database
   * @param relation relation to use
   * @return result
   */
  public Clustering<MedoidModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Medoids Clustering", "kmedoids-clustering");
    }
    DistanceQuery<V> distQ = database.getDistanceQuery(relation, getDistanceFunction());
    DBIDs ids = relation.getDBIDs();
    // Choose initial medoids
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distQ));
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k));
    }

    runPAMOptimization(distQ, ids, medoids, clusters);

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("k-Medoids Clustering", "kmedoids-clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      MedoidModel model = new MedoidModel(DBIDUtil.deref(it));
      result.addToplevelCluster(new Cluster<>(clusters.get(it.getOffset()), model));
    }
    return result;
  }

  /**
   * Run the PAM optimization phase.
   * 
   * @param distQ Distance query
   * @param ids IDs to process
   * @param medoids Medoids list
   * @param clusters Clusters
   */
  protected void runPAMOptimization(DistanceQuery<V> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, List<ModifiableDBIDs> clusters) {
    WritableDoubleDataStore second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    assignToNearestCluster(medoids, ids, second, clusters, distQ);

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
    // Swap phase
    DBIDVar bestid = DBIDUtil.newVar();
    int iteration = 0;
    for(boolean changed = true; changed; iteration++) {
      LOG.incrementProcessed(prog);
      changed = false;
      // Try to swap the medoid with a better cluster member:
      double best = 0;
      int bestcluster = -1;
      int i = 0;
      for(DBIDIter miter = medoids.iter(); miter.valid(); miter.advance(), i++) {
        for(DBIDIter iter = clusters.get(i).iter(); iter.valid(); iter.advance()) {
          if(DBIDUtil.equal(miter, iter)) {
            continue;
          }
          double cost = 0;
          DBIDIter miter2 = medoids.iter();
          for(int j = 0; j < k; j++, miter2.advance()) {
            for(DBIDIter iter2 = clusters.get(j).iter(); iter2.valid(); iter2.advance()) {
              if(DBIDUtil.equal(miter2, iter2)) {
                continue;
              }
              double distcur = distQ.distance(iter2, miter2);
              double distnew = distQ.distance(iter2, iter);
              if(j == i) {
                // Cases 1 and 2.
                double distsec = second.doubleValue(iter2);
                cost += (distcur > distsec) ? //
                // Case 1, other would switch to a third medoid
                distsec - distcur // Always positive!
                : // Would remain with the candidate
                distnew - distcur; // Could be negative
              }
              else {
                // Cases 3-4: objects from other clusters
                // Case 3: is no change
                if(distcur > distnew) {
                  // Case 4: would switch to new medoid
                  cost += distnew - distcur; // Always negative
                }
              }
            }
          }
          if(cost < best) {
            best = cost;
            bestid.set(iter);
            bestcluster = i;
          }
        }
      }
      if(LOG.isDebugging()) {
        LOG.debug("Best cost: " + best);
      }
      if(best < 0.) {
        changed = true;
        medoids.set(bestcluster, bestid);
      }
      // Reassign
      if(changed) {
        // TODO: can we save some of these recomputations?
        assignToNearestCluster(medoids, ids, second, clusters, distQ);
      }
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param means Object centroids
   * @param ids Object ids
   * @param second Distance to second nearest medoid
   * @param clusters cluster assignment
   * @param distQ distance query
   * @return true when any object was reassigned
   */
  protected boolean assignToNearestCluster(ArrayDBIDs means, DBIDs ids, WritableDoubleDataStore second, List<? extends ModifiableDBIDs> clusters, DistanceQuery<V> distQ) {
    boolean changed = false;

    DBIDArrayIter miter = means.iter();
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY, mindist2 = Double.POSITIVE_INFINITY;
      int minIndex = 0;
      miter.seek(0); // Reuse iterator.
      for(int i = 0; miter.valid(); miter.advance(), i++) {
        double dist = distQ.distance(iditer, miter);
        if(dist < mindist) {
          minIndex = i;
          mindist2 = mindist;
          mindist = dist;
        }
        else if(dist < mindist2) {
          mindist2 = dist;
        }
      }
      if(clusters.get(minIndex).add(iditer)) {
        changed = true;
        // Remove from previous cluster
        // TODO: keep a list of cluster assignments to save this search?
        for(int j = 0; j < k; j++) {
          if(j != minIndex && clusters.get(j).remove(iditer)) {
            break;
          }
        }
      }
      second.put(iditer, mindist2);
    }
    return changed;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * The number of clusters to produce.
     */
    protected int k;

    /**
     * The maximum number of iterations.
     */
    protected int maxiter;

    /**
     * Method to choose initial means.
     */
    protected KMedoidsInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KMeans.K_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<>(KMeans.INIT_ID, KMedoidsInitialization.class, PAMInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID, 0) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected KMedoidsPAM<V> makeInstance() {
      return new KMedoidsPAM<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
