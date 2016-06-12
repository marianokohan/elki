package ar.uba.fi.algorithm.coldroutes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import ar.uba.fi.result.ColdRoute;
import ar.uba.fi.result.ColdRoutes;
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
 * Perform ColdScan run and verify expected values
 *
 * The datasets used in the tests (trajectories and road network)
 * are not versioned on github to avoid problems with size restrictions
 *
 * @author Mariano Kohan
 *
 */
public class TestColdScanResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {

  /**
   * Run ColdScan for generated dataset with Brinkhoff generator (considering neighborhoods) with fixed parameters and verify expected results
   *  (use results from JamFlowScan execution)
   *
   * @throws ParameterException
   */
  @Test
  public void testColdScanScanGeneratorBrinkhoffResults() {
    System.out.println("\nColdScan applied to Brinkhoff (jr neighborhoods v2) generated dataset \n------------------------------------------------------------------");
    Database db = makeSimpleDatabase(UNITTEST + "coldscan/generators/Brinkhoff_neighborhoods/sanfrancisco_3600_30-3_s150_jrneighsv2__sorted_converted_edges.txt", 16345285);

    ListParameterization params = new ListParameterization();
    params.addParameter(ColdScan.Parameterizer.ROAD_NETWORK_FILE_ID, UNITTEST + "coldscan/generators/Brinkhoff_neighborhoods/san-francisco_california_osm_line.shp");
    params.addParameter(ColdScan.Parameterizer.MAX_TRAFFIC_ID, 3);
    params.addParameter(ColdScan.Parameterizer.EXPAND_X_BR, 0.4);
    params.addParameter(ColdScan.Parameterizer.EXPAND_Y_BR, 0.4);
    params.addParameter(ColdScan.Parameterizer.ROAD_JAM_ROUTES_FILE_ID, UNITTEST + "coldscan/generators/Brinkhoff_neighborhoods/jam_routes_eps2_minTraffic300_jamSpeed22.txt");

    long startParameterizationTime = System.currentTimeMillis();
    ColdScan coldScan = ClassGenericsUtil.parameterizeOrAbort(ColdScan.class, params);
    long endParameterizationTime = System.currentTimeMillis();
    System.out.println("Parameterization time: " + (endParameterizationTime - startParameterizationTime) + " msecs");
    testParameterizationOk(params);

    // run ColdScan on database
    long startColdScanTime = System.currentTimeMillis();
    ColdRoutes coldRoutes = (ColdRoutes)coldScan.run(db);
    long endColdScanTime = System.currentTimeMillis();
    System.out.println("ColdScan time: " + ((endColdScanTime - startColdScanTime)/1000) + " secs");

    //verify total number of cold routes
    assertEquals("Number of discovered cold routes do not match", 50, coldRoutes.getColdRoutes().size());
    //verify number of cold routes by size
    Map<Integer,Integer> expectedNumberBySize = new HashMap<Integer, Integer>();
    expectedNumberBySize.put(1, 41);
    expectedNumberBySize.put(2, 5);
    expectedNumberBySize.put(3, 1);
    expectedNumberBySize.put(4, 1);
    expectedNumberBySize.put(6, 1);
    expectedNumberBySize.put(10, 1);

    testColdRoutesNumbersBySize(coldRoutes, expectedNumberBySize);

    //verify content for cold routes of size 10
    List<String> expectedColdRoutesSize10 = new LinkedList<String>();
    expectedColdRoutesSize10.add("cold route (10 edges): san-francisco_california_osm_line.15824 -> san-francisco_california_osm_line.16161 -> san-francisco_california_osm_line.16177 -> san-francisco_california_osm_line.19707 -> san-francisco_california_osm_line.19740 -> san-francisco_california_osm_line.20062 -> san-francisco_california_osm_line.20393 -> san-francisco_california_osm_line.20675 -> san-francisco_california_osm_line.20966 -> san-francisco_california_osm_line.21883 (COLD)");
    testExpectedColdRoutesContent(coldRoutes, 10, expectedColdRoutesSize10);

    //verify content for cold routes of size 6
    List<String> expectedColdRoutesSize6 = new LinkedList<String>();
    expectedColdRoutesSize6.add("cold route (6 edges): san-francisco_california_osm_line.24142 -> san-francisco_california_osm_line.23900 -> san-francisco_california_osm_line.23651 -> san-francisco_california_osm_line.23620 -> san-francisco_california_osm_line.23478 -> san-francisco_california_osm_line.23449 (COLD)");
    testExpectedColdRoutesContent(coldRoutes, 6, expectedColdRoutesSize6);

    //verify content for cold routes of size 4
    List<String> expectedColdRoutesSize4 = new LinkedList<String>();
    expectedColdRoutesSize4.add("cold route (4 edges): san-francisco_california_osm_line.25974 -> san-francisco_california_osm_line.26391 -> san-francisco_california_osm_line.26738 -> san-francisco_california_osm_line.26773 (COLD)");
    testExpectedColdRoutesContent(coldRoutes, 4, expectedColdRoutesSize4);
  }

  private Map<Integer, Integer> getColdRoutesSizeCounters(ColdRoutes coldRoutes) {
    Map<Integer, Integer> coldRouteSizeCounters = new HashMap<Integer, Integer>();
    for(ColdRoute coldRoute : coldRoutes.getColdRoutes()) {
      Integer coldRouteSizeCounter = coldRouteSizeCounters.get(coldRoute.getLength());
      if (coldRouteSizeCounter == null) {
        coldRouteSizeCounters.put(coldRoute.getLength(), new Integer(1));
      } else {
        coldRouteSizeCounters.put(coldRoute.getLength(), coldRouteSizeCounter + 1);
      }
    }
    return coldRouteSizeCounters;
  }

  protected void testColdRoutesNumbersBySize(ColdRoutes coldRoutes, Map expectedSizes) {
    Map<Integer, Integer> coldRouteSizeCounters = getColdRoutesSizeCounters(coldRoutes);
    for(Entry<Integer, Integer> sizeCounterEntry : coldRouteSizeCounters.entrySet()) {
      assertEquals("Number of cold routes with size " + sizeCounterEntry.getKey() + " do not match", expectedSizes.get(sizeCounterEntry.getKey()), sizeCounterEntry.getValue());
    }
  }

  private void testExpectedColdRoutesContent(ColdRoutes coldRoutes, int size, List<String> expectedcoldRoutes) {
    for(ColdRoute coldRoute : coldRoutes.getColdRoutes()) {
      if (coldRoute.getLength() == size) {
        String coldRouteString = coldRoute.toString();
        boolean expectedRoute = false;
        Iterator expectedColdRoutesIterator = expectedcoldRoutes.iterator();
        while (!expectedRoute && expectedColdRoutesIterator.hasNext()) {
          expectedRoute = expectedColdRoutesIterator.next().equals(coldRouteString);
        }
        assertTrue("Cold Route of size " + size + " do not expected: " + coldRouteString , expectedRoute);
      }
    }
  }

}
