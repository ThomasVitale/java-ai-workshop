package com.thomasvitale.ai.spring;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SearchController {

    private final VectorStore vectorStore;

    SearchController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping("/search")
    List<String> search(@RequestBody String query) {
        return vectorStore.similaritySearch(SearchRequest.query(query)
                        //.withFilterExpression("location == 'North Pole'")
                        //.withSimilarityThreshold(0.25)
                        .withTopK(5))
                .stream()
                .map(Document::getContent)
                .toList();
    }

}
