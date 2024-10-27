# Retrieval Augmented Generation

## Introduction

Welcome to this hands-on lab on Retrieval Augmented Generation (RAG). In this lab, you'll learn how to
use a vector database to store and retrieve documents, and how to use a large language model (LLM)
to generate answers based on those documents.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 23 (install using [SDKMAN!](https://sdkman.io/jdks))
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [httpie](https://httpie.io/cli) for making API calls
* [Ollama](https://ollama.com/) for running large language models locally.

## Learning Goals

By the end of this lab, you will be able to:

* Implement a Retrieval Augmented Generation (RAG) system using Spring AI and Ollama
* Create a question-answering system that leverages RAG techniques
* Evaluate the performance of your LLM-powered application
* Configure and utilize observability features in Spring AI applications

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final
state of the project.

The project is configured as a standalone CLI application. You can run the application as follows.

```shell
./gradlew bootTestRun
```

Wait until step 1.1 before starting the application.

It will reload automatically whenever you make changes. On Visual Studio Code, it works out of the box.
If you're using IntelliJ IDEA, follow the [documentation](https://www.jetbrains.com/help/idea/spring-boot.html#enable-auto-restart)
to configure IntelliJ IDEA to support the Spring Boot DevTools.

This time, wait until the end of step 1.1 to start the application.

### 1. Question Answering with Documents

Large language models are trained on a large corpus of text, but they don't have access to the latest information
nor do they know about your specific documents. To answer questions based on specific documents, we need to use
a technique called Retrieval Augmented Generation (RAG). 

The idea is to first retrieve the relevant documents from a vector database, and then use the large language model
to generate an answer based on those documents. This technique is cheaper and faster than fine-tuning
a large language model on your specific documents. The reason we only send a few documents to the large language model
is that the context window of the model is limited. Furthermore, cloud-based models are billed per token,
so sending fewer tokens to the model can save costs. Finally, sending too much information risks to confuse the model.

In this exercise, you'll learn how to implement a simple question answering system using Spring AI and Ollama.

#### 1.1 Set up the question answering system

The project is already equipped with the Spring AI Ollama and PGVector integration.
Check the `build.gradle` file to see the dependencies.

It also includes the same Ingestion Pipeline you built in the previous lab to ingest documents into the vector database.
That's a pre-requisite for the retrieval step in the RAG system. Inspect the `IngestionPipeline` class. 
During the application start-up phase, the two documents in the `src/main/resources/documents` folder
are ingested into the vector database (PostgreSQL). Similar to the previous lab, Testcontainers is used to spin up
a PostgreSQL database automatically with the pgvector extension, so you don't need to set up the database manually.

The first thing you need to do is to configure the chat model, the embedding model, and the vector store
in the `application.yml` file. You can also configure Spring AI to download the model automaticall
at startup time if it's not available locally.

Open the `application.yml` file and configure all those aspects.

```yaml
spring:
  ai:
    ollama:
    init:
      pull-model-strategy: when_missing
    chat:
      options:
        model: qwen2.5
    embedding:
      options:
        model: nomic-embed-text
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 768
        index-type: hnsw
```

Run the application and check the ingestion pipeline output in the logs.

```shell
./gradlew bootTestRun
```

Now that we verified the ingestion pipeline is working, let's move on to the retrieval step.

Create a new `ChatService` class, autowire a `ChatClient.Builder` object in the constructor,
and use it to initialize a `ChatClient` object. You also need to autowire a `VectorStore`
which will be needed for the retrieval step.

```java
@Service
class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

}
```

#### 1.2 Implement the question answering system

Implement the `chat` method in the `ChatService` class. It should take a question as input and return a `ChatResponse`
object as output.

```java
@Service
class ChatService {

    ChatResponse chat(String query) {
        return chatClient.prompt()
                .user(query)
                .call()
                .chatResponse();
    }

}
```

Spring AI provides a built-in `Advisor` to implement RAG without having to retrieve data explicitly from the vector
database and include them in the prompt. The `QuestionAnswerAdvisor` will do that for you. The SearchRequest object
can be configured to refine the retrieval step.

```java
@Service
class ChatService {

    ChatResponse chat(String query) {
        return chatClient.prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults().withTopK(5)))
                .user(query)
                .call()
                .chatResponse();
    }

}
```

In the next section, you'll expose the question answering system via a REST API and test it.

#### 1.3 Test the question answering system

Create a new `ChatController` class, autowire the `ChatService` object in the constructor, and implement a `/chat` endpoint
that takes a question as input and returns a `String` object as output.

Notice how the answer from the model is encapsulated in a `ChatResponse` object. You can extract the answer
from the `ChatResponse` object using the `getResult().getOutput().getContent()` method.

```java
@RestController
class ChatController {

    private final ChatService chatService;

    ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    String chat(@RequestBody String query) {
        return chatService.chat(query).getResult().getOutput().getContent();
    }

}
```

Finally, you can test the newly created endpoint.

```shell
http --raw "What is Iorek's biggest dream?" :8080/chat -b --pretty none
```

```shell
http --raw "Who is Lucio?" :8080/chat -b --pretty none
```

How good is the result? You can refine the retrieval step by configuring the `SearchRequest` parameters
Try tweaking `filterExpression`, `similarityThreshold`, and `topK`.

Before moving on, consider reading the [Bringing Your Data & APIs to the AI Model](https://docs.spring.io/spring-ai/reference/concepts.html#_bringing_your_data_apis_to_the_ai_model)
and [Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/concepts.html#concept-rag)
sections of the Spring AI documentation.

### 2. Evaluation

Large language models are probabilistic and can produce different results for the same input. Evaluation is a critical
step in the development of LLM-powered applications to ensure that the model is performing as expected.

Spring AI provides an evaluation framework to evaluate the performance of LLM-powered applications. It allows you
to define a set of evaluation criteria and evaluate the model's performance against those criteria.
Currently, evaluation strategies for relevancy and fact-checking are supported. They both rely on a strategy called
"LLM as a Judge", which uses a language model to evaluate the model output.

#### 2.1 LLM as a Judge

The project contains already an empty `ApplicationTests` class to test the RAG system implemented in the `ChatService` class.
We'll write integration tests, so we are also importing the `TestcontainersConfiguration` to test against
a real vector store (in this case, PostgreSQL/PGVector).

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ApplicationTests {
    
}
```

We'll use an evaluation technique called "LLM-as-a-Judge", which relies on a language model to evaluate the relevancy
of the output. For that reason, we need to autowire both the `ChatService` object and the `ChatClient.Builder`
we'll use to build a new `ChatClient` object to interact with a model for evaluation purposes.

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ApplicationTests {

    @Autowired
    ChatService chatService;

    @Autowired
    ChatClient.Builder chatClientBuilder;

}
```

#### 2.2 Relevancy Evaluation

Spring AI provides the `Evaluator` abstraction. In this example, we'll use the `RelevancyEvaluator` implementation
to evaluate the relevancy of the output from the question-answering system. 

In the `evaluateRelevancy()` test case, call the `chatService.chat()` method to get the `ChatResponse` object,
and extract both the answer and the contextual documents that the `QuestionAnswerAdvisor` retrieved for you
and passed to the model.

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ApplicationTests {

    @Test
    void evaluateRelevancy() {
        String question = "What is Iorek's biggest dream?";
        ChatResponse response = chatService.chat(question);
        String answer = response.getResult().getOutput().getContent();
        List<Content> contextDocuments = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
    }

}
```

The relevancy evaluation strategy works in this way: given the question and the documents retrieved from the vector store
(the context), the evaluator will ask the language model to evaluate the relevancy of the answer to the question,
given the context. The result is binary: either the evaluation passes or fails.

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ApplicationTests {

    @Test
    void evaluateRelevancy() {
        String question = "What is Iorek's biggest dream?";
        ChatResponse response = chatService.chat(question);
        String answer = response.getResult().getOutput().getContent();
        List<Content> contextDocuments = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);

        RelevancyEvaluator relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, contextDocuments, answer);
        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);
    
        System.out.println(evaluationResponse);

        assertThat(evaluationResponse.isPass()).isTrue();
    }

}
```

Run the tests and verify that the evaluation passes. If it doesn't, consider inspecting the search configuration
in the retrieval step or the tokenization configuration in the data ingestion step.

```shell
./gradlew test
```

For simplicity, we re-used the same chat model configuration. However, for evaluation purposes, you probably want to
use a different model, possibly a specialized one. That's how the Fact-Checking Evaluator is configured in the
project available in the `end` folder. Check it out!

Before moving on, consider reading the [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html)
section of the Spring AI documentation.

### 3. Observability

When building LLM-powered applications, it's essential to have visibility into the system's behavior.
Besides the traditional use cases for observability, it's crucial to have insights into the prompt and the response
from the model, the documents retrieved from the vector store, and the options configured for the model
and the vector store. That way, you can refine the prompt and iterate on the model configuration to improve
the quality of the output.

Spring AI provides observability features building on top of the Spring Observability framework, built on top of
Micrometer and OpenTelemetry.

#### 3.1 Configuring observability

The project comes already configured with observability for Spring AI. On top of the default observability configuration,
you can choose to include in the exported telemetry also the content of prompts and responses.
Do so from the `application.yml` file. These options are convenient during development, but you don't want to
include them in production to avoid leaking sensitive data. That's why they are disabled by default.

```yaml
spring:
  ai:
    chat:
      client:
        observations:
          include-input: true
      observations:
        include-completion: true
        include-prompt: true
    vectorstore:
      observations:
        include-query-response: true
```

You won't need to set up any observability backend. That's because we already have one up and running via Testcontainers,
managed by Spring Boot as a dev service. Inspect the `TestcontainersConfiguration` class to see how it's done.
When you run the application via `./gradlew bootTestRun`, Testcontainers will start a Grafana observability platform (LGTM)
and Spring Boot will connect to it automatically.

#### 3.2 Inspecting metrics and traces

Let's start from a clean slate. Stop the application and start it again.

```shell
./gradlew bootTestRun
```

Send a few requests to the application to populate logs, metrics, and traces.

```shell
http --raw "What is Iorek's biggest dream?" :8080/chat -b --pretty none
```

```shell
http --raw "Who is Lucio?" :8080/chat -b --pretty none
```

Grafana is listening on port 3000. Check your container runtime to find the port to which it is exposed on your localhost
and access Grafana from http://localhost:<port>. The credentials are `admin`/`admin`. Spring Boot also logs the Grafana URL
in the console output.

The application is automatically configured to export logs, metrics, and traces to the Grafana LGTM stack via OpenTelemetry.
You can inspect all telemetry data from the Grafana UI. You can query logs and traces from the "Explore" page, selecting
the "Loki" data source for logs and the "Tempo" data source for traces. You can also visualize metrics in "Explore > Metrics". 

Take your time to follow the path of data through the ingestion pipeline, how the advisors manage the retrieval step
with the vector store, and how Spring AI collects all the information in an augmented prompt to the chat model.

Before moving on, consider reading the [Observability](https://docs.spring.io/spring-ai/reference/observabilty/index.html)
section of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on Retrieval Augmented Generation (RAG) and learned the basics of evaluation
and observability for safer and more robust LLM-powered applications!
