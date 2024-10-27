# Function Calling

## Introduction

Welcome to this hands-on lab on function calling with Large Language Models (LLMs). In this lab, you'll learn
how to bring your own APIs and data to LLMs and use them to perform tasks. This is the core of agentic behavior.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 22 (install using [SDKMAN!](https://sdkman.io/jdks))
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [Ollama](https://ollama.com/) for running large language models locally.

## Learning Goals

By the end of this lab, you will be able to:

* Understand the concept of function calling in the context of LLMs
* Configure and implement functions/tools for use with LLMs
* Integrate function calling capabilities into a Spring Boot application
* Use the Spring AI library to interact with LLMs and execute function calls

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final
state of the project.

The project is configured as a standalone CLI application. You can run the application as follows.

```shell
./gradlew bootRun
```

### 1. Agentic AI with Function Calling

Large language models are trained on a large corpus of text, but they don't have access to the latest information
nor do they know about your specific documents. To overcome this limitation, you can use function calling
to bring your own APIs and data to LLMs.

It's also the foundation for building agentic AI applications, where the AI can perform tasks by calling functions.
In reality, LLMs are not the ones calling the functions, but a developer-provided function-calling handler
that interprets the LLM's output and calls the appropriate functions. That's why it's better to think in terms of tools
rather than function calling.

Before moving on, consider reading the [Function Calling](https://docs.spring.io/spring-ai/reference/concepts.html#concept-fc)
section of the Spring AI documentation.

#### 1.1 Configure functions/tools

The project is already equipped with the Spring AI Ollama integration. Inspect the `build.gradle` file to check the
configured dependencies.

It's also equipped with an API we would like to make available to the LLM. It's a simple API that returns a list of
books available in a library for a given author. Inspect the `BookService` class to understand how it works.

Spring AI provides a few different ways to configure functions/tools. In this exercise, you'll use the `@Bean` annotation
to register the `getBooksByAuthor` method in `BookService` as a function/tool.

In the `Application` class, define a Java `Function` called `booksByAuthor` that takes an `Author` as input
and returns a `List<Book>` as output. The function should call the `getBooksByAuthor` method in `BookService`.

It is extremely important to provide a good description of the function via the `@Description` annotation. This description
will be used by the LLM to decide when to call the function.

It's also a good idea to instantiate a `Logger` object to log conveniently when a function is called.

```java
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Bean
    @Description("Get the list of books written by the given author available in the library")
    Function<BookService.Author, List<BookService.Book>> booksByAuthor(BookService bookService) {
        logger.info("Calling function...");
        return bookService::getBooksByAuthor;
    }

}
```

Not all chat models support function calling. If you're using the `llama3.2` or `qwen2.5` models, you're good to go
after configuring the model in the `application.yml` file. It's also a good idea to set the `temperature` to `0.0`
to make the model output more precise, which is important when integrating with functions.
You can also configure Spring AI to download the model automatically at startup time if it's not available locally.

```yaml
spring:
  ai:
    ollama:
      init:
        pull-model-strategy: when_missing
        embedding:
          include: false
      chat:
        options:
          model: llama3.2
          temperature: 0.0
```

Before moving on, consider reading the [Function Calling API](https://docs.spring.io/spring-ai/reference/api/functions.html)
section of the Spring AI documentation.

#### 1.2 Use functions/tools with LLMs

In the `Application` class, create a `CommandLineRunner` bean with an autowired `ChatClient.Builder` object
that you can use to initialize a `ChatClient` object.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
        var chatClient = chatClientBuilder.build();
        return _ -> {};
    }
}
```

In the `CommandLineRunner` bean, use the `ChatClient` to prompt the user with a question about books written by
a specific author. Use a template to define a user prompt that includes the author name.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();
    return _ -> {
        var authorName = "Philip Pullman";
        var response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("What books written by {author} are available in the library?")
                        .param("author", authorName)
                )
                .call()
                .content();
        logger.info(response);
    };
}
```

At this point, the LLM doesn't know how to answer this question yet . The prompt should ask the LLM to use the `booksByAuthor`
function to get the list of available books written by the given author. The ChatClient API provides a `functions()` method
that allows you to specify the functions/tools that the LLM can/should use.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();
    return _ -> {
        var authorName = "Philip Pullman";
        var response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("What books written by {author} are available in the library?")
                        .param("author", authorName)
                )
                .functions("booksByAuthor")
                .call()
                .content();
        logger.info(response);
    };
}
```

Run the application and check the console output for the model's response and to verify if the function was called.

```shell
./gradlew bootRun
```

It's up to the model to decide if and when a certain function should be called. Once again, prompt design is crucial
to get the desired outcome. It's also important to provide a good description of the function via
the `@Description` annotation.

Try changing the prompt to see how the model behaves and monitor the logs to see if and when the function is called.

Before moving on, consider reading the [Ollama Function Calling](https://docs.spring.io/spring-ai/reference/api/chat/functions/ollama-chat-functions.html) section of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on function calling and learned how to use functions/tools with LLMs. You've gained practical experience in integrating custom APIs with language models, enhancing their capabilities, and building more intelligent and context-aware applications.
