# Embeddings and Vector Stores

## Introduction

Welcome to this hands-on lab on working with embeddings and vector stores. In this lab, you'll learn how to ingest data and make it available for machine learning models to extract information from it efficiently, including searching by the meaning of words rather than by keywords.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 22 (install using [SDKMAN!](https://sdkman.io/jdks))
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [httpie](https://httpie.io/cli) for making API calls
* [Ollama](https://ollama.com/) for running large language models locally

Ensure you have the `nomic-embed-text` embedding model available in Ollama:

```shell
ollama pull nomic-embed-text
```

## Learning Goals

By the end of this lab, you will be able to:

* Use Spring AI to integrate with embedding models via Ollama
* Create an ingestion pipeline to process and store document data
* Work with different document formats (text and PDF) using Spring AI's document readers
* Transform document data using Spring AI's document transformers
* Store document embeddings in a vector database using Spring AI's VectorStore abstraction
* Perform semantic searches on the stored documents using Spring AI's query system

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final state of the project.

You can start the application as follows. It will reload automatically whenever you make changes. On Visual Studio Code, it works out of the box. If you're using IntelliJ IDEA, follow the [documentation](https://www.jetbrains.com/help/idea/spring-boot.html#enable-auto-restart) to configure IDEA to support the Spring Boot DevTools.

```shell
./gradlew bootTestRun
```

This time, wait until the end of step 1.1 to start the application.

### 1. Embedding Models

Embedding models are machine learning models that convert text into a numerical representation, known as an embedding. These embeddings capture the semantic meaning of the text, allowing for efficient comparison and analysis.

Spring AI provides an `EmbeddingModel` abstraction for integrating with LLMs via several providers, including Ollama.

When using the _Spring AI Ollama Spring Boot Starter_, an `EmbeddingModel` object is autoconfigured for you to use Ollama.

```java
@RestController
class EmbeddingController {
    private final EmbeddingModel embeddingModel;

    EmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/embed")
    String embed(@RequestParam(defaultValue = "And Gandalf yelled: 'You shall not pass!'") String message) {
        var embeddings = embeddingModel.embed(message);
        return "Size of the embedding vector: " + embeddings.size();
    }
}
```

Before moving on, consider reading the [Embeddings](https://docs.spring.io/spring-ai/reference/concepts.html#_embeddings) section from the Spring AI documentation.

#### 1.1 Bootstrapping the Ollama integration

* The project is already equipped with the Spring AI Ollama integration. Open the `application.yml` file and configure the embedding model to use (`nomic-embed-text`).

```yaml
spring:
  ai:
    ollama:
      embedding:
        options:
          model: nomic-embed-text
```

* Create a new `EmbeddingController` class and autowire an `EmbeddingModel` object in the constructor.

```java
@RestController
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    public EmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

}
```

#### 1.2 Make your first call to an embedding model

* In the `EmbeddingController` class, define an `embed()` method accepting some text as a request parameter and returning a `String`. Configure it to handle GET requests to the `/embed` endpoint.

```java
@RestController
public class EmbeddingController {

    @GetMapping("/embed")
    String embed(@RequestParam(defaultValue = "And Gandalf yelled: 'You shall not pass!'") String query) {
    }

}
```

* Pass the input query to the `EmbeddingModel` and use it to get the embeddings.

```java
@GetMapping("/embed")
String embed(@RequestParam(defaultValue = "And Gandalf yelled: 'You shall not pass!'") String query) {
    var embeddings = embeddingModel.embed(query);
    return "Size of the embedding vector: " + embeddings.length;
}
```

* Test the newly created endpoint

```shell
http :8080/embed query=="Spring AI Rocks" -b
```

* Before moving on, consider reading the [Embedding Model API](https://docs.spring.io/spring-ai/reference/api/embeddings.html) section of the Spring AI documentation.

### 2. Ingestion Pipeline

To allow a machine learning model to act on your data, you first need to prepare and load it. This process is often referred to as the data ingestion pipeline, similar to the ETL (Extract, Transform, Load) pipeline in data engineering.

Spring AI provides primitives to read, transform, and write your data, wrapped in a `Document` abstraction. This exercise will guide you through the process of creating a simple ingestion pipeline, which ultimately writes the data to a vector store.

A vector store is a database that stores vectors, which are numerical representations of data. It allows for efficient similarity search, which is a common operation in machine learning applications.

Spring AI provides a `VectorStore` abstraction for integrating with vector stores via several providers, including PostgreSQL (pgvector).

Before moving on, consider reading the [ETL Pipeline](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html) and [API Overview](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_api_overview) sections of the Spring AI documentation.

#### 2.1 Set up an ingestion pipeline

* Create a new `IngestionPipeline` class and autowire a `VectorStore` object in the constructor.
* Define a `Logger` as well, so you can log messages during the ingestion process and debug any issues that may arise.

```java
@Component
public class IngestionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(IngestionPipeline.class);

    private final VectorStore vectorStore;

    public IngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
}
```

* The project already includes a text file and a PDF file in `src/main/resources/documents`. Inject them in the `IngestionPipeline` class as `Resource` objects. You'll soon use them as input for the ingestion pipeline. If you're curious, you can check out the contents of the files and learn about Iorek and Pingu.

```java
@Component
public class IngestionPipeline {

    @Value("classpath:documents/story1.md")
    Resource file1;

    @Value("classpath:documents/story2.pdf")
    Resource file2;
}
```

* Define a `run()` method that will be called to start the ingestion process. Annotate it with `@PostConstruct` so that it is called automatically when the application starts.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    public void run() {
    }
}
```

* Everything is ready to start populating the ingestion pipeline.

* Before moving on, consider reading the [ETL Interfaces](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_etl_interfaces) section of the Spring AI documentation.

#### 2.2 Read the data (txt)

* Spring AI supports reading many different types of files via the `DocumentReader`. The core framework supports reading text (`TextReader`) and JSON (`JsonReader`) files. Additional formats are supported by dedicated Spring AI modules, including PDF, Markdown, DOCX, PowerPoint, HTML, and more.
* In the `run()` method, initialize a `List<Document>` to hold all the documents read from the pipeline.

* Create a `TextReader` object and use it to read the first file. Explore the `TextReader` API to learn about the available options, including adding custom metadata to the document, setting the character encoding, and more. Metadata are useful for filtering documentation during the retrieval/search phase.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    public void run() {
        List<Document> documents = new ArrayList<>();

        logger.info("Loading .md files as Documents");
        var textReader = new TextReader(file1);
        textReader.getCustomMetadata().put("location", "North Pole");
        textReader.setCharset(Charset.defaultCharset());
        documents.addAll(textReader.get());
    }
}
```

* Before moving on, consider reading the [Text Document Reader](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_text) section of the Spring AI documentation.

#### 2.3 Read the data (pdf)

* Spring AI supports reading PDF documents via the Spring AI PDF Reader module and the Spring AI Tikka Reader module (based on Apache Tika). This project is already configured to use the former.
* The Spring AI PDF Reader module provides two ways of reading data from a PDF file: `PagePdfDocumentReader` and `ParagraphPdfDocumentReader`, which read the data page by page and paragraph by paragraph, respectively.
* Create a `PagePdfDocumentReader` object and use it to read the second file. Explore the `PagePdfDocumentReader` API to learn about the available options, including skipping pages, removing initial or ending lines, and more. You can also choose how many pages should end up being parsed in one `Document` object.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    public void run() {
        ...

        logger.info("Loading .pdf files as Documents");
        var pdfReader = new PagePdfDocumentReader(file2, PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfTopPagesToSkipBeforeDelete(0)
                        .withNumberOfBottomTextLinesToDelete(1)
                        .withNumberOfTopTextLinesToDelete(1)
                        .build())
                .withPagesPerDocument(1)
                .build());
        documents.addAll(pdfReader.get());
    }
}
```

* Before moving on, consider reading the [PDF Page](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_pdf_page) section of the Spring AI documentation.

#### 2.4 Transform the data

* You could write the current `List<Document>` to a database or a file system, but you might want to transform the data first.
* Spring AI provides `DocumentTransformer` implementations for common use cases, such as splitting documents into chunks, summarizing documents, and extracting keywords.
* For this example, split the documents into small chunks with a `TokenTextSplitter`. We'll use small numbers for the chunk size to keep the example simple and make it easier to retrieve and understand the chunks later. In a real-world application, you might want to use larger numbers.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    public void run() {
        ...

        logger.info("Split Documents to better fit the LLM context window");
        var textSplitter = new TokenTextSplitter(50, 100, 5, 10000, true);
        var transformedDocuments = textSplitter.apply(documents);
    }
}
```

* Before moving on, consider reading the [Document Transformers](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_transformers) section of the Spring AI documentation.

#### 2.5 Write the data

* Now that you have a `List<Document>` of transformed documents, you can generate the vector representation (embeddings) for each of them and write them to a database or a file system.
* Spring AI provides `DocumentWriter` implementations for common use cases, such as writing documents to a database or a file system. In this example, we'll use a `VectorStore`.
* A `VectorStore` is a database that stores documents and their vector representations. It allows you to search for documents that are similar to a given query. This project is already configured with PostgreSQL, which we can also use as a vector store thanks to the Spring AI PGVector module. Open the `application.yml` file and inspect the PGVector configuration for initializing the database schema automatically. Furthermore, it's set up for adopting the HNSW index type we'll use later for similarity search, and 768 dimensionality (that's the dimensions of the vectors generated by `nomic-embed-text`, the embedding model we've been using in this example).

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 768
        index-type: hnsw
```

* You might have noticed that you didn't have to set up any PostgreSQL database so far. That's because we already have one up and running via Testcontainers, managed by Spring Boot as a dev service. Inspect the `TestcontainersConfiguration` class to see how it's done. When you run the application via "./gradlew bootTestRun", Testcontainers will start a PostgreSQL container and Spring Boot will connect to it automatically.
* Complete the ingestion pipeline by adding one final step to write the transformed documents to the vector store. Behind the scenes, the `add()` method will generate the vector representation for each document and write it to the database.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    public void run() {
        ...

        logger.info("Creating and storing Embeddings from Documents");
        vectorStore.add(transformedDocuments);
    }
}
```

* When you start up the application, the ingestion pipeline will be executed. You might want to experiment with the pipeline configuration, change how the data is read and how it is transformed. For that reason, the PostgreSQL container is not configured to be re-usable, so it will be destroyed and created again every time you start the application to ensure a clean state (the `@RestartScope` annotation in `TestcontainersConfiguration` is commented out). In general, you might want to configure the container to be re-usable in a real-world application.
* In the next section, you'll learn how to use the vector store to search for documents that are similar to a given query.

* Before moving on, consider reading the [Document Writers](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_writers) section of the Spring AI documentation.

### 3. Semantic Search

Now that you have a vector store with documents and their vector representations, you can use it to search for documents that are similar to a given query.

Spring AI provides a powerful query system to perform semantic search on a vector store, including a portable SQL-like metadata filter API.

#### 3.1 Set up semantic search

* Create a new `SearchController` class and autowire a `VectorStore` object in the constructor.

```java
@RestController
class SearchController {

    private final VectorStore vectorStore;

    SearchController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

}
```

* Before moving on, consider reading the [Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html) and [API Overview](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#api-overview) sections of the Spring AI documentation.

#### 3.2 Perform semantic search

* Add a new `search()` method to the `SearchController` class. This method will receive a `query` parameter and return a `List<String>` with the content of the documents that are similar to the query.

```java
@RestController
class SearchController {

    private final VectorStore vectorStore;

    @PostMapping("/search")
    List<String> search(@RequestBody String query) {
        return vectorStore.similaritySearch(SearchRequest.query(query))
                .stream()
                .map(Document::getContent)
                .toList();
    }

}
```

* Test the newly created endpoint

```shell
http --raw "Iorek's biggest dream" :8080/search
```

```shell
http --raw "glaciers and mountains" :8080/search
```

* Inspect the `SearchRequest` API and try out different options. For example, you can limit the number of results, define a threshold for how similar two documents can be, and filter the results based on metadata. Then try performing a search again and see how the results change.

```java
@RestController
class SearchController {

    private final VectorStore vectorStore;

    @PostMapping("/search")
    List<String> search(@RequestBody String query) {
        return vectorStore.similaritySearch(SearchRequest.query(query)
                        .withFilterExpression("location == 'North Pole'")
                        .withSimilarityThreshold(0.25)
                        .withTopK(5))
                .stream()
                .map(Document::getContent)
                .toList();
    }

}
```

* Before moving on, consider reading the [Vector Database Example Usage](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_example_usage) and [Metadata Filters](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#metadata-filters) sections of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on how to design an ingestion pipeline and perform semantic search on a vector store using Spring AI.
