package com.vivareal.search.api.adapter;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.SOURCE_EXCLUDES;
import static com.vivareal.search.api.configuration.environment.RemoteProperties.SOURCE_INCLUDES;
import static org.apache.commons.lang3.ArrayUtils.contains;

import com.vivareal.search.api.exception.InvalidFieldException;
import com.vivareal.search.api.model.event.RemotePropertiesUpdatedEvent;
import com.vivareal.search.api.model.search.Fetchable;
import com.vivareal.search.api.service.parser.factory.FieldFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SourceFieldAdapter implements ApplicationListener<RemotePropertiesUpdatedEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(SourceFieldAdapter.class);

  private static final String FETCH_ALL_FIELD = "*";

  private final FieldFactory fieldFactory;

  private final Map<String, String[]> defaultSourceIncludes;
  private final Map<String, String[]> defaultSourceExcludes;

  @Autowired
  public SourceFieldAdapter(FieldFactory fieldFactory) {
    this.fieldFactory = fieldFactory;

    defaultSourceIncludes = new ConcurrentHashMap<>();
    defaultSourceExcludes = new ConcurrentHashMap<>();
  }

  public void apply(SearchRequestBuilder searchRequestBuilder, final Fetchable request) {
    String[] includeFields = getFetchSourceIncludeFields(request);
    searchRequestBuilder.setFetchSource(
        includeFields, getFetchSourceExcludeFields(request, includeFields));
  }

  public void apply(GetRequestBuilder getRequestBuilder, final Fetchable request) {
    String[] includeFields = getFetchSourceIncludeFields(request);
    getRequestBuilder.setFetchSource(
        includeFields, getFetchSourceExcludeFields(request, includeFields));
  }

  private String[] getFetchSourceIncludeFields(final Fetchable request) {
    return request.getIncludeFields() == null
        ? defaultSourceIncludes.get(request.getIndex())
        : getFetchSourceIncludeFields(request.getIncludeFields(), request.getIndex());
  }

  private String[] getFetchSourceIncludeFields(Set<String> fields, String indexName) {
    return SOURCE_INCLUDES
        .getValue(fields, indexName)
        .stream()
        .filter(field -> isValidFetchSourceField(indexName, field))
        .toArray(String[]::new);
  }

  private String[] getDefaultFetchSourceExcludeFieldsForIndex(
      String index, String[] defaultIncludes) {
    return getFetchSourceExcludeFields(null, defaultIncludes, index);
  }

  private String[] getFetchSourceExcludeFields(final Fetchable request, String[] includeFields) {
    return request.getExcludeFields() == null && includeFields.length == 0
        ? defaultSourceExcludes.get(request.getIndex())
        : getFetchSourceExcludeFields(
            request.getExcludeFields(), includeFields, request.getIndex());
  }

  private String[] getFetchSourceExcludeFields(
      Set<String> fields, String[] includeFields, String indexName) {
    return SOURCE_EXCLUDES
        .getValue(fields, indexName)
        .stream()
        .filter(
            field -> !contains(includeFields, field) && isValidFetchSourceField(indexName, field))
        .toArray(String[]::new);
  }

  private String[] getDefaultFetchSourceIncludeFieldsForIndex(String index) {
    return getFetchSourceIncludeFields(null, index);
  }

  private boolean isValidFetchSourceField(String index, String fieldName) {
    if (FETCH_ALL_FIELD.equals(fieldName) || fieldFactory.isIndexHasField(index, fieldName)) {
      return true;
    }
    throw new InvalidFieldException(fieldName, index);
  }

  @Override
  public void onApplicationEvent(RemotePropertiesUpdatedEvent event) {
    String[] defaultIncludes = getDefaultFetchSourceIncludeFieldsForIndex(event.getIndex());
    defaultSourceIncludes.put(event.getIndex(), defaultIncludes);
    defaultSourceExcludes.put(
        event.getIndex(),
        getDefaultFetchSourceExcludeFieldsForIndex(event.getIndex(), defaultIncludes));

    LOG.debug("Refreshed default source fields for index: " + event.getIndex());
  }
}
