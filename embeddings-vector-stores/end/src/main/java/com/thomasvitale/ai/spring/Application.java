package com.thomasvitale.ai.spring;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner embed(EmbeddingModel embeddingModel) {
        return _ -> {
            var embeddings = embeddingModel.embed("And Gandalf yelled: 'You shall not pass!'");
            logger.info("Size of the embeddings vector: " + embeddings.length);
        };
    }

    @Bean
    CommandLineRunner search(VectorStore vectorStore) {
        return _ -> {
            logger.info(">>> Semantic Search - Basic...");
            var query = "Iorek's biggest dream";
            var documents = vectorStore.similaritySearch(query);
            documents.stream()
                    .map(Document::getContent)
                    .forEach(logger::info);

            logger.info(">>> Semantic Search - Tuning...");
            query = "Iorek's biggest dream";
            documents = vectorStore.similaritySearch(SearchRequest.query(query)
                    .withSimilarityThreshold(0.5)
                    .withTopK(3));
            documents.stream()
                    .map(Document::getContent)
                    .forEach(logger::info);

            logger.info(">>> Semantic Search - Metadata Filters...");
            query = "Iorek's biggest dream";
            documents = vectorStore.similaritySearch(SearchRequest.query(query)
                    .withFilterExpression("location == 'North Pole'")
                    .withSimilarityThreshold(0.5)
                    .withTopK(3));
            documents.stream()
                    .map(Document::getContent)
                    .forEach(logger::info);

            logger.info("Search completed");
        };
    }

}

@Component
class IngestionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(IngestionPipeline.class);

    private final VectorStore vectorStore;

    @Value("classpath:documents/story1.txt")
    Resource file1;

    @Value("classpath:documents/story2.md")
    Resource file2;

    IngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    void run() {
        List<Document> documents = new ArrayList<>();

        logger.info("Loading .txt files as Documents");
        var textReader = new TextReader(file1);
        textReader.getCustomMetadata().put("location", "North Pole");
        textReader.setCharset(Charset.defaultCharset());
        documents.addAll(textReader.get());

        logger.info("Loading .md files as Documents");
        var markdownReader = new MarkdownDocumentReader(file2, MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("location", "Italy")
                .build());
        documents.addAll(markdownReader.get());

        logger.info("Split Documents to better fit the LLM context window");
        var textSplitter = TokenTextSplitter.builder()
                .withChunkSize(50)
                .build();
        var transformedDocuments = textSplitter.apply(documents);

        logger.info("Creating and storing Embeddings from Documents");
        vectorStore.add(transformedDocuments);
    }

}
