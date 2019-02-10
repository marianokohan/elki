package ar.uba.fi.visualization.geo.map.markers;

import processing.core.PGraphics;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;

public class EndPointMarker extends SimplePointMarker {

  public EndPointMarker(Location location) {
    super(location);
  }

  @Override
  public void draw(PGraphics pg, float x, float y) {
    pg.pushStyle();
    pg.strokeWeight(4);
    pg.stroke(255,246,0);
    pg.line(x-5, y-5, x+5, y+5);
    pg.line(x+5, y-5, x-5, y+5);
    pg.popStyle();
  }

}
