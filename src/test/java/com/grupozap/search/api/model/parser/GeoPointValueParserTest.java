package com.grupozap.search.api.model.parser;

import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.POLYGON;
import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.SINGLE;
import static com.grupozap.search.api.model.parser.ValueParser.GeoPoint.Type.VIEWPORT;
import static org.junit.Assert.assertEquals;

import com.grupozap.search.api.model.query.Value;
import org.jparsec.error.ParserException;
import org.junit.Test;

public class GeoPointValueParserTest {

  private final ValueParser valueParser = new ValueParser();

  @Test
  public void testPolygon() {
    Value parse =
        valueParser.getGeoPointValue(POLYGON).parse("[[42.0,-74.0],[-40.0,-72.0],[-30,-23]]");
    assertEquals("[[42.0, -74.0], [-40.0, -72.0], [-30.0, -23.0]]", parse.toString());
  }

  @Test(expected = ParserException.class)
  public void testPolygonWithLessThan3Points() {
    valueParser.getGeoPointValue(POLYGON).parse("[[42.0,-74.0],[-40.0,-72.0]]");
  }

  @Test(expected = ParserException.class)
  public void testPolygonWithInvalidPointLeft() {
    valueParser.getGeoPointValue(POLYGON).parse("[[,-74.0],[-40.0,-72.0],[-30,-23]]");
  }

  @Test(expected = ParserException.class)
  public void testPolygonWithInvalidPointRigth() {
    valueParser.getGeoPointValue(POLYGON).parse("[[42.0,-74.0],[-40.0,-72.0],[-30,]]");
  }

  @Test
  public void testViewport() {
    Value parse = valueParser.getGeoPointValue(VIEWPORT).parse("[[42.0,-74.0],[-40.0,-72.0]]");
    assertEquals("[[42.0, -74.0], [-40.0, -72.0]]", parse.toString());
  }

  @Test
  public void testSingle() {
    Value parse = valueParser.getGeoPointValue(SINGLE).parse("[42.0,-74.0]");
    assertEquals("[42.0, -74.0]", parse.toString());
  }
}
