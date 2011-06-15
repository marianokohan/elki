package de.lmu.ifi.dbs.elki.math.linearalgebra;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;

/**
 * Class to compute the centroid of some data.
 * 
 * @author Erich Schubert
 */
public class Centroid extends Vector {
  /**
   * The current weight
   */
  protected double wsum;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   */
  public Centroid(int dim) {
    super(dim);
    this.wsum = 0;
  }

  /**
   * Constructor from an existing matrix columns.
   * 
   * @param mat Matrix to use the columns from.
   */
  public Centroid(Matrix mat) {
    this(mat.getRowDimensionality());
    int n = mat.getColumnDimensionality();
    for(int i = 0; i < n; i++) {
      // TODO: avoid constructing the vector objects?
      this.put(mat.getColumnVector(i));
    }
  }

  /**
   * Constructor from an existing relation.
   * 
   * @param relation Relation to use
   */
  public Centroid(Relation<? extends NumberVector<?, ?>> relation) {
    this(DatabaseUtil.dimensionality(relation));
    if(relation.size() == 0) {
      throw new IllegalArgumentException("Cannot compute a centroid of an empty relation!");
    }
    for(DBID id : relation.iterDBIDs()) {
      this.put(relation.get(id));
    }
  }

  /**
   * Constructor from an existing relation.
   * 
   * @param relation Relation to use
   * @param ids IDs to use
   */
  public Centroid(Relation<? extends NumberVector<?, ?>> relation, DBIDs ids) {
    this(DatabaseUtil.dimensionality(relation));
    if(ids.isEmpty()) {
      throw new IllegalArgumentException("Cannot compute a centroid of an empty set!");
    }
    for(DBID id : ids) {
      this.put(relation.get(id));
    }
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(double[] val) {
    assert (val.length == elements.length);
    wsum += 1.0;
    for(int i = 0; i < elements.length; i++) {
      final double delta = val[i] - elements[i];
      elements[i] += delta / wsum;
    }
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public void put(double val[], double weight) {
    assert (val.length == elements.length);
    final double nwsum = weight + wsum;
    for(int i = 0; i < elements.length; i++) {
      final double delta = val[i] - elements[i];
      final double rval = delta * weight / nwsum;
      elements[i] += rval;
    }
    wsum = nwsum;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public final void put(Vector val) {
    put(val.getArrayRef());
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public final void put(Vector val, double weight) {
    put(val.getArrayRef(), weight);
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(NumberVector<?, ?> val) {
    assert (val.getDimensionality() == elements.length);
    wsum += 1.0;
    for(int i = 0; i < elements.length; i++) {
      final double delta = val.doubleValue(i + 1) - elements[i];
      elements[i] += delta / wsum;
    }
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  public void put(NumberVector<?, ?> val, double weight) {
    assert (val.getDimensionality() == elements.length);
    final double nwsum = weight + wsum;
    for(int i = 0; i < elements.length; i++) {
      final double delta = val.doubleValue(i + 1) - elements[i];
      final double rval = delta * weight / nwsum;
      elements[i] += rval;
    }
    wsum = nwsum;
  }

  /**
   * Get the data as vector
   * 
   * @return the data
   */
  public <F extends NumberVector<? extends F, ?>> F getVector(F factory) {
    return factory.newInstance(elements);
  }
}