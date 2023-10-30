package test.opensearch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.AnalyzeRequest;
import org.opensearch.client.indices.AnalyzeResponse;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import static org.opensearch.index.query.QueryBuilders.multiMatchQuery;

@Slf4j
@SpringBootTest
@Testcontainers
class OpenSearchTest {
    @Container
    static final OpensearchContainer openSearchContainer =
            new OpensearchContainer("opensearchproject/opensearch:2.7.0")
                    .withStartupAttempts(5)
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withLogConsumer(
                            new Slf4jLogConsumer(log)
                                    .withSeparateOutputStreams()
                                    .withPrefix("OPENSEARCH_TEST_DOCKER_CONTAINER"))
                    .withEnv("action.auto_create_index", "false");
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    private IndexOperations indexOperations;

    @DynamicPropertySource
    static void openSearchProperties(DynamicPropertyRegistry registry) {
        registry.add("opensearch.uris", openSearchContainer::getHttpHostAddress);
    }

    @BeforeEach
    final void setup() {
        indexOperations = elasticsearchOperations.indexOps(TestDocument.class);

        indexOperations.createWithMapping();
    }

    @AfterEach
    final void clearIndices() {
        indexOperations.delete();
    }

    @Test
    void indexesDocuments() {
        // given
        var expectedDocument = new TestDocument(
                "some text",
                "some other text",
                List.of(new SubObject("another text", "third text")));

        // when
        indexDocuments(expectedDocument);

        // then
        assertThat(fetchAllDocuments()).singleElement().isEqualTo(expectedDocument);
    }

    @Test
    void synonymsAnalyzerWorks() throws IOException {
        // given
        var analyzeRequest = AnalyzeRequest.withIndexAnalyzer(
                "test_document",
                "synonyms_analyzer",
                "first Diners Club last");

        // when
        var analysisResult = restHighLevelClient.indices().analyze(analyzeRequest, RequestOptions.DEFAULT);

        // then
        assertThat(analysisResult.getTokens())
                .extracting(AnalyzeResponse.AnalyzeToken::getTerm)
                .contains("first", "diners_club", "DINERS_CLUB", "club", "Diners", "Club", "last");
    }

    @Test
    void searchesDocumentsWithSynonyms2() {
        // given
        var expectedMatch = new TestDocument(
                "DINERS_CLUB",
                "some other text",
                List.of());
        var expectedNonMatch = new TestDocument(
                "any text",
                "any other text",
                List.of());
        indexDocuments(expectedMatch, expectedNonMatch);

        // when
        var actualSearchResult = elasticsearchOperations.search(
                new NativeSearchQueryBuilder()
                        .withQuery(multiMatchQuery("Diners Club some", "filteredText", "tokenizedText")
                                .operator(Operator.AND)
                                .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                                .analyzer("synonyms_analyzer"))
                        .build(),
                TestDocument.class);

        // then
        assertThat(actualSearchResult.getSearchHits())
                .extracting(SearchHit::getContent)
                .singleElement()
                .isEqualTo(expectedMatch);
    }

    @Test
    void searchesDocuments() {
        // given
        var expectedMatch = new TestDocument(
                "needle",
                "some other text",
                List.of(new SubObject("another text", "third text")));
        var expectedNonMatch = new TestDocument(
                "some text",
                "some other text",
                List.of(new SubObject("another text", "third text")));
        indexDocuments(expectedMatch, expectedNonMatch);

        // when
        var actualSearchResult = elasticsearchOperations.search(
                new NativeSearchQueryBuilder()
                        .withQuery(matchQuery("tokenizedText", "needle"))
                        .build(),
                TestDocument.class);

        // then
        assertThat(actualSearchResult.getSearchHits())
                .extracting(SearchHit::getContent)
                .singleElement()
                .isEqualTo(expectedMatch);
    }

    private void indexDocuments(TestDocument... testDocuments) {
        elasticsearchOperations.save(testDocuments);
        indexOperations.refresh();
    }

    private List<TestDocument> fetchAllDocuments() {
        return elasticsearchOperations.search(elasticsearchOperations.matchAllQuery(), TestDocument.class)
                .getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
