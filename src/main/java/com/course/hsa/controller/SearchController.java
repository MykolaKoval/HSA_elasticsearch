package com.course.hsa.controller;

import com.course.hsa.service.ElasticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticSearchService elasticSearchService;

    @GetMapping(path = "/search")
    public List<String> getAutocomplete(@RequestParam("text") String text) {
        return elasticSearchService.getAutocomplete(text);
    }

}
