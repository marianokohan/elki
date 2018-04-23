package ar.uba.fi.visualization.geo.map;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ar.uba.fi.visualization.geo.map.unfolding.GeoJSONReaderMultiLineString;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimplePolygonMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;

public class CongestionClustersMapVisualizer extends RoutesMapVisualizer {

  protected String congestionClustersCellFeaturesFileName = "congestion_clusters_cells.json";
  protected int cellColor = color(255, 100, 50, 125);
  protected int cellBorderWeight = 2;

  @Override
  public void setup() {
      size(1400, 900);
      providerSetup();
      // Add mouse and keyboard interactions
      MapUtils.createDefaultEventDispatcher(this, map);
      map.zoomLevel(12);

      Location panLocation = defaultBeijingLocation;
      File cellFeaturesFile = new File(congestionClustersCellFeaturesFileName);
      if (cellFeaturesFile.exists()) {
        List<Feature> cellFeatures = GeoJSONReaderMultiLineString.loadData(this, cellFeaturesFile);

        panLocation = ((ShapeFeature)cellFeatures.get(0)).getLocations().get(0);

        List<Marker> cellMarkers = createCellMarkers(cellFeatures, cellColor, cellBorderWeight);
        map.addMarkers(cellMarkers);
      }

      map.panTo(panLocation);
  }

  protected List<Marker> createCellMarkers(List<Feature> cells, int color, int weight) {
    List<Marker> cellMarkers = new ArrayList<Marker>();
    for (Feature feature : cells) {
      ShapeFeature cellFeature = (ShapeFeature) feature;
      SimplePolygonMarker cellMarker = new SimplePolygonMarker(cellFeature.getLocations());
      cellMarker.setColor(color);
      cellMarker.setStrokeColor(color);
      cellMarker.setStrokeWeight(weight);
      cellMarkers.add(cellMarker);
    }
    return cellMarkers;
  }

}
