package ar.uba.fi.algorithm.hotroutes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import ar.uba.fi.result.HotRoute;
import ar.uba.fi.result.HotRoutes;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/*
 This file is part of ELKI:
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
 * Perform FlowScan runs and verify expected values
 *
 * The datasets used in the tests (trajectories and road network) 
 * are not versioned on github to avoid problems with size restrictions
 *
 * @author Mariano Kohan
 *
 */
public class TestFlowScanResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {

  /**
   * Run FlowScan for Trucks dataset with fixed parameters and verify expected results
   *
   * @throws ParameterException
   */
  @Test
  public void testFlowScanTrucksResults() {
    System.out.println("\nFlowscan applied to Trucks dataset \n----------------------------------");
    Database db = makeSimpleDatabase(UNITTEST + "flowscan/trucks/Trucks_tr_denorm_simplified_converted_edges.txt", 112203);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(FlowScan.Parameterizer.ROAD_NETWORK_FILE_ID, UNITTEST + "flowscan/trucks/athens_greece.osm-line.shp");
    params.addParameter(FlowScan.Parameterizer.EPSILON_ID, 2);
    params.addParameter(FlowScan.Parameterizer.MIN_TRAFFIC_ID, 15);
    long startParameterizationTime = System.currentTimeMillis();
    FlowScan flowscan = ClassGenericsUtil.parameterizeOrAbort(FlowScan.class, params);
    long endParameterizationTime = System.currentTimeMillis();
    System.out.println("Parameterization time: " + (endParameterizationTime - startParameterizationTime) + " msecs");
    testParameterizationOk(params);

    // run FlowScan on database
    long startFlowScanTime = System.currentTimeMillis();
    HotRoutes hotRoutes = (HotRoutes)flowscan.run(db);
    long endFlowScanTime = System.currentTimeMillis();
    System.out.println("FlowScan time: " + ((endFlowScanTime - startFlowScanTime)/1000) + " secs");

    //verify total number of hot routes
    assertEquals("Number of discovered hot routes do not match", 177, hotRoutes.getHotRoutes().size());
    //verify number of hot routes by size
    Map<Integer,Integer> expectedNumberBySize = new HashMap<Integer, Integer>();
    expectedNumberBySize.put(1, 89);
    expectedNumberBySize.put(2, 27);
    expectedNumberBySize.put(3, 12);
    expectedNumberBySize.put(4, 12);
    expectedNumberBySize.put(5, 7);
    expectedNumberBySize.put(6, 3);
    expectedNumberBySize.put(7, 3);
    expectedNumberBySize.put(8, 2);
    expectedNumberBySize.put(9, 5);
    expectedNumberBySize.put(10, 2);
    expectedNumberBySize.put(11, 3);
    expectedNumberBySize.put(12, 1);
    expectedNumberBySize.put(13, 2);
    expectedNumberBySize.put(14, 2);
    expectedNumberBySize.put(15, 2);
    expectedNumberBySize.put(16, 3);
    expectedNumberBySize.put(20, 2);
    testHotRoutesNumberBySize(hotRoutes, expectedNumberBySize);
    //verify content for hot routes of size 20
    List<List<String>> expectedHotRoutesSize20EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize20EdgeIds.add(Arrays.asList(new String[] {"athens_greece.osm-line.76813", "athens_greece.osm-line.76176", "athens_greece.osm-line.76169", "athens_greece.osm-line.76821", "athens_greece.osm-line.77457", "athens_greece.osm-line.77713", "athens_greece.osm-line.78632", "athens_greece.osm-line.79345", "athens_greece.osm-line.79916", "athens_greece.osm-line.81208", "athens_greece.osm-line.82362", "athens_greece.osm-line.82805", "athens_greece.osm-line.82833", "athens_greece.osm-line.82195", "athens_greece.osm-line.81209", "athens_greece.osm-line.79853", "athens_greece.osm-line.79033", "athens_greece.osm-line.78609", "athens_greece.osm-line.77702", "athens_greece.osm-line.77526"}));
    expectedHotRoutesSize20EdgeIds.add(Arrays.asList(new String[] {"athens_greece.osm-line.75291", "athens_greece.osm-line.75490", "athens_greece.osm-line.76169", "athens_greece.osm-line.76821", "athens_greece.osm-line.77457", "athens_greece.osm-line.77713", "athens_greece.osm-line.78632", "athens_greece.osm-line.79345", "athens_greece.osm-line.79916", "athens_greece.osm-line.81208", "athens_greece.osm-line.82362", "athens_greece.osm-line.82805", "athens_greece.osm-line.82833", "athens_greece.osm-line.82195", "athens_greece.osm-line.81209", "athens_greece.osm-line.79853", "athens_greece.osm-line.79033", "athens_greece.osm-line.78609", "athens_greece.osm-line.77702", "athens_greece.osm-line.77526"}));
    testExpectedHotRoutesContent(hotRoutes, 20, expectedHotRoutesSize20EdgeIds);
    //verify content for hot routes of size 16
    List<List<String>> expectedHotRoutesSize16EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize16EdgeIds.add(Arrays.asList(new String[] {"athens_greece.osm-line.76813", "athens_greece.osm-line.76176", "athens_greece.osm-line.76169", "athens_greece.osm-line.76821", "athens_greece.osm-line.77457", "athens_greece.osm-line.77713", "athens_greece.osm-line.78632", "athens_greece.osm-line.79345", "athens_greece.osm-line.79916", "athens_greece.osm-line.81208", "athens_greece.osm-line.82362", "athens_greece.osm-line.82805", "athens_greece.osm-line.82833", "athens_greece.osm-line.82195", "athens_greece.osm-line.80970", "athens_greece.osm-line.79033"}));
    expectedHotRoutesSize16EdgeIds.add(Arrays.asList(new String[] {"athens_greece.osm-line.75291", "athens_greece.osm-line.75490", "athens_greece.osm-line.76169", "athens_greece.osm-line.76821", "athens_greece.osm-line.77457", "athens_greece.osm-line.77713", "athens_greece.osm-line.78632", "athens_greece.osm-line.79345", "athens_greece.osm-line.79916", "athens_greece.osm-line.81208", "athens_greece.osm-line.82362", "athens_greece.osm-line.82805", "athens_greece.osm-line.82833", "athens_greece.osm-line.82195", "athens_greece.osm-line.80970", "athens_greece.osm-line.79033"}));
    expectedHotRoutesSize16EdgeIds.add(Arrays.asList(new String[] {"athens_greece.osm-line.77457", "athens_greece.osm-line.77713", "athens_greece.osm-line.78632", "athens_greece.osm-line.79345", "athens_greece.osm-line.79916", "athens_greece.osm-line.81208", "athens_greece.osm-line.82362", "athens_greece.osm-line.82805", "athens_greece.osm-line.82833", "athens_greece.osm-line.82195", "athens_greece.osm-line.81209", "athens_greece.osm-line.79853", "athens_greece.osm-line.79033", "athens_greece.osm-line.78609", "athens_greece.osm-line.77702", "athens_greece.osm-line.77526"}));
    testExpectedHotRoutesContent(hotRoutes, 16, expectedHotRoutesSize16EdgeIds);

  }

  private Map<Integer, Integer> getHotRoutesSizeCounters(HotRoutes hotRoutes) {
    Map<Integer, Integer> hotRouteSizeCounters = new HashMap<Integer, Integer>();
    for(HotRoute hotRoute : hotRoutes.getHotRoutes()) {
      Integer hotRouteSizeCounter = hotRouteSizeCounters.get(hotRoute.getLength());
      if (hotRouteSizeCounter == null) {
        hotRouteSizeCounters.put(hotRoute.getLength(), new Integer(1));
      } else {
        hotRouteSizeCounters.put(hotRoute.getLength(), hotRouteSizeCounter + 1);
      }
    }
    return hotRouteSizeCounters;
  }

  protected void testHotRoutesNumberBySize(HotRoutes hotRoutes, Map expectedSizes) {
    Map<Integer, Integer> hotRouteSizeCounters = getHotRoutesSizeCounters(hotRoutes);
    for(Entry<Integer, Integer> sizeCounterEntry : hotRouteSizeCounters.entrySet()) {
      assertEquals("Number of hot routes with size " + sizeCounterEntry.getKey() + " do not match", expectedSizes.get(sizeCounterEntry.getKey()), sizeCounterEntry.getValue());
    }
  }

  private void testExpectedHotRoutesContent(HotRoutes hotRoutes, int size, List<List<String>> expectedHotRoutesSizeEdgeIds) {
    for(HotRoute hotRoute : hotRoutes.getHotRoutes()) {
      if (hotRoute.getLength() == size) {
        List<String> hotRouteEdgeIds = hotRoute.getLastEdgesIds(size); //TODO: could be improved
        boolean expectedRoute = false;
        Iterator expectedHotRoutesSizeEdgeIdsIterator = expectedHotRoutesSizeEdgeIds.iterator();
        while (!expectedRoute && expectedHotRoutesSizeEdgeIdsIterator.hasNext()) {
          expectedRoute = expectedHotRoutesSizeEdgeIdsIterator.next().equals(hotRouteEdgeIds);
        }
        assertTrue("Hot Route of size " + size + " do not expected - edgeIds: " + hotRouteEdgeIds , expectedRoute);
      }
    }
  }

  /**
   * Run FlowScan for roma/taxi (crawdad) dataset with fixed parameters and verify expected results
   *
   * @throws ParameterException
   */
  @Test
  public void testFlowScanRomaTaxiResults() {
    System.out.println("\nFlowscan applied to roma/taxi (crawdad) dataset \n-----------------------------------------------");
    Database db = makeSimpleDatabase(UNITTEST + "flowscan/crawdad/roma_taxi/taxi_february__sample_hd_100M__time_pos_converted_edges__sorted.txt", 99748);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(FlowScan.Parameterizer.ROAD_NETWORK_FILE_ID, UNITTEST + "flowscan/crawdad/roma_taxi/rome_italy_osm_line.shp");
    params.addParameter(FlowScan.Parameterizer.EPSILON_ID, 2);
    params.addParameter(FlowScan.Parameterizer.MIN_TRAFFIC_ID, 10);
    long startParameterizationTime = System.currentTimeMillis();
    FlowScan flowscan = ClassGenericsUtil.parameterizeOrAbort(FlowScan.class, params);
    long endParameterizationTime = System.currentTimeMillis();
    System.out.println("Parameterization time: " + (endParameterizationTime - startParameterizationTime) + " msecs");
    testParameterizationOk(params);

    // run FlowScan on database
    long startFlowScanTime = System.currentTimeMillis();
    HotRoutes hotRoutes = (HotRoutes)flowscan.run(db);
    long endFlowScanTime = System.currentTimeMillis();
    System.out.println("FlowScan time: " + ((endFlowScanTime - startFlowScanTime)/1000) + " secs");

    //verify total number of hot routes
    assertEquals("Number of discovered hot routes do not match", 410, hotRoutes.getHotRoutes().size());
    //verify number of hot routes by size
    Map<Integer,Integer> expectedNumberBySize = new HashMap<Integer, Integer>();
    expectedNumberBySize.put(1, 207);
    expectedNumberBySize.put(2, 110);
    expectedNumberBySize.put(3, 37);
    expectedNumberBySize.put(4, 26);
    expectedNumberBySize.put(5, 17);
    expectedNumberBySize.put(6, 6);
    expectedNumberBySize.put(7, 5);
    expectedNumberBySize.put(10, 1);
    expectedNumberBySize.put(11, 1);

    testHotRoutesNumberBySize(hotRoutes, expectedNumberBySize);
    //verify content for hot routes of size 11
    List<List<String>> expectedHotRoutesSize11EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize11EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.4835", "rome_italy_osm_line.5097", "rome_italy_osm_line.5233", "rome_italy_osm_line.5395", "rome_italy_osm_line.7266", "rome_italy_osm_line.9048", "rome_italy_osm_line.10229", "rome_italy_osm_line.10837", "rome_italy_osm_line.11080", "rome_italy_osm_line.14107", "rome_italy_osm_line.16726"}));
    testExpectedHotRoutesContent(hotRoutes, 11, expectedHotRoutesSize11EdgeIds);
    //verify content for hot routes of size 10
    List<List<String>> expectedHotRoutesSize10EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize10EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.5097", "rome_italy_osm_line.5233", "rome_italy_osm_line.5395", "rome_italy_osm_line.7266", "rome_italy_osm_line.9048", "rome_italy_osm_line.10229", "rome_italy_osm_line.10837", "rome_italy_osm_line.11080", "rome_italy_osm_line.14107", "rome_italy_osm_line.16726"}));
    testExpectedHotRoutesContent(hotRoutes, 10, expectedHotRoutesSize10EdgeIds);
    //verify content for hot routes of size 7
    List<List<String>> expectedHotRoutesSize7EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize7EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.50841", "rome_italy_osm_line.50937", "rome_italy_osm_line.52426", "rome_italy_osm_line.52369", "rome_italy_osm_line.52172", "rome_italy_osm_line.52176", "rome_italy_osm_line.53563"}));
    expectedHotRoutesSize7EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.55482", "rome_italy_osm_line.54199", "rome_italy_osm_line.52980", "rome_italy_osm_line.52559", "rome_italy_osm_line.51739", "rome_italy_osm_line.50955", "rome_italy_osm_line.50187"}));
    expectedHotRoutesSize7EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.55484", "rome_italy_osm_line.54199", "rome_italy_osm_line.52980", "rome_italy_osm_line.52559", "rome_italy_osm_line.51739", "rome_italy_osm_line.50955", "rome_italy_osm_line.50187"}));
    expectedHotRoutesSize7EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.49419", "rome_italy_osm_line.51005", "rome_italy_osm_line.52980", "rome_italy_osm_line.52559", "rome_italy_osm_line.51739", "rome_italy_osm_line.50955", "rome_italy_osm_line.50187"}));
    expectedHotRoutesSize7EdgeIds.add(Arrays.asList(new String[] {"rome_italy_osm_line.49207", "rome_italy_osm_line.51005", "rome_italy_osm_line.52980", "rome_italy_osm_line.52559", "rome_italy_osm_line.51739", "rome_italy_osm_line.50955", "rome_italy_osm_line.50187"}));
    testExpectedHotRoutesContent(hotRoutes, 7, expectedHotRoutesSize7EdgeIds);

  }

  /**
   * Run FlowScan for generated dataset with Brinkhoff generator (considering neighborhoods) with fixed parameters and verify expected results
   *
   * @throws ParameterException
   */
  @Test
  public void testFlowScanGeneratorBrinkhoffResults() {
    System.out.println("\nFlowscan applied to Brinkhoff (neighborhoods) generated dataset \n-----------------------------------------------");
    Database db = makeSimpleDatabase(UNITTEST + "flowscan/generators/Brinkhoff_neighborhoods/sanfrancisco_3600_30-1_100__sorted_converted_edges.txt", 1452745);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(FlowScan.Parameterizer.ROAD_NETWORK_FILE_ID, UNITTEST + "flowscan/generators/Brinkhoff_neighborhoods/san-francisco_california_osm_line.shp");
    params.addParameter(FlowScan.Parameterizer.EPSILON_ID, 6);
    params.addParameter(FlowScan.Parameterizer.MIN_TRAFFIC_ID, 300);
    long startParameterizationTime = System.currentTimeMillis();
    FlowScan flowscan = ClassGenericsUtil.parameterizeOrAbort(FlowScan.class, params);
    long endParameterizationTime = System.currentTimeMillis();
    System.out.println("Parameterization time: " + (endParameterizationTime - startParameterizationTime) + " msecs");
    testParameterizationOk(params);

    // run FlowScan on database
    long startFlowScanTime = System.currentTimeMillis();
    HotRoutes hotRoutes = (HotRoutes)flowscan.run(db);
    long endFlowScanTime = System.currentTimeMillis();
    System.out.println("FlowScan time: " + ((endFlowScanTime - startFlowScanTime)/1000) + " secs");

    //verify total number of hot routes
    assertEquals("Number of discovered hot routes do not match", 406, hotRoutes.getHotRoutes().size());
    //verify number of hot routes by size
    Map<Integer,Integer> expectedNumberBySize = new HashMap<Integer, Integer>();
    expectedNumberBySize.put(1, 223);
    expectedNumberBySize.put(2, 65);
    expectedNumberBySize.put(3, 44);
    expectedNumberBySize.put(4, 29);
    expectedNumberBySize.put(5, 13);
    expectedNumberBySize.put(6, 15);
    expectedNumberBySize.put(7, 1);
    expectedNumberBySize.put(8, 4);
    expectedNumberBySize.put(9, 3);
    expectedNumberBySize.put(10, 3);
    expectedNumberBySize.put(12, 1);
    expectedNumberBySize.put(13, 1);
    expectedNumberBySize.put(14, 1);
    expectedNumberBySize.put(15, 1);
    expectedNumberBySize.put(16, 1);
    expectedNumberBySize.put(17, 1);

    testHotRoutesNumberBySize(hotRoutes, expectedNumberBySize);
    //verify content for hot routes of size 17
    List<List<String>> expectedHotRoutesSize17EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize17EdgeIds.add(Arrays.asList(new String[] {"san-francisco_california_osm_line.7643", "san-francisco_california_osm_line.6612", "san-francisco_california_osm_line.3706", "san-francisco_california_osm_line.2406", "san-francisco_california_osm_line.2326", "san-francisco_california_osm_line.1059", "san-francisco_california_osm_line.852", "san-francisco_california_osm_line.734", "san-francisco_california_osm_line.575", "san-francisco_california_osm_line.567", "san-francisco_california_osm_line.549", "san-francisco_california_osm_line.430", "san-francisco_california_osm_line.403", "san-francisco_california_osm_line.360", "san-francisco_california_osm_line.346", "san-francisco_california_osm_line.336", "san-francisco_california_osm_line.278"}));
    testExpectedHotRoutesContent(hotRoutes, 17, expectedHotRoutesSize17EdgeIds);
    //verify content for hot routes of size 16
    List<List<String>> expectedHotRoutesSize16EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize16EdgeIds.add(Arrays.asList(new String[] {"san-francisco_california_osm_line.6612", "san-francisco_california_osm_line.3706", "san-francisco_california_osm_line.2406", "san-francisco_california_osm_line.2326", "san-francisco_california_osm_line.1059", "san-francisco_california_osm_line.852", "san-francisco_california_osm_line.734", "san-francisco_california_osm_line.575", "san-francisco_california_osm_line.567", "san-francisco_california_osm_line.549", "san-francisco_california_osm_line.430", "san-francisco_california_osm_line.403", "san-francisco_california_osm_line.360", "san-francisco_california_osm_line.346", "san-francisco_california_osm_line.336", "san-francisco_california_osm_line.278"}));
    testExpectedHotRoutesContent(hotRoutes, 16, expectedHotRoutesSize16EdgeIds);
    //verify content for hot routes of size 15
    List<List<String>> expectedHotRoutesSize15EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize15EdgeIds.add(Arrays.asList(new String[] {"san-francisco_california_osm_line.3706", "san-francisco_california_osm_line.2406", "san-francisco_california_osm_line.2326", "san-francisco_california_osm_line.1059", "san-francisco_california_osm_line.852", "san-francisco_california_osm_line.734", "san-francisco_california_osm_line.575", "san-francisco_california_osm_line.567", "san-francisco_california_osm_line.549", "san-francisco_california_osm_line.430", "san-francisco_california_osm_line.403", "san-francisco_california_osm_line.360", "san-francisco_california_osm_line.346", "san-francisco_california_osm_line.336", "san-francisco_california_osm_line.278"}));
    testExpectedHotRoutesContent(hotRoutes, 15, expectedHotRoutesSize15EdgeIds);
    //verify content for hot routes of size 14
    List<List<String>> expectedHotRoutesSize14EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize14EdgeIds.add(Arrays.asList(new String[] {"san-francisco_california_osm_line.2406", "san-francisco_california_osm_line.2326", "san-francisco_california_osm_line.1059", "san-francisco_california_osm_line.852", "san-francisco_california_osm_line.734", "san-francisco_california_osm_line.575", "san-francisco_california_osm_line.567", "san-francisco_california_osm_line.549", "san-francisco_california_osm_line.430", "san-francisco_california_osm_line.403", "san-francisco_california_osm_line.360", "san-francisco_california_osm_line.346", "san-francisco_california_osm_line.336", "san-francisco_california_osm_line.278"}));
    testExpectedHotRoutesContent(hotRoutes, 14, expectedHotRoutesSize14EdgeIds);
    //verify content for hot routes of size 13
    List<List<String>> expectedHotRoutesSize13EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize13EdgeIds.add(Arrays.asList(new String[] {"san-francisco_california_osm_line.2326", "san-francisco_california_osm_line.1059", "san-francisco_california_osm_line.852", "san-francisco_california_osm_line.734", "san-francisco_california_osm_line.575", "san-francisco_california_osm_line.567", "san-francisco_california_osm_line.549", "san-francisco_california_osm_line.430", "san-francisco_california_osm_line.403", "san-francisco_california_osm_line.360", "san-francisco_california_osm_line.346", "san-francisco_california_osm_line.336", "san-francisco_california_osm_line.278"}));
    testExpectedHotRoutesContent(hotRoutes, 13, expectedHotRoutesSize13EdgeIds);
    //verify content for hot routes of size 12
    List<List<String>> expectedHotRoutesSize12EdgeIds = new LinkedList<List<String>>();
    expectedHotRoutesSize12EdgeIds.add(Arrays.asList(new String[] {"san-francisco_california_osm_line.1059", "san-francisco_california_osm_line.852", "san-francisco_california_osm_line.734", "san-francisco_california_osm_line.575", "san-francisco_california_osm_line.567", "san-francisco_california_osm_line.549", "san-francisco_california_osm_line.430", "san-francisco_california_osm_line.403", "san-francisco_california_osm_line.360", "san-francisco_california_osm_line.346", "san-francisco_california_osm_line.336", "san-francisco_california_osm_line.278"}));
    testExpectedHotRoutesContent(hotRoutes, 12, expectedHotRoutesSize12EdgeIds);

  }

}
