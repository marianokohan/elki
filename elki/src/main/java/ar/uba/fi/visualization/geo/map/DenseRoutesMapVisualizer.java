package ar.uba.fi.visualization.geo.map;

import java.util.List;

import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.utils.MapUtils;

public class DenseRoutesMapVisualizer extends RoutesMapVisualizer {

  protected String denseRoutesEdgesFileName = "dense_routes_edges.json";
  protected int edgeColor = color(255, 127, 0);
  protected int edgeWeight = 10;
  protected String denseRoutesStartsFileName = "dense_routes_starts.json";
  protected String denseRoutesEndsFileName = "dense_routes_ends.json";

  @Override
  public void setup() {
      size(1400, 900);
      providerSetup();
      // Add mouse and keyboard interactions
      MapUtils.createDefaultEventDispatcher(this, map);
      map.zoomLevel(12);

      List<Feature> startPoints = GeoJSONReaderMultiLineString.loadData(this, denseRoutesStartsFileName);
      Location panLocation = defaultBeijingLocation;
      if (!startPoints.isEmpty()) {
        panLocation = ((PointFeature)startPoints.get(0)).getLocation();
      }
      map.panTo(panLocation);

      addEdgeMarkers(denseRoutesEdgesFileName, edgeColor, edgeWeight);

      List<Marker> startPointsMarkers = createStartPointsMarkers(startPoints);
      map.addMarkers(startPointsMarkers);
      List<Feature> endPoints = GeoJSONReaderMultiLineString.loadData(this, denseRoutesEndsFileName);
      List<Marker> endPointsMarkers = createEndPointsMarkers(endPoints);
      map.addMarkers(endPointsMarkers);
  }

}
