package com.vivareal.search.api.fixtures.model.parser;

import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.vivareal.search.api.model.parser.*;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.service.parser.IndexSettings;
import com.vivareal.search.api.service.parser.factory.FieldCache;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.LinkedMap;
import org.mockito.stubbing.Answer;

public class ParserTemplateLoader {

  public static QueryParser queryParserFixture() {
    OperatorParser operatorParser = new OperatorParser();
    FieldParser fieldParser = fieldParserFixture();
    FilterParser filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());
    return new QueryParser(operatorParser, filterParser, new NotParser());
  }

  public static QueryParser queryParserWithOutValidationFixture() {
    OperatorParser operatorParser = new OperatorParser();
    FieldParser fieldParser = fieldParserWithoutValidationFixture();
    FilterParser filterParser = new FilterParser(fieldParser, operatorParser, new ValueParser());
    return new QueryParser(operatorParser, filterParser, new NotParser());
  }

  public static SortParser sortParserFixture() {
    return new SortParser(
        fieldParserWithoutValidationFixture(),
        new OperatorParser(),
        new ValueParser(),
        queryParserFixture());
  }

  public static FacetParser facetParserFixture() {
    return new FacetParser(fieldParserFixture(), sortParserFixture());
  }

  public static FilterParser filterParserFixture() {
    return new FilterParser(fieldParserFixture(), new OperatorParser(), new ValueParser());
  }

  public static FieldCache fieldCacheFixture() {
    IndexSettings indexSettings = mock(IndexSettings.class);
    when(indexSettings.getIndex()).thenReturn(INDEX_NAME);

    Map<String, Field> mockPreprocessedFields = mock(Map.class);
    when(mockPreprocessedFields.get(anyString()))
        .thenAnswer(
            (Answer<Field>)
                invocationOnMock -> {
                  String fieldName = invocationOnMock.getArguments()[0].toString();
                  if (fieldName.contains("invalid")) return null;

                  String[] split = fieldName.split("\\.");
                  List<String> names = asList(split).subList(1, split.length); // Ignore index name
                  return new Field(mockLinkedMapForField(names));
                });
    when(mockPreprocessedFields.containsKey(anyString()))
        .thenAnswer(
            invocationOnMock -> !invocationOnMock.getArguments()[0].toString().contains("invalid"));

    FieldCache fieldCache = new FieldCache(new FieldFactory());
    setInternalState(fieldCache, "validFields", mockPreprocessedFields);
    setInternalState(fieldCache, "indexSettings", indexSettings);
    return fieldCache;
  }

  private static LinkedMap mockLinkedMapForField(List<String> names) {
    return rangeClosed(1, names.size())
        .boxed()
        .map(i -> names.stream().limit(i).collect(joining(".")))
        .collect(
            LinkedMap::new,
            (map, field) -> map.put(field, getTypeForField(field)),
            LinkedMap::putAll);
  }

  private static String getTypeForField(String fieldName) {
    if (fieldName.contains("nested")) return "nested";
    if (fieldName.contains("keyword")) return "keyword";
    if (fieldName.contains("geo_point")) return "geo_point";
    return "_obj";
  }

  public static FieldParser fieldParserFixture() {
    return new FieldParser(new NotParser(), fieldCacheFixture());
  }

  private static FieldParser fieldParserWithoutValidationFixture() {
    return new FieldParser(new NotParser());
  }
}
