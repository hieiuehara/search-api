package com.vivareal.search.api.itest.configuration.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Service
public class ESIndexHandler {

    private static Logger LOG = LoggerFactory.getLogger(ESIndexHandler.class);

    public static final String TEST_DATA_INDEX = "/testdata";

    private final RestClient restClient;
    private int standardDatasetSize;
    private final Long timeout;

    @Autowired
    public ESIndexHandler(RestClient restClient,
                          @Value("${itest.standard.dataset.size}") Integer standardDatasetSize,
                          @Value("${es.controller.search.timeout}") Long timeout) {
        this.restClient = restClient;
        this.standardDatasetSize = standardDatasetSize;
        this.timeout = timeout;
    }

    public void truncateIndexData() throws IOException {
        Response response = restClient.performRequest("POST", TEST_DATA_INDEX + "/_delete_by_query?refresh=true", emptyMap(),
        new NStringEntity(
        "{\n" +
        "  \"query\": {\n" +
        "    \"match_all\": {}\n" +
        "  }\n" +
        "}", APPLICATION_JSON));

        LOG.info(TEST_DATA_INDEX + " index cleared, deleted " + response + " documents");
    }

    public void addStandardTestData() throws IOException {
        List<String> entities = new ArrayList<>(standardDatasetSize);
        for(int id = 1; id<= standardDatasetSize; id++) {
            String entity = createStandardEntityForId(id);
            if(insertEntityToTestDataIndex(id, entity))
                entities.add(entity);
        }

        LOG.info(TEST_DATA_INDEX + " inserted " + entities.size() + " documents");
        refreshIndex();
    }

    private String createStandardEntityForId(int id) {
        boolean isEven = id % 2 == 0;

        Map<String, Object> kv = unmodifiableMap(Stream.of(
            new SimpleEntry<>("field", "common"),
            new SimpleEntry<>("array_string", rangeClosed(1, id).boxed().map(String::valueOf).collect(toList()))
        ).collect(toMap(SimpleEntry::getKey, e -> (Object) e.getValue())));

        Map<String, Double> geo = unmodifiableMap(Stream.of(
            new SimpleEntry<>("lat", id * -1d),
            new SimpleEntry<>("lon", id * 1d)
        ).collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

        Map<String, Object> nestedObject = unmodifiableMap(Stream.of(
            new SimpleEntry<>("object", kv),
            new SimpleEntry<>("number", id * 2),
            new SimpleEntry<>("float", id * 3.5f),
            new SimpleEntry<>("string", format("string with char %s", (char) (id + 'a' - 1))),
            new SimpleEntry<>("boolean", !isEven),
            new SimpleEntry<>(isEven ? "even" : "odd", true)
        ).collect(toMap(SimpleEntry::getKey, e -> (Object) e.getValue())));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(id));
        data.put("numeric", id);
        data.put("field" + id, "value" + id);
        data.put("isEven", isEven);
        data.put("array_integer", rangeClosed(1, id).boxed().collect(toList()));
        data.put("nested", nestedObject);
        data.put("object", nestedObject);
        data.put("geo", geo);

        try {
            return new ObjectMapper().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.error("Unable to create standard entity for id", e);
            return "{}";
        }
    }

    private boolean insertEntityToTestDataIndex(int id, String body) {
        try {
            Response response = restClient.performRequest("POST", TEST_DATA_INDEX + "/testdata/" + id + "?refresh=true", emptyMap(), new NStringEntity(body, APPLICATION_JSON));
            LOG.info(TEST_DATA_INDEX + " adding document" + response + " documents");
            return true;
        } catch (IOException e) {
            LOG.error("Unable to add entity for index: " + TEST_DATA_INDEX, e);
        }
        return false;
    }

    private void refreshIndex() {
        try {
            restClient.performRequest("POST", TEST_DATA_INDEX + "/_refresh", emptyMap());
            MICROSECONDS.sleep(timeout);
            LOG.info("Forced commit into index: " + TEST_DATA_INDEX);
        } catch (IOException | InterruptedException e) {
            LOG.error("Unable to force commit into index: " + TEST_DATA_INDEX, e);
        }
    }
}
