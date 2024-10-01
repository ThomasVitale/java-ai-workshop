# Function Calling

## Introduction

Welcome to this hands-on lab on function calling with Large Language Models (LLMs). In this lab, you'll learn how to bring your own APIs and data to LLMs and use them to perform tasks. This is the core of agentic behavior.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 22 (install using [SDKMAN!](https://sdkman.io/jdks))
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [httpie](https://httpie.io/cli) for making API calls
* [Ollama](https://ollama.com/) for running large language models locally

Ensure you have either the `qwen2.5` or the `mistral` chat model available in Ollama:

```shell
ollama pull qwen2.5
ollama pull mistral
```

## Learning Goals

By the end of this lab, you will be able to:

* Understand the concept of function calling in the context of LLMs
* Configure and implement functions/tools for use with LLMs
* Integrate function calling capabilities into a Spring Boot application
* Use the Spring AI library to interact with LLMs and execute function calls

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final state of the project.

You can start the application as follows. It will reload automatically whenever you make changes. On Visual Studio Code, it works out of the box. If you're using IntelliJ IDEA, follow the [documentation](https://www.jetbrains.com/help/idea/spring-boot.html#enable-auto-restart) to configure IntelliJ IDEA to support the Spring Boot DevTools.

```shell
./gradlew bootTestRun
```

### 1. Agentic AI with Function Calling

Large language models are trained on a large corpus of text, but they don't have access to the latest information nor do they know about your specific documents. To overcome this limitation, you can use function calling to bring your own APIs and data to LLMs.

It's also the foundation for building agentic AI applications, where the AI can perform tasks by calling functions. In reality, LLMs are not the ones calling the functions, but a developer-provided function calling handler that interprets the LLM's output and calls the appropriate functions. That's why it's better to think in terms of tools rather than functions.

Before moving on, consider reading the [Function Calling](https://docs.spring.io/spring-ai/reference/concepts.html#concept-fc) section of the Spring AI documentation.

#### 1.1 Configure functions/tools

* The project comes with an API we would like to make available to the LLM. It's a simple API that returns a list of books available in a library for a given author. Inspect the `BookService` class to understand how it works.
* Spring AI provides a few different ways to configure functions/tools. In this exercise, you'll use the `@Bean` annotation to register the `getBooksByAuthor` method in `BookService` as a function/tool.
* Create a new `Functions` configuration class and define a Java `Function` called `booksByAuthor` that takes an `Author` as input and returns a `List<Book>` as output. The function should call the `getBooksByAuthor` method in `BookService`.

```java
@Configuration(proxyBeanMethods = false)
public class Functions {

    @Bean
    @Description("Get the list of available books written by the given author")
    public Function<BookService.Author, List<BookService.Book>> booksByAuthor(BookService bookService) {
        return bookService::getBooksByAuthor;
    }

}
```

* Not all chat models support function calling. If you're using the `qwen2.5` or `mistral` model, you're good to go after configuring the model in the `application.yml` file.

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen2.5
          temperature: 0.7
```

* Before moving on, consider reading the [Function Calling API](https://docs.spring.io/spring-ai/reference/api/functions.html) section of the Spring AI documentation.

#### 1.2 Use functions/tools with LLMs

* Create a new `ChatController` class, autowire a `ChatClient.Builder` object in the constructor, and use it to initialize a `ChatClient` object.

```java
@RestController
class ChatController {

    private final ChatClient chatClient;

    ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

}
```

* Create a new `/chat` endpoint that accepts an author name as input and returns a `String` as output. The endpoint should use the `ChatClient` to generate a response based on the input.
* Use a template to define a user prompt that includes the author name.

```java
@RestController
class ChatController {

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "J.R.R. Tolkien") String authorName) {
        var userPromptTemplate = "What books written by {author} are available to read?";
        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("author", authorName)
                )
                .call()
                .content();
    }

}
```

* At this point, the LLM doesn't know how to answer this question. The prompt should ask the LLM to use the `booksByAuthor` function to get the list of available books written by the given author. The ChatClient API provides a `functions()` method that allows you to specify the functions/tools that the LLM can/should use.

```java
@RestController
class ChatController {

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "J.R.R. Tolkien") String authorName) {
        var userPromptTemplate = "What books written by {author} are available to read?";
        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("author", authorName)
                )
                .functions("booksByAuthor")
                .call()
                .content();
    }

}
```

* Test the newly created endpoint with the default author (Tolkien).

```shell
http :8080/chat -b
```

* Specify a different author

```shell
http :8080/chat authorName=="Philip Pullman" -b
```

* It's up to the model to decide if and when a certain function should be called. Once again, prompt design is crucial to get the desired outcome. It's also important to provide a good description of the function via the `@Description` annotation.

* Before moving on, consider reading the [Ollama Function Calling](https://docs.spring.io/spring-ai/reference/api/chat/functions/ollama-chat-functions.html) section of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on function calling and learned how to use functions/tools with LLMs. You've gained practical experience in integrating custom APIs with language models, enhancing their capabilities, and building more intelligent and context-aware applications.
