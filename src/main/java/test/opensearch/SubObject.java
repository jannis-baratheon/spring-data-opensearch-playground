package test.opensearch;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

public record SubObject(
        @MultiField(mainField = @Field(type = FieldType.Text), otherFields = {@InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "tokenizer_analyzer")}) String tokenizedText,
        @MultiField(mainField = @Field(type = FieldType.Text), otherFields = {@InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "filter_analyzer")}) String filteredText) {
}
