package test.opensearch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
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

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opensearch.index.query.QueryBuilders.matchAllQuery;

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
        elasticsearchOperations.delete(
                new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build(),
                TestDocument.class);
    }

    @Test
    void test() {
        var testDocument = elasticsearchOperations.save(new TestDocument("some text", "some text"));
        indexOperations.refresh();

        assertThat(fetchAllDocuments()).containsExactly(testDocument);
    }

    private List<TestDocument> fetchAllDocuments() {
        return elasticsearchOperations.search(elasticsearchOperations.matchAllQuery(), TestDocument.class)
                .getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
