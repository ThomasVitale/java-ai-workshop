# Integrating Java with Chat Models - Basics

## Introduction

Welcome to this hands-on lab on the basics of integrating Java applications with Large Language Models (LLMs).
In this lab, you'll learn how to interact with a chat model, design prompts with user and system messages,
tweak model options to reach desired results, and implement a chatbot with memory.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 23 (installation using [SDKMAN!](https://sdkman.io/jdks) is recommended)
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [Ollama](https://ollama.com/) for running large language models locally.

## Learning Goals

By the end of this lab, you will be able to:

* Manage and run LLMs locally using Ollama
* Integrate chat models into Java applications using Spring AI
* Design effective prompts with user and system messages
* Customize model behavior using runtime options
* Implement a chatbot with conversation memory

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final
state of the project.

The project is configured as a standalone CLI application. You can run the application as follows.

```shell
./gradlew bootRun
```

### 1. Chat Models with Ollama and Spring AI

Spring AI provides a `ChatModel` low-level abstraction for integrating with LLMs via several providers, including Ollama.

When using the _Spring AI Ollama Spring Boot Starter_, a `ChatModel` object is autoconfigured for you to use Ollama.

```java
@Bean
CommandLineRunner chat(ChatModel chatModel) {
    return _ -> {
        var response = chatModel.call("What is the capital of Denmark?");
        System.out.println(response);
    };
}
```

Spring AI also provides a higher-level abstraction for building more advanced LLM workflows: `ChatClient`.
A `ChatClient.Builder` object is autoconfigured for you to build a `ChatClient` object.
Under the hood, it relies on a `ChatModel`.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();
    return _ -> {
        var response = chatClient
                .prompt("What is the capital of Denmark?")
                .call()
                .content();
        System.out.println(response);
    };
}
```

Before moving on, consider reading the [Models](https://docs.spring.io/spring-ai/reference/concepts.html#_models) section from the Spring AI documentation.

#### 1.1 Bootstrapping the Ollama integration

The project is already equipped with the Spring AI Ollama integration. Inspect the `build.gradle` file to check the
configured dependencies. Then, open the `application.yml` file and configure the chat model to use (`llama3.2`).
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
```

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

It's a good idea to instantiate a `Logger` object to log conveniently the chat responses in the following exercises.

```java
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
}
```

#### 1.2 Make your first call to a chat model

Time to make your first call to a chat model using Spring AI! In the `CommandLineRunner` bean, use the `ChatClient`
to prompt the model with a question and get an answer.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var response = chatClient
                .prompt("What is the capital of Denmark?")
                .call()
                .content();
        logger.info(response);
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

Feel free to experiment with different questions and see how the model responses.

Before moving on, consider reading the [Creating a ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_creating_a_chatclient) section of the Spring AI documentation.

#### 1.3 Prompt with user and system messages

Prompts can comprise multiple messages, each with a specific role. A message with the User role includes the user's input.
A message with the System role includes generic instructions for the model on how it should answer user's questions.

The ChatClient API provides a fluent interface to build prompts with user and system messages.
First, update the `CommandLineRunner` bean to pass your question as a user message via the `user()` method.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        response = chatClient.prompt()
                .user("What is the capital of Denmark?")
                .call()
                .content();
        logger.info(response);
    };
}
```

Next, pass a system message to the model via the `system()` method. The system message can include instructions
on how the model should answer the user's question. For example, you can ask the model to answer like a pirate.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        response = chatClient.prompt()
                .system("You are a helpful assistant who always answers like a pirate.")
                .user("What is the capital of Denmark?")
                .call()
                .content();
        logger.info(response);
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

Feel free to experiment with different system messages to see how they affect the model's responses.
For example, you can define a persona for the model, such as an engineer, a customer assistant, or a poet.

You should take good care of the prompts you design, similar to how you would for database schemas or API contracts,
as they can significantly influence the model's responses.

Spring AI lets you externalize any prompt and load it as a `Resource` via the `ChatClient`.
In the `src/main/resources` folder, create a `prompts/system-message.st` file with the following content.

```text
You are a funny and hilarious assistant.
Answer in one sentence using a very informal language
and start the answer with a knock knock joke.
```

Load the file content as a `Resource` inside the `CommandLineRunner` bean.

```java
@Bean
CommandLineRunner chat(
        ChatClient.Builder chatClientBuilder, 
        @Value("classpath:/prompts/system-message.st") Resource systemMessageResource
) {
}
```

Instead of the hard-coded system message, try using the `systemMessageResource` in the `system()` method.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        response = chatClient.prompt()
                .system(systemMessageResource)
                .user("What is the capital of Denmark?")
                .call()
                .content();
        logger.info(response);
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

Before moving on, consider reading the [ChatClient Fluent API](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chatclient_fluent_api) section of the Spring AI documentation.

#### 1.4 Prompt templates

Instead of asking the user to provide the entire prompt message, you can prepare a template and insert the user values
into pre-defined placeholders.

First, let's go back to use a hard-coded system message for easy customization.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        response = chatClient.prompt()
                .system("You are a helpful assistant who always answers like a pirate.")
                .user("What is the capital of Denmark?")
                .call()
                .content();
        logger.info(response);
    };
}
```

Next, replace the user message with a template that includes a placeholder for the user value.
This time, we want the model to help us compose a short pirate song about a specific topic.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var topic = "hobbits";
        response = chatClient.prompt()
                .system("You are a helpful assistant who always answers like a pirate.")
                .user(user -> user
                        .text("Compose a short pirate song about {topic}.")
                        .param("topic", topic)
                )
                .call()
                .content();
        logger.info(response);
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

Have fun with different templates and see how the model responds to them.

Before moving on, consider reading the [Prompt](https://docs.spring.io/spring-ai/reference/api/prompt.html#_api_overview) section of the Spring AI documentation.

#### 1.5 Prompt with model options

Spring AI allows you to customize the options for each model integration via configuration properties (`application.yml`).
We refer to them as "build-time options" and they are used globally.

Open the `application.yml` file and explore what options you have available to customize the Ollama chat integration.
For example, you can change the temperature, which determines the level of creativity for the model answer.

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: llama3.2
          temperature: 0.7
```

Spring AI also lets you customize a model integration "at run-time" via the `ChatClient` API.
That's very useful to tweak the model behavior for specific requests or even for calling different models.

This time, we won't use any system message, and we'll ask a more complicated question to the model.
We'll also use the `options()` method to set the temperature to 0.9, which should make the model more creative.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        response = chatClient
                .prompt("What is the meaning of life? Give me a one-sentence, philosophical answer.")
                .options(ChatOptionsBuilder.builder()
                        .withTemperature(0.9)
                        .build())
                .call()
                .content();
        logger.info(response);
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

Try different temperatures with different prompts and see how they affect the model's responses.

#### 1.6 Make your first streaming call to a chat model

Spring AI supports both blocking and streaming interactions. You can instruct the model to stream back its answer
by replacing the `call()` clause of the ChatClient API with `stream()`.

The result is a `Flux<String>` that you can subscribe to and process the model's responses as they come.
It's a concept from reactive programming and provided by Project Reactor, the foundation of the reactive stack in Spring.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        chatClient
                .prompt("What is the meaning of life? Give me a one-sentence, philosophical answer.")
                .options(ChatOptionsBuilder.builder()
                        .withTemperature(0.9)
                        .build())
                .stream()
                .content()
                .doOnEach(signal -> logger.info(signal.get()))
                .blockLast();
    };
}
```

Run the application and check the console output for the model's response. You should see the model's responses streaming in,
one token at a time.

```shell
./gradlew bootRun
```

Before moving on, consider reading the [Chat Client Responses](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chatclient_responses),
[call()](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_call_return_values),
and [stream()](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_stream_return_values) sections of the Spring AI documentation.

### 2. Building a chatbot

By design, large language models have no memory. If you try calling one of the previously implemented endpoints
and ask the model about your previous conversations, it will have no recollection of them. Therefore, it's up to the
application to keep record of the ongoing conversation and provide the chat history to the model.

Spring AI provides the `ChatMemory` API to manage the history of a chat conversation with a model.
You will use an in-memory implementation, but storing data externally (e.g. in a database) is also supported.

#### 2.1 Defining a ChatMemory implementation

In the `Application` class, define a `@Bean` of type `ChatMemory` using the built-in `InMemoryChatMemory` implementation.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

}
```

#### 2.2 Your first chatbot

Let's start by autowiring the `ChatMemory` bean in the `CommandLineRunner` bean. This time, when building the `ChatClient`,
we will customize it with memory using the `MessageChatMemoryAdvisor` and the `ChatMemory` bean.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
    var chatClient = chatClientBuilder
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();
}
```

The `MessageChatMemoryAdvisor` will use the `ChatMemory` bean to store and retrieve the conversation history
for each chat session processed by the `ChatClient`. Each chat session is identified by a unique chat ID,
which is passed as a parameter on each call. For example, in a web application, the chat ID could be the user's session ID.
When using WebSockets, the chat ID could be the WebSocket session ID.

Let's simulate a conversation with the model to test if it remembers the previous interactions.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
    var chatClient = chatClientBuilder
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();

    return _ -> {
        var chatId = "007";
        
        response = chatBot(chatClient, chatId, "My name is Bond. James Bond.");
        logger.info(response);

        response = chatBot(chatClient, chatId, "What's my name?");
        logger.info(response);

        response = chatBot(chatClient, chatId, "I was counting on your discretion. Please, do not share my name!");
        logger.info(response);
    };
}

private String chatBot(ChatClient chatClient, String chatId, String input) {
    return chatClient.prompt()
            .user(input)
            .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
            .call()
            .content();
}
```

Run the application and check the console output for the model's responses. You should see the model remembering the previous interactions.

```shell
./gradlew bootRun
```

Feel free to experiment with different chat IDs and see how the model responds to them and whether it keeps
the memory private for each chat session.

Before moving on, consider reading the [Advisors](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_advisors),
[Advisor Configuration in ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_advisors),
and [Chat Memory](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chat_memory) sections of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on the basics of integrating Java applications with chat models.
