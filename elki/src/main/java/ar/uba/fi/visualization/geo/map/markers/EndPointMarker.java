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
    pg.strokeWeight(6);
    pg.stroke(255,255,0);
    pg.line(x-10, y-10, x+10, y+10);
    pg.line(x+10, y-10, x-10, y+10);
    pg.popStyle();
  }

}
