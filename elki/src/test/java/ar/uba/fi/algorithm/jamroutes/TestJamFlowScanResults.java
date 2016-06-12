package ar.uba.fi.algorithm.jamroutes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import ar.uba.fi.result.JamRoute;
import ar.uba.fi.result.JamRoutes;
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

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

/**
 * Perform JamFlowScan runs and verify expected values
 *
 * The datasets used in the tests (trajectories and road network)
 * are not versioned on github to avoid problems with size restrictions
 *
 * @author Mariano Kohan
 *
 */
public class TestJamFlowScanResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {

  /**
   * Run JamFlowScan for generated dataset with Brinkhoff generator (considering neighborhoods) with fixed parameters and verify expected results
   *
   * @throws ParameterException
   */
  @Test
  public void testJamFlowScanGeneratorBrinkhoffResults() {
    System.out.println("\nJamFlowscan applied to Brinkhoff (neighborhoods) generated dataset \n------------------------------------------------------------------");
    Database db = makeSimpleDatabase(UNITTEST + "jamflowscan/generators/Brinkhoff_neighborhoods/sanfrancisco_3600_30-1_100__sorted_converted_edges.txt", 1452745);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(JamFlowScan.Parameterizer.ROAD_NETWORK_FILE_ID, UNITTEST + "jamflowscan/generators/Brinkhoff_neighborhoods/san-francisco_california_osm_line.shp");
    params.addParameter(JamFlowScan.Parameterizer.EPSILON_ID, 6);
    params.addParameter(JamFlowScan.Parameterizer.MIN_TRAFFIC_ID, 300);
    params.addParameter(JamFlowScan.Parameterizer.JAM_SPEED_ID, 50);
    long startParameterizationTime = System.currentTimeMillis();
    JamFlowScan jamFlowscan = ClassGenericsUtil.parameterizeOrAbort(JamFlowScan.class, params);
    long endParameterizationTime = System.currentTimeMillis();
    System.out.println("Parameterization time: " + (endParameterizationTime - startParameterizationTime) + " msecs");
    testParameterizationOk(params);

    // run JamFlowScan on database
    long startJamFlowScanTime = System.currentTimeMillis();
    JamRoutes jamRoutes = (JamRoutes)jamFlowscan.run(db);
    long endJamFlowScanTime = System.currentTimeMillis();
    System.out.println("JamFlowScan time: " + ((endJamFlowScanTime - startJamFlowScanTime)/1000) + " secs");

    //verify total number of jam routes
    assertEquals("Number of discovered jam routes do not match", 406, jamRoutes.getJamRoutes().size());
    //verify number of jam routes by size
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

    Map<Integer,Integer> expectedNumberWithJamsBySize = new HashMap<Integer, Integer>();
    expectedNumberWithJamsBySize.put(1, 7);
    expectedNumberWithJamsBySize.put(2, 5);
    expectedNumberWithJamsBySize.put(3, 4);
    expectedNumberWithJamsBySize.put(4, 3);
    expectedNumberWithJamsBySize.put(5, 1);
    expectedNumberWithJamsBySize.put(6, 1);
    expectedNumberWithJamsBySize.put(7, 0);
    expectedNumberWithJamsBySize.put(8, 0);
    expectedNumberWithJamsBySize.put(9, 0);
    expectedNumberWithJamsBySize.put(10, 0);
    expectedNumberWithJamsBySize.put(12, 0);
    expectedNumberWithJamsBySize.put(13, 0);
    expectedNumberWithJamsBySize.put(14, 0);
    expectedNumberWithJamsBySize.put(15, 0);
    expectedNumberWithJamsBySize.put(16, 0);
    expectedNumberWithJamsBySize.put(17, 0);

    testJamRoutesNumbersBySize(jamRoutes, expectedNumberBySize, expectedNumberWithJamsBySize);

    //verify content for jam routes of size 17
    List<String> expectedJamRoutesSize17 = new LinkedList<String>();
    expectedJamRoutesSize17.add("jam route (17 edges): san-francisco_california_osm_line.7643 -> san-francisco_california_osm_line.6612 -> san-francisco_california_osm_line.3706 -> san-francisco_california_osm_line.2406 -> san-francisco_california_osm_line.2326 -> san-francisco_california_osm_line.1059 -> san-francisco_california_osm_line.852 -> san-francisco_california_osm_line.734 -> san-francisco_california_osm_line.575 -> san-francisco_california_osm_line.567 -> san-francisco_california_osm_line.549 -> san-francisco_california_osm_line.430 -> san-francisco_california_osm_line.403 -> san-francisco_california_osm_line.360 -> san-francisco_california_osm_line.346 -> san-francisco_california_osm_line.336 -> san-francisco_california_osm_line.278");
    testExpectedJamRoutesContent(jamRoutes, 17, false, expectedJamRoutesSize17);

    //verify content for jam routes of size 16
    List<String> expectedJamRoutesSize16 = new LinkedList<String>();
    expectedJamRoutesSize16.add("jam route (16 edges): san-francisco_california_osm_line.6612 -> san-francisco_california_osm_line.3706 -> san-francisco_california_osm_line.2406 -> san-francisco_california_osm_line.2326 -> san-francisco_california_osm_line.1059 -> san-francisco_california_osm_line.852 -> san-francisco_california_osm_line.734 -> san-francisco_california_osm_line.575 -> san-francisco_california_osm_line.567 -> san-francisco_california_osm_line.549 -> san-francisco_california_osm_line.430 -> san-francisco_california_osm_line.403 -> san-francisco_california_osm_line.360 -> san-francisco_california_osm_line.346 -> san-francisco_california_osm_line.336 -> san-francisco_california_osm_line.278");
    testExpectedJamRoutesContent(jamRoutes, 16, false, expectedJamRoutesSize16);

    //verify content for jam routes of size 15
    List<String> expectedJamRoutesSize15 = new LinkedList<String>();
    expectedJamRoutesSize15.add("jam route (15 edges): san-francisco_california_osm_line.3706 -> san-francisco_california_osm_line.2406 -> san-francisco_california_osm_line.2326 -> san-francisco_california_osm_line.1059 -> san-francisco_california_osm_line.852 -> san-francisco_california_osm_line.734 -> san-francisco_california_osm_line.575 -> san-francisco_california_osm_line.567 -> san-francisco_california_osm_line.549 -> san-francisco_california_osm_line.430 -> san-francisco_california_osm_line.403 -> san-francisco_california_osm_line.360 -> san-francisco_california_osm_line.346 -> san-francisco_california_osm_line.336 -> san-francisco_california_osm_line.278");
    testExpectedJamRoutesContent(jamRoutes, 15, false, expectedJamRoutesSize15);

    //verify content for jam routes with jams of size 6
    List<String> expectedJamRoutesWithJamsSize6 = new LinkedList<String>();
    expectedJamRoutesWithJamsSize6.add("jam route (6 edges): san-francisco_california_osm_line.34285 -> san-francisco_california_osm_line.32403 -> san-francisco_california_osm_line.29819 (JAM) -> san-francisco_california_osm_line.29514 (JAM) -> san-francisco_california_osm_line.29372 -> san-francisco_california_osm_line.26335 (JAM)");
    testExpectedJamRoutesContent(jamRoutes, 6, true, expectedJamRoutesWithJamsSize6);

    //verify content for jam routes with jams of size 5
    List<String> expectedJamRoutesWithJamsSize5 = new LinkedList<String>();
    expectedJamRoutesWithJamsSize5.add("jam route (5 edges): san-francisco_california_osm_line.32403 -> san-francisco_california_osm_line.29819 (JAM) -> san-francisco_california_osm_line.29514 (JAM) -> san-francisco_california_osm_line.29372 -> san-francisco_california_osm_line.26335 (JAM)");
    testExpectedJamRoutesContent(jamRoutes, 5, true, expectedJamRoutesWithJamsSize5);

    //verify content for jam routes with jams of size 4
    List<String> expectedJamRoutesWithJamsSize4 = new LinkedList<String>();
    expectedJamRoutesWithJamsSize4.add("jam route (4 edges): san-francisco_california_osm_line.29517 (JAM) -> san-francisco_california_osm_line.32409 (JAM) -> san-francisco_california_osm_line.33217 -> san-francisco_california_osm_line.33409");
    expectedJamRoutesWithJamsSize4.add("jam route (4 edges): san-francisco_california_osm_line.35624 -> san-francisco_california_osm_line.32403 -> san-francisco_california_osm_line.29819 (JAM) -> san-francisco_california_osm_line.29514 (JAM)");
    expectedJamRoutesWithJamsSize4.add("jam route (4 edges): san-francisco_california_osm_line.35624 -> san-francisco_california_osm_line.34285 -> san-francisco_california_osm_line.32403 -> san-francisco_california_osm_line.29819 (JAM)");
    testExpectedJamRoutesContent(jamRoutes, 4, true, expectedJamRoutesWithJamsSize4);

  }

  class JamRouteSizesCounter {
    int total = 0;
    int jams = 0;
  }

  private Map<Integer, JamRouteSizesCounter> getJamRoutesSizeCounters(JamRoutes jamRoutes) {
    Map<Integer, JamRouteSizesCounter> jamRouteSizesCounters = new HashMap<Integer, JamRouteSizesCounter>();
    for(JamRoute jamRoute : jamRoutes.getJamRoutes()) {
      JamRouteSizesCounter jamRouteSizesCounter = jamRouteSizesCounters.get(jamRoute.getLength());
      if (jamRouteSizesCounter == null) {
        jamRouteSizesCounter = new JamRouteSizesCounter();
        jamRouteSizesCounters.put(jamRoute.getLength(), jamRouteSizesCounter);
      }
      jamRouteSizesCounter.total++;
      if (jamRoute.containsJams()) {
        jamRouteSizesCounter.jams++;
      }
    }
    return jamRouteSizesCounters;
  }

  protected void testJamRoutesNumbersBySize(JamRoutes hotRoutes, Map expectedSizes, Map expectedSizesWithJams) {
    Map<Integer, JamRouteSizesCounter> jamRouteSizesCounters = getJamRoutesSizeCounters(hotRoutes);
    for(Entry<Integer, JamRouteSizesCounter> jamRouteSizesCountersCounterEntry : jamRouteSizesCounters.entrySet()) {
      assertEquals("Total of jam routes of size " + jamRouteSizesCountersCounterEntry.getKey() + " do not match", 
          expectedSizes.get(jamRouteSizesCountersCounterEntry.getKey()), jamRouteSizesCountersCounterEntry.getValue().total);
      assertEquals("Number of jam routes with jams of size " + jamRouteSizesCountersCounterEntry.getKey() + " do not match", 
          expectedSizesWithJams.get(jamRouteSizesCountersCounterEntry.getKey()), jamRouteSizesCountersCounterEntry.getValue().jams);
    }
  }

  private void testExpectedJamRoutesContent(JamRoutes jamRoutes, int size, boolean withJams, List<String> expectedJamRoutes) {
    for(JamRoute jamRoute : jamRoutes.getJamRoutes()) {
      if (jamRoute.getLength() == size) {
        if ((withJams && jamRoute.containsJams()) || (!withJams)) {
          String jamRouteString = jamRoute.toString();
          boolean expectedRoute = false;
          Iterator expectedJamRoutesIterator = expectedJamRoutes.iterator();
          while (!expectedRoute && expectedJamRoutesIterator.hasNext()) {
            expectedRoute = expectedJamRoutesIterator.next().equals(jamRouteString);
          }
          assertTrue("Jam Route of size " + size + (withJams? " (with jams)": "") + " do not expected: " + jamRouteString , expectedRoute);
        }
      }
    }
  }

}
