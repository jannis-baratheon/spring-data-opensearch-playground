package test.opensearch;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "test_document")
@Setting(settingPath = "index_settings.json")
public record TestDocument(@Field(type = FieldType.Text, analyzer = "tokenizer_analyzer") String tokenizedText,
                           @Field(type = FieldType.Text, analyzer = "filter_analyzer") String filteredText) {
}
