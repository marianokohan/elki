package ar.uba.fi.visualization.geo.map.markers;

import processing.core.PGraphics;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;

public class StartPointMarker extends SimplePointMarker {

  public StartPointMarker(Location location) {
    super(location);
  }

  @Override
  public void draw(PGraphics pg, float x, float y) {
    pg.pushStyle();
    pg.fill(255,249,91);
    pg.rect(x-10, y-10, 20, 20);
    pg.popStyle();
  }

}
