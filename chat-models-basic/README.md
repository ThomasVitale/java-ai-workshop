# Integrating Java with Chat Models - Basics

## Introduction

Welcome to this hands-on lab on the basics of integrating Java applications with Large Language Models (LLMs). In this lab, you'll learn how to interact with a chat model, design prompts with user and system messages, tweak model options to reach desired results, and implement a chatbot with memory.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 22 (installation using [SDKMAN!](https://sdkman.io/jdks) is recommended)
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [httpie](https://httpie.io/cli) for making API calls
* [Ollama](https://ollama.com/) for running large language models locally

Ensure you have either the `qwen2.5` or the `mistral` model available in Ollama:

```shell
ollama pull qwen2.5
ollama pull mistral
```

## Learning Goals

By the end of this lab, you will be able to:

* Manage and run LLMs locally using Ollama
* Integrate chat models into Java applications using Spring AI
* Design effective prompts with user and system messages
* Customize model behavior using runtime options
* Implement a chatbot with conversation memory

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final state of the project.

You can start the application as follows. The application will reload automatically whenever you make changes. On Visual Studio Code, it works out of the box. If you're using IntelliJ IDEA, follow the [documentation](https://www.jetbrains.com/help/idea/spring-boot.html#enable-auto-restart) to configure the IDEA to support the Spring Boot DevTools.

```shell
./gradlew bootTestRun
```

### 1. Chat Models with Ollama and Spring AI

Spring AI provides a `ChatModel` low-level abstraction for integrating with LLMs via several providers, including Ollama.

When using the _Spring AI Ollama Spring Boot Starter_, a `ChatModel` object is autoconfigured for you to use Ollama.

```java
@RestController
class ChatController {
    private final ChatModel chatModel;

    ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "What did Gandalf say to the Balrog?") String question) {
        return chatModel.call(question);
    }
}
```

Spring AI also provides a higher-level abstraction for building more advanced LLM workflows: `ChatClient`.
A `ChatClient.Builder` object is autoconfigured for you to build a `ChatClient` object. Under the hood, it relies on a `ChatModel`.

```java
@RestController
class ChatController {
    private final ChatClient chatClient;

    ChatClientController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "What did Gandalf say to the Balrog?") String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
```

Before moving on, consider reading the [Models](https://docs.spring.io/spring-ai/reference/concepts.html#_models) section from the Spring AI documentation.

#### 1.1 Bootstrapping the Ollama integration

* The project is already equipped with the Spring AI Ollama integration. Open the `application.yml` file and configure the chat model to use (`qwen2.5` or `mistral`, depending on which model you downloaded).

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen2.5
```

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

#### 1.2 Make your first call to a chat model

* In the `ChatController` class, define a `chat()` method accepting some text as a request parameter and returning a `String`. Configure it to handle GET requests to the `/chat` endpoint.

```java
@RestController
class ChatController {

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "What did Gandalf say to the Balrog?") String question) {
    }

}
```

* Use the previously configured `ChatClient`, pass the input question in the prompt, and call the chat model to get an answer.

```java
@RestController
class ChatController {

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "What did Gandalf say to the Balrog?") String question) {
        return chatClient.prompt(question)
                .call()
                .content();
    }

}
```

* Test the newly created endpoint

```shell
http :8080/chat -b
```

* You can also pass a custom question.

```shell
http :8080/chat question=="What is the capital of Denmark?" -b
```

* Before moving on, consider reading the [Creating a ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_creating_a_chatclient) section of the Spring AI documentation.

#### 1.3 Prompt with user and system messages

* Prompts can comprise multiple messages, each with a specific role. A message with the User role includes the user's input. A message with the System role includes generic instructions for the model on how it should answer user's questions.
* In the `ChatController` class, define a `chatRoles()` method accepting some text as a request parameter and returning a `String`. Make it handle POST requests to the `/chat/roles` endpoint.

```java
@RestController
class ChatController {

    @PostMapping("/chat/roles")
    String chatRoles(@RequestBody String input) {
        return null;
    }

}
```

* Similar to the previous example, use the `ChatClient` to make a call to the chat model. This time, pass the user's input as a User message.

```java
@RestController
class ChatController {

    @PostMapping("/chat/roles")
    String chatRoles(@RequestBody String input) {
        return chatClient.prompt()
                .user(input)
                .call()
                .content();
    }

}
```

* You can also pass dedicated instructions for the model on how to handle the chat interactions. For example, you can ask the model to answer like a pirate.

```java
@RestController
class ChatController {

    @PostMapping("/chat/roles")
    String chatRoles(@RequestBody String input) {
        return chatClient.prompt()
                .system("""
                    You are a helpful assistant who always answers like a pirate.
                """)
                .user(input)
                .call()
                .content();
    }

}
```

* Let's give it a try.

```shell
http --raw "What is the meaning of life?" :8080/chat/roles -b --pretty none
```

* Spring AI lets you externalize any prompt and load it as a `Resource` via the `ChatClient`. In the `src/main/resources` folder, create a `prompts/system-message.st` file with the following content.

```text
You are a funny and hilarious assistant.
Answer in one sentence using a very informal language
and start the answer with a knock knowck joke.
```

* Load the file content as a `Resource` inside the `ChatController` class.

```java
@RestController
class ChatController {

    private final ChatClient chatClient;

    private final Resource systemMessageResource;

    ChatController(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
        this.chatClient = chatClientBuilder.build();
        this.systemMessageResource = systemMessageResource;
    }

}
```

* In the `ChatController` class, duplicate the `chatRoles` method and name it `chatExternal`. Then, replace the explicit system message with the `systemMessageResource`.

```java
@RestController
class ChatController {

    @PostMapping("/chat/external")
    String chatExternal(@RequestBody String input) {
        return chatClient.prompt()
                .system(systemMessageResource)
                .user(input)
                .call()
                .content();
    }

}
```

* Let's give it a try.

```shell
http --raw "What did Gandalf say to the Balrog?" :8080/chat/roles -b --pretty none
```

* Before moving on, consider reading the [ChatClient Fluent API](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chatclient_fluent_api) section of the Spring AI documentation.

#### 1.4 Prompt templates

* Instead of asking the user to provide the entire prompt message, you can prepare a template and insert the user values into pre-defined placeholders.

* In the `ChatController` class, duplicate the `chatRoles` method and name it `chatTemplate`, accepting requests via `/chat/template`.

```java
@RestController
class ChatController {

    @PostMapping("/chat/template")
    String chatTemplate(@RequestBody String topic) {
        return chatClient.prompt()
                .system("""
                    You are a helpful assistant who always answers like a pirate.
                """)
                .user(input)
                .call()
                .content();
    }

}
```

* Instead of passing the user input directly, define a prompt template in the user message, and populate the placeholder with the value provided by the user.

```java
@PostMapping("/chat/template")
    String chatTemplate(@RequestBody String topic) {
        return chatClient.prompt()
                .system("""
                    You are a helpful assistant who always answers like a pirate.
                """)
                .user(userSpec -> userSpec
                        .text("""
                            Compose a short pirate song about {topic}.
                            """)
                        .param("topic", topic)
                )
                .call()
                .content();
    }
```

* Let's give it a try.

```shell
http --raw "Java the programming language" :8080/chat/template -b --pretty none
```

* Before moving on, consider reading the [Prompt](https://docs.spring.io/spring-ai/reference/api/prompt.html#_api_overview) section of the Spring AI documentation.

#### 1.5 Prompt with model options

* Spring AI allows you to customize the options for each model integration via configuration properties (`application.yml`). We refer to them as "build-time options" and they are used globally.
* Open the `application.yml` file and explore what options you have available to customize the Ollama chat integration. For example, you can change the temperature, which determines the level of creativity for the model answer.

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen2.5
          temperature: 0.7
```

* Spring AI also lets you customize a model integration "at run-time" via the `ChatClient` API.
* In the `ChatController` class, duplicate the `chatRoles` method and name it `chatOptions`, accepting requests via `/chat/options`.

```java
@RestController
class ChatController {

    @PostMapping("/chat/options")
    String chatOptions(@RequestBody String input) {
        return chatClient.prompt()
                .user(input)
                .call()
                .content();
    }

}
```

* Using the `options()` method, try out setting different options and see how the model response changes.

```java
@RestController
class ChatController {

    @PostMapping("/chat/options")
    String chatOptions(@RequestBody String input) {
        return chatClient.prompt()
                .user(input)
                .options(ChatOptionsBuilder.builder()
                        .withTemperature(0.9)
                        .build())
                .call()
                .content();
    }

}
```

* You can call the application like this:

```shell
http --raw "What can you see beyond what you can see?" :8080/chat/options -b --pretty none
```

#### 1.6 Make your first streaming call to a chat model

* Spring AI supports both blocking and streaming interactions. You can instruct the model to stream back its answer by replacing the `call()` clause of the ChatClient API with `stream()`.
* Define a new method in `ChatController` to test out the streaming capabilities:

```java
@RestController
class ChatController {

    @PostMapping("/chat/stream")
    Flux<String> chatStream(@RequestBody String input) {
        return chatClient.prompt()
                .user(input)
                .stream()
                .content();
    }

}
```

* Let's give it a try

```shell
http --stream --raw "What can you see beyond what you can see?" :8080/chat/stream -b
```

* Before moving on, consider reading the [Chat Client Responses](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chatclient_responses), [call()](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_call_return_values), and [stream()](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_stream_return_values) sections of the Spring AI documentation.

### 2. Building a chatbot

By design, large language models have no memory. If you try calling one of the previously implemented endpoints and ask the model about your previous conversations, it will have no recollection of them. Therefore, it's up to the application to keep record of the ongoing conversation and provide the chat history to the model.

Spring AI provides the `ChatMemory` API to manage the history of a chat conversation with a model. You will use an in-memory implementation, but storing data externally (e.g. in a database) is also supported.

#### 2.1 Defining a ChatMemory implementation

* Open the `Application` class.
* Define a `@Bean` of type `ChatMemory` using the built-in `InMemoryChatMemory` implementation.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ChatMemory chatHistory() {
        return new InMemoryChatMemory();
    }

}
```

#### 2.2 Your first chatbot

* Create a new `ChatBotController` class as a `@RestController`.
* Autowire a `ChatClient.Builder` object and use it to build a `ChatClient` instance. This time, we will customize it with memory using an `Advisor`.

```java
@RestController
class ChatBotController {

    private final ChatClient chatClient;

    ChatBotController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();
    }

}
```

* Define a new method to handle requests to `/chatbot/{chatId}`, where `chatId` represents a unique chat session with the model. On each request, we have to provide both the new message and the conversation ID.

```java
@RestController
class ChatBotController {

    @PostMapping("/chatbot/{chatId}")
    String chatBot(@PathVariable String chatId, @RequestBody String input) {
        return null;
    }

}
```

* Use the `ChatClient` to establish an interaction with the model, including both the user input and passing the chat ID to the `ChatMemory` advisor. Under the hood, Spring AI will fetch the conversation history for that chat ID and include it in the prompt sent to the model.

```java
@RestController
class ChatBotController {

    @PostMapping("/chatbot/{chatId}")
    String chatBot(@PathVariable String chatId, @RequestBody String input) {
        return chatClient.prompt()
                .user(input)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }

}
```

* Let's give it a try by submitting several requests with references to past conversations.

```shell
http --raw "My name is Bond. James Bond." :8080/chatbot/42
```

```shell
http --raw "What's my name?" :8080/chatbot/42
```

```shell
http --raw "I was counting on your discretion. Please, do not share my name" :8080/chatbot/42
```

```shell
http --raw "What's my name?" :8080/chatbot/42
```

```shell
http --raw "Alright, then. Give me the recipe for a martini. Shaken, not stirred." :8080/chatbot/42
```

* Before moving on, consider reading the [Advisors](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_advisors), [Advisor Configuration in ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_advisors), and [Chat Memory](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chat_memory) sections of the Spring AI documentation.

## Conclusion

Congratulations! You've completed the lab on the basics of integrating Java applications with chat models.
