# Embeddings and Vector Stores

## Introduction

Welcome to this hands-on lab on working with embeddings and vector stores. In this lab, you'll learn
how to ingest data and make it available for machine learning models to extract information from it efficiently,
including searching by the meaning of words rather than by keywords.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 23 (install using [SDKMAN!](https://sdkman.io/jdks))
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [Ollama](https://ollama.com/) for running large language models locally.

## Learning Goals

By the end of this lab, you will be able to:

* Use Spring AI to integrate with embedding models via Ollama
* Create an ingestion pipeline to process and store document data
* Work with different document formats (text and PDF) using Spring AI's document readers
* Transform document data using Spring AI's document transformers
* Store document embeddings in a vector database using Spring AI's VectorStore abstraction
* Perform semantic searches on the stored documents using Spring AI's query system

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final
state of the project.

The project is configured as a standalone CLI application. You can run the application as follows.

```shell
./gradlew bootTestRun
```

### 1. Embedding Models

Embedding models are machine learning models that convert text into a numerical representation, known as an embedding.
These embeddings capture the semantic meaning of the text, allowing for efficient comparison and analysis.

Spring AI provides an `EmbeddingModel` abstraction for integrating with LLMs via several providers, including Ollama.

When using the _Spring AI Ollama Spring Boot Starter_, an `EmbeddingModel` object is autoconfigured for you to use Ollama.

```java
@Bean
CommandLineRunner embed(EmbeddingModel embeddingModel) {
    return _ -> {
        var embeddings = embeddingModel.embed("And Gandalf yelled: 'You shall not pass!'");
        System.out.println("Size of the embedding vector: " + embeddings.length);
    };
}
```

Before moving on, consider reading the [Embeddings](https://docs.spring.io/spring-ai/reference/concepts.html#_embeddings)
section from the Spring AI documentation.

#### 1.1 Bootstrapping the Ollama integration

The project is already equipped with the Spring AI Ollama integration. Inspect the `build.gradle` file to check the
configured dependencies. Then, open the `application.yml` file and configure the embedding model to use (`nomic-embed-text`).
You can also configure Spring AI to download the model automatically at startup time if it's not available locally.

```yaml
spring:
  ai:
    ollama:
      init:
        pull-model-strategy: when_missing
        chat:
          include: false
      embedding:
        options:
          model: nomic-embed-text
```

In the `Application` class, create a `CommandLineRunner` bean with an autowired `EmbeddingModel` object.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner embed(EmbeddingModel embeddingModel) {
        return _ -> {};
    }
}
```

It's a good idea to instantiate a `Logger` object to log conveniently the model responses in the following exercises.

```java
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
}
```

#### 1.2 Make your first call to an embedding model

Time to make your first call to an embedding model using Spring AI! In the `CommandLineRunner` bean,
use the `EmbeddingModel` to generate an embedding for some input text.

```java
@Bean
CommandLineRunner embed(EmbeddingModel embeddingModel) {
    return _ -> {
        var embeddings = embeddingModel.embed("And Gandalf yelled: 'You shall not pass!'");
        logger.info("Size of the embeddings vector: " + embeddings.length);
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootTestRun
```

Before moving on, consider reading the [Embedding Model API](https://docs.spring.io/spring-ai/reference/api/embeddings.html)
section of the Spring AI documentation.

### 2. Ingestion Pipeline

To allow a machine learning model to act on your data, you first need to prepare and load it. This process is often
referred to as the data ingestion pipeline, similar to the ETL (Extract, Transform, Load) pipeline in data engineering.

Spring AI provides primitives to read, transform, and write your data, wrapped in a `Document` abstraction.
This exercise will guide you through the process of creating a simple ingestion pipeline, which ultimately
writes the data to a vector store.

A vector store is a database that stores vectors, which are numerical representations of data. It allows for efficient
similarity search, which is a common operation in machine learning applications.

Spring AI provides a `VectorStore` abstraction for integrating with vector stores via several providers,
including PostgreSQL (pgvector).

Before moving on, consider reading the [ETL Pipeline](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)
and [API Overview](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_api_overview)
sections of the Spring AI documentation.

#### 2.1 Set up an ingestion pipeline

Let's start by creating a new `IngestionPipeline` class below the `Application` class, and autowiring a `VectorStore`
object in the constructor.

For convenience, define a `Logger` object to log messages during the ingestion process and debug any issues that may arise.

```java
@SpringBootApplication
public class Application {
}

@Component
class IngestionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(IngestionPipeline.class);
    
    private final VectorStore vectorStore;

    IngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

}
```

The project includes already a text file and a Markdown file in `src/main/resources/documents`. Autowire them in the
`IngestionPipeline` class as `Resource` objects. You'll soon use them as input for the ingestion pipeline.
If you're curious, check out the contents of the files and learn about Iorek and Lucio.

```java
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

}
```

Since it might take some time to process the data, it's a good idea to run the ingestion pipeline asynchronously.
Data ingestion typically happens "offline" and doesn't need to be done in real-time. In this example,
we'll run the pipeline when the application starts.

Define a `run()` method that will be called to start the ingestion process. Annotate it with `@PostConstruct`
so that it's called automatically when the application starts.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    void run() {
    }
}
```

Everything is ready to start populating the data ingestion pipeline!

Before moving on, consider reading the [ETL Interfaces](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_etl_interfaces)
section of the Spring AI documentation.

#### 2.2 Read the data (Text)

Spring AI supports reading many different types of files via the `DocumentReader` API. The core framework supports
reading text (`TextReader`) and JSON (`JsonReader`) files. Additional formats are supported by dedicated Spring AI
modules, including `PagePdfDocumentReader` and `ParagraphPdfDocumentReader` (PDF), `MarkdownDocumentReader` (Markdown),
and `TikaDocumentReader` (PDF, DOCX, PowerPoint, HTML, Excel, Text, Markdown, and more).

In the `run()` method, initialize a `List<Document>` to hold all the documents read from the pipeline.
We'll start by reading the text document. Create a `TextReader` object and use it to read the first file.
Explore the `TextReader` API to learn about the available options, including adding custom metadata to the document,
setting the character encoding, and more. Metadata are useful for filtering documentation during the retrieval/search phase.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    void run() {
        List<Document> documents = new ArrayList<>();

        logger.info("Loading .txt files as Documents");
        var textReader = new TextReader(file1);
        textReader.getCustomMetadata().put("location", "North Pole");
        textReader.setCharset(Charset.defaultCharset());
        documents.addAll(textReader.get());
    }
}
```

Before moving on, consider reading the [Text Document Reader](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_text)
section of the Spring AI documentation.

#### 2.3 Read the data (Markdown)

Spring AI supports reading Markdown documents via the Spring AI Markdown Document Reader module
and the Spring AI Tikka Document Reader module (based on Apache Tika). This project is already configured to use the former.
Check out the `build.gradle` file to see the dependencies.

Next, create a `MarkdownDocumentReader` object and use it to read the second file. Explore the `MarkdownDocumentReader` API
to learn about the available options, including setting custom metadata and configuring the parsing of code blocks and blockquotes.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    void run() {
        ...

        logger.info("Loading .md files as Documents");
        var markdownReader = new MarkdownDocumentReader(file2, MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("location", "Italy")
                .build());
        documents.addAll(markdownReader.get());
    }
}
```

Before moving on, consider reading the [Markdown Document Reader](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_markdown)
section of the Spring AI documentation.

#### 2.4 Transform the data

You could write the current `List<Document>` to a database or a file system, but you might want to transform the data first.
Spring AI provides `DocumentTransformer` implementations for common use cases, such as splitting documents into chunks,
summarizing documents, and extracting keywords. The way you chunk the documents can have a significant impact on the
performance of semantic search operations later on.

For this example, split the documents into small chunks with a `TokenTextSplitter`. We'll use small numbers
for the chunk size to keep the example simple and make it easier to retrieve and understand the chunks later.
In a real-world application, you probably want to use larger numbers.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    void run() {
        ...

        logger.info("Split Documents to better fit the LLM context window");
        var textSplitter = TokenTextSplitter.builder()
                .withChunkSize(50)
                .build();
        var transformedDocuments = textSplitter.apply(documents);
    }
}
```

Before moving on, consider reading the [Document Transformers](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_transformers)
section of the Spring AI documentation.

#### 2.5 Write the data

Now that you have a `List<Document>` of transformed documents, you can generate the vector representation (embeddings)
for each of them and write them to a database or a file system. 

Spring AI provides `DocumentWriter` implementations for common use cases, such as writing documents to a database
or a file system. In this example, we'll use a `VectorStore`. A `VectorStore` is a database that stores documents
and their vector representations. It allows you to search for documents that are similar to a given query.
This project is already configured with PostgreSQL, which we can also use as a vector store thanks to
the Spring AI PGVector module. Check out the `build.gradle` file to see the dependencies.

Let's configure the PGVector integration now. Open the `application.yml` file and configure Spring AI to initialize
the database schema automatically. Furthermore, choose the HNSW index type that we'll use later for similarity search,
and 768 dimensionality (that's the dimensions of the vectors generated by `nomic-embed-text`,
the embedding model we've been using in this example).

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 768
        index-type: hnsw
```

You might have noticed that you didn't have to set up any PostgreSQL database so far. That's because we already
have one up and running via Testcontainers, managed by Spring Boot as a dev service. Inspect the
`TestcontainersConfiguration` class to see how it's done. When you run the application via `./gradlew bootTestRun`,
Testcontainers will start a PostgreSQL container and Spring Boot will connect to it automatically.

Complete the ingestion pipeline by adding one final step to write the transformed documents to the vector store.
Behind the scenes, the `add()` method will generate the vector representation for each document and write it to the database.

```java
@Component
public class IngestionPipeline {

    @PostConstruct
    void run() {
        ...

        logger.info("Creating and storing Embeddings from Documents");
        vectorStore.add(transformedDocuments);
    }
}
```

Run the application and check the console output to track the ingestion pipeline execution happening at startup.
Make sure you have Podman Desktop or Docker Desktop running on your machine before running the application.

```shell
./gradlew bootTestRun
```

In the next section, you'll learn how to use the vector store to search for documents that are similar to a given query.

Before moving on, consider reading the [Document Writers](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_writers)
section of the Spring AI documentation.

### 3. Semantic Search

Now that you have a vector store with documents and their vector representations, you can use it to search for documents
that are similar to a given query.

Spring AI provides a powerful query system to perform semantic search on a vector store, including a portable SQL-like
metadata filter API.

#### 3.1 Set up semantic search

In the `Application` class, create a `CommandLineRunner` bean with an autowired `VectorStore` object.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner search(VectorStore vectorStore) {
        return _ -> {};
    }
}
```

Before moving on, consider reading the [Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
and [API Overview](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#api-overview)
sections of the Spring AI documentation.

#### 3.2 Perform semantic search

Let's start with a basic search. In the `CommandLineRunner` bean, use the `VectorStore` to search for documents
that have a similar meaning to the given query. The `similaritySearch()` method returns a list of `Document` objects.

```java
@Bean
CommandLineRunner search(VectorStore vectorStore) {
    return _ -> {
        var query = "Iorek's biggest dream";
        var documents = vectorStore.similaritySearch(SearchRequest.query(query));
        documents.stream()
                .map(Document::getContent)
                .forEach(logger::info);
    };
}
```

Run the application and check the console output for the vector search results.

```shell
./gradlew bootTestRun
```

How is the quality of the search results? Are they relevant to answer the query?

The `similaritySearch()` method accepts a `SearchRequest` object that can be used to refine the semantic search.
Inspect the API and try out different options. For example, you can limit the number of results (the ones with the highest
similarity value) and define a threshold for how similar two documents should be.

```java
@Bean
CommandLineRunner search(VectorStore vectorStore) {
    return _ -> {
        var query = "Iorek's biggest dream";
        var documents = vectorStore.similaritySearch(SearchRequest.query(query)
                .withSimilarityThreshold(0.5)
                .withTopK(3));
        documents.stream()
                .map(Document::getContent)
                .forEach(logger::info);
    };
}
```

Then try performing a search again and see how the results change.

```shell
./gradlew bootTestRun
```

You can also filter the search results based on metadata. For example, you can search for documents that contain
a specific keyword or were written by a specific author. The `SearchRequest` object allows you to define metadata filters
using an SQL-like syntax.

```java
@Bean
CommandLineRunner search(VectorStore vectorStore) {
    return _ -> {
        var query = "Iorek's biggest dream";
        var documents = vectorStore.similaritySearch(SearchRequest.query(query)
                .withFilterExpression("location == 'North Pole'")
                .withSimilarityThreshold(0.5)
                .withTopK(3));
        documents.stream()
                .map(Document::getContent)
                .forEach(logger::info);
    };
}
```

Before moving on, consider reading the [Vector Database Example Usage](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_example_usage)
and [Metadata Filters](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#metadata-filters) sections of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on how to design an ingestion pipeline and perform semantic search on a vector store using Spring AI.
