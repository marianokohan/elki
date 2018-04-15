package ar.uba.fi.result;

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
public class Cell {

  private Integer attributeId;
  private SimpleFeature feature;
  private Double sci;
  private Double performanceIndex;
  private Boolean coreCell;

  public Cell(Integer attributeId, SimpleFeature feature, Double performanceIndex) {
    this.attributeId = attributeId;
    this.feature = feature;
    this.performanceIndex = performanceIndex;
    this.coreCell = false;
  }

  public Cell(Integer attributeId, SimpleFeature feature, Double sci, Double performanceIndex) {
    this.attributeId = attributeId;
    this.feature = feature;
    this.sci = sci;
    this.performanceIndex = performanceIndex;
    this.coreCell = false;
  }

  public Cell(Integer attributeId, SimpleFeature feature, Double sci, Double performanceIndex, Boolean coreCell) {
    this.attributeId = attributeId;
    this.feature = feature;
    this.sci = sci;
    this.performanceIndex = performanceIndex;
    this.coreCell = coreCell;
  }

  //to find elements in CongestionCluster
  public Cell(Integer cellId) {
    this.attributeId = cellId;
  }

  public void setAsCoreCell() {
    this.coreCell = true;
  }

  public SimpleFeature getFeature() {
    return feature;
  }

  public Double getSCI() {
    return sci;
  }

  public Double getPerformanceIndex() {
    return performanceIndex;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Cell) {
      Cell anotherCell = (Cell) obj;
      return this.attributeId.equals(anotherCell.attributeId);
    }
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return String.format("cell %d (%f) (core: %s) ", attributeId, performanceIndex, coreCell);
  }

}
