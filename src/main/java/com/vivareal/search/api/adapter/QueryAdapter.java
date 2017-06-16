package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;

import java.util.Optional;

public interface QueryAdapter<Q, F, S> {

    Optional<SearchApiResponse> getById(SearchApiRequest request, String id);

    SearchApiResponse query(SearchApiRequest request);
}
