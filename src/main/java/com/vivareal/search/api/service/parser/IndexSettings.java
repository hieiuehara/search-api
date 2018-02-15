package com.vivareal.search.api.service.parser;

import static com.vivareal.search.api.adapter.ElasticsearchSettingsAdapter.SHARDS;
import static java.lang.Integer.parseInt;

import com.vivareal.search.api.adapter.SettingsAdapter;
import com.vivareal.search.api.model.query.Field;
import com.vivareal.search.api.model.search.Indexable;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component
public class IndexSettings {

  private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter;
  private String index;

  @Autowired
  public IndexSettings(SettingsAdapter settingsAdapter) {
    this.settingsAdapter = settingsAdapter;
  }

  public void validateIndex(Indexable indexable) {
    settingsAdapter.checkIndex(indexable);
    this.index = indexable.getIndex();
  }

  public void validateField(Field field) {
    // TODO - Remove this issue when fix the parser :'(
    if (!"NOT".equalsIgnoreCase(field.getName())) {
      settingsAdapter.checkFieldName(index, field.getName(), false);
    }
  }

  public String getFieldType(String field) {
    return settingsAdapter.getFieldType(index, field);
  }

  public int getShards() {
    return parseInt(settingsAdapter.settingsByKey(index, SHARDS));
  }
}