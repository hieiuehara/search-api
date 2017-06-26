package com.vivareal.search.api.model;

import com.vivareal.search.api.model.parser.QueryParser;
import com.vivareal.search.api.model.query.QueryFragment;
import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;

import java.util.Collections;
import java.util.List;

import static com.vivareal.search.api.model.parser.SortParser.get;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public final class SearchApiRequest {

    private static final Parser<QueryFragment> QUERY_PARSER = QueryParser.get();

    private String index;
    private String operator;
    private String mm;
    private List<String> fields = Collections.emptyList();
    private List<String> includeFields = Collections.emptyList();
    private List<String> excludeFields = Collections.emptyList();
    private QueryFragment filter;
    private Sort sort;

    private String q;
    private Integer from, size;

    public QueryFragment getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = QUERY_PARSER.parse(filter);
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getMm() {
        return mm;
    }

    public void setMm(String mm) {
        this.mm = mm;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        if (q != null) this.q = q.trim();
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(String... sort) {
        this.sort = get().parse(stream(sort).collect(joining(",")));
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(List<String> includeFields) {
        this.includeFields = includeFields;
    }

    public List<String> getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(List<String> excludeFields) {
        this.excludeFields = excludeFields;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setPaginationValues(int defaultSize, int maxSize) {
        this.from = from != null && from >= 0 ? from : 0;
        this.size = size != null && size >= 0 && size <= maxSize ? size : defaultSize;
    }
}
