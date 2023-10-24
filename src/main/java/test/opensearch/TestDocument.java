package test.opensearch;

import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

@Document(indexName = "test_document")
@Setting(settingPath = "index_settings.json")
public record TestDocument(
        @MultiField(mainField = @Field(type = FieldType.Text), otherFields = {@InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "tokenizer_analyzer", searchAnalyzer = "standard")}) String tokenizedText,
        @MultiField(mainField = @Field(type = FieldType.Text), otherFields = {@InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "filter_analyzer", searchAnalyzer = "standard")}) String filteredText,
        @Field(type = FieldType.Object) List<SubObject> subObjects) {
}
