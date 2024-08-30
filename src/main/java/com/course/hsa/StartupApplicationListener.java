package com.course.hsa;

import com.course.hsa.service.ElasticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Value("classpath:data/words_alpha.txt")
    private final Resource wordsFile;
    private final ElasticSearchService elasticSearchService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Start creating english vocabulary index..");

        var words = readWords();
        elasticSearchService.ingestWordsToIndex(words);

        log.info("Index eng-dictionary is created");
    }

    @SneakyThrows
    private List<String> readWords() {
        var words = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(wordsFile.getInputStream(), UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line);
            }
        }
        return words;
    }
}
