package com.course.hsa.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.course.hsa.domain.Word;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticSearchService {

    private static final String INDEX_NAME = "eng-dictionary";
    private static final String CONTENT_FIELD = "content";

    @Value("${es.host}")
    private final String esHost;
    @Value("classpath:elasticsearch/createIndex.json")
    private final Resource createIndex;

    @SneakyThrows
    public List<String> getAutocomplete(String searchText) {
        // Create the low-level client
        RestClient restClient = RestClient
                .builder(HttpHost.create(esHost))
                .build();

        // Create the transport with a Jackson mapper
        try (ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {

            // Create the API client
            ElasticsearchClient esClient = new ElasticsearchClient(transport);

            var searchResponse = esClient.search(s -> s.index(INDEX_NAME)
                    .query(q -> q
                            .match(t -> t
                                    .field(CONTENT_FIELD)
                                    .query(searchText)
                                    .fuzziness("AUTO"))), Word.class);

            return searchResponse.hits().hits().stream().map(Hit::source).map(Word::getContent).toList();
        }
    }

    @SneakyThrows
    public void ingestWordsToIndex(List<String> words) {
        // Create the low-level client
        RestClient restClient = RestClient
                .builder(HttpHost.create(esHost))
                .build();

        // Create the transport with a Jackson mapper
        try (ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())) {

            // Create the API client
            ElasticsearchClient esClient = new ElasticsearchClient(transport);

            createIndex(esClient);

            var docCounter = new AtomicLong(0);
            var batches = ListUtils.partition(words, 10000);
            batches.forEach(batch -> bulkIngest(esClient, batch, docCounter));
        }
    }

    @SneakyThrows
    private void createIndex(ElasticsearchClient esClient) {
        var createIndexRequest = new CreateIndexRequest.Builder().index(INDEX_NAME)
                .withJson(createIndex.getInputStream())
                .build();

        var response = esClient.indices().create(createIndexRequest);
        log.info("Index created: {}", response);
    }

    @SneakyThrows
    private void bulkIngest(ElasticsearchClient esClient, List<String> words, AtomicLong docCounter) {
        var bulkRequest = createBulkRequest(words, docCounter);
        var result = esClient.bulk(bulkRequest);
        if (result.errors()) {
            log.error("Bulk had errors");
            result.items().stream().filter(hasError()).forEach(item -> {
                log.error("Item error: {}", item.error().reason());
            });
        } else {
            log.info("Bulk ingested, size={}", words.size());
        }
    }

    private BulkRequest createBulkRequest(List<String> words, AtomicLong docCounter) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        words.forEach(word -> br.operations(op -> op
                .index(idx -> idx
                        .index(INDEX_NAME)
                        .id(String.valueOf(docCounter.incrementAndGet()))
                        .document(asDto(word)))));
        return br.build();
    }

    private static Word asDto(String word) {
        return Word.builder().content(word).size(word.length()).build();
    }

    private static Predicate<BulkResponseItem> hasError() {
        return item -> item.error() != null;
    }
}
