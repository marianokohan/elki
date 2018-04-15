package ar.uba.fi.result;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;
/*
 This file is developed to run as part of ELKI:
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

/**
 * @author mariano kohan
 *
 */
public class CongestionCluster {

  private Set<Cell> cells;

  public CongestionCluster(Cell cell) {
    this.cells = new HashSet<Cell>();
    this.cells.add(cell);
  }

  public void addCell(Cell cell) {
    this.cells.add(cell);
  }

  public List<SimpleFeature> getCellFeatures() {
    List<SimpleFeature> cellFeatures = new LinkedList<SimpleFeature>();
    for(Cell cell : cells) {
      cellFeatures.add(cell.getFeature());
    }
    return cellFeatures;
  }

  public int size() {
    return this.cells.size();
  }

  public boolean contains(Integer cellId) {
    return this.cells.contains(new Cell(cellId));
  }

  public double performanceIndexSum() {
    double sum = 0;
    for(Cell cell : cells) {
      sum += cell.getPerformanceIndex();
    }
    return sum;
  }

}
