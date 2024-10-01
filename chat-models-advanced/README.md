# Integrating Java with Chat Models - Advanced

## Introduction

Welcome to this hands-on lab on more advanced techniques for integrating Java applications with Large Language Models (LLMs). In this lab, you'll learn how to instruct a chat model to return structured output (such as JSON), define multimodal prompts, and design a service to extract structured data from a free-form text.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 22 (install using [SDKMAN!](https://sdkman.io/jdks) recommended)
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [httpie](https://httpie.io/cli) for making API calls
* [Ollama](https://ollama.com/) for running large language models locally

Ensure you have either the `qwen2.5` or the `mistral` chat model available in Ollama:

```shell
ollama pull qwen2.5
ollama pull mistral
```

You'll also need a chat model with vision capabilities, such as `llava` or `moondream`.

```shell
ollama pull llava
ollama pull moondream
```

## Learning Goals

By the end of this lab, you will be able to:

* Implement structured output generation using different data types (Beans, Maps, Lists)
* Utilize Spring AI's entity conversion capabilities for parsing model outputs
* Create multimodal prompts incorporating both text and images
* Implement a structured data extraction service from free-form text
* Fine-tune model behavior using runtime options like temperature settings.

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final state of the project.

You can start the application as follows. It will reload automatically whenever you make changes. On Visual Studio Code, it works out of the box. If you're using IntelliJ IDEA, follow the [documentation](https://www.jetbrains.com/help/idea/spring-boot.html#enable-auto-restart) to configure the IDEA to support the Spring Boot DevTools.

```shell
./gradlew bootTestRun
```

### 1. Structured Outputs

Some chat models support generating content compliant with certain data types, such as following JSON or XML schemas.

Spring AI makes it possible to declare which type we would like the output to be compliant with. Under the hood, it augments our prompt with instructions for the model on how to structure the output based on our type declaration.

There are two phases:

1. Spring AI enhances the prompt with formatting instructions that the model will follow to structure the output.
2. Spring AI will parse the text returned by the model and deserialize it into a Java object of the desired data type.

#### 1.1 Structured Output: Beans

* Spring AI supports instructing a chat model to structure the output based on a Java type definition and parse it into a Java object (bean).
* The project is already equipped with the Spring AI Ollama integration. Open the `application.yml` file and configure the chat model to use (`qwen2.5` or `mistral`, depending on which model you downloaded).

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen2.5
          temperature: 0.7
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

* The project already contains the object used for the user input (`MusicQuestion`) and the object desired for obtaining structured output from the chat model (`ArtistInfo`). Inspect them.
* In the `ChatController` class, define a `chatBean()` method accepting a `MusicQuestion` in the request body and returning an `ArtistInfo` object. Make it handle POST requests to the `/chat/bean` endpoint.

```java
@RestController
class ChatController {

    @PostMapping("/chat/bean")
    ArtistInfo chatBean(@RequestBody MusicQuestion question) {
    }

}
```

* Use the previously configured `ChatClient`, pass the input parameters in the prompt template, and call the chat model to get an answer. The user prompt template will be: "Tell me name and band of one musician famous for playing in a {genre} band. Consider only the musicians that play the {instrument} in that band.". Notice the usage of the `entity()` clause instead of `content()`, instructing Spring AI to configure the chat interaction for getting structured output.

```java
@RestController
class ChatController {

    @PostMapping("/chat/bean")
    ArtistInfo chatBean(@RequestBody MusicQuestion question) {
        var userPromptTemplate = """
                Tell me name and band of one musician famous for playing in a {genre} band.
                Consider only the musicians that play the {instrument} in that band.
                """;

        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .call()
                .entity(ArtistInfo.class);
    }

}
```

* This approach works across different models. However, some model services provide dedicated support for generating JSON-compliant answers. In those cases, you might want to rely on that to get a better result. Ollama, OpenAI, and Mistral AI provide dedicated support for JSON. Since this is not a generic capability, the generic `ChatOptions` you used in previous exercises will not let you enable the JSON mode in Ollama. Instead, you'll need to use the specialized `OllamaOptions`.

```java
@RestController
class ChatController {

    @PostMapping("/chat/bean")
    ArtistInfo chatBean(@RequestBody MusicQuestion question) {
        var userPromptTemplate = """
                Tell me name and band of one musician famous for playing in a {genre} band.
                Consider only the musicians that play the {instrument} in that band.
                """;

        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .options(OllamaOptions.builder()
                        .withFormat("json")
                        .build())
                .call()
                .entity(ArtistInfo.class);
    }

}
```

* Test the newly created endpoint

```shell
http :8080/chat/bean genre="rock" instrument="piano" -b
```

* How's the result? Is it the expected answer? If not, try refining the prompt to achieve the desired result. For example, you can use the few-shots technique and include some examples in the prompt for the model to understand better how it's supposed to generate an answer.

* Try removing the Ollama-specific option for handling JSON content. Is the result still good?

* You can try out different prompting techniques and compare the results. Check the [Prompt Engineering Guide](https://www.promptingguide.ai/techniques) for some inspiration.

* Before moving on, consider reading the [Structured Output API](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_structured_output_api), [Bean Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_bean_output_converter), and [Built-in JSON mode](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_built_in_json_mode) sections of the Spring AI documentation.

#### 1.2 Structured Output: Maps

* Spring AI supports instructing a chat model to structure the output as a map and parse it into a generic `Map` Java object.

* In the `ChatController` class, define a `chatMap()` method accepting a `MusicQuestion` in the request body and returning a `Map` object. Make it handle POST requests to the `/chat/map` endpoint.

```java
@RestController
class ChatController {

    @PostMapping("/chat/map")
    Map<String,Object> chatMap(@RequestBody MusicQuestion question) {
    }

}
```

* Use the previously configured `ChatClient`, pass the input parameters in the prompt template, and call the chat model to get an answer. The user prompt template will be: "Tell me name and band of one musician famous for playing in a {genre} band. Consider only the musicians that play the {instrument} in that band.". Notice the usage of the `entity()` clause instead of `content()`, instructing Spring AI to configure the chat interaction for getting structured output.

```java
@RestController
class ChatController {

    @PostMapping("/chat/map")
    Map<String,Object> chatMap(@RequestBody MusicQuestion question) {
        var userPromptTemplate = """
                Tell me the names of three musicians famous for playing in a {genre} band.
                Consider only the musicians that play the {instrument} in that band.
                """;

        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .call()
                .entity(new MapOutputConverter());
    }

}
```

* Test the newly created endpoint

```shell
http :8080/chat/map genre="rock" instrument="piano" -b
```

* How does this result compare to the previous example? Is it still satisfactory? If not, try refining the prompt to achieve the desired result. For example, you can use the few-shots technique and include some examples in the prompt for the model to understand better how it's supposed to generate an answer.

* You can try out different prompting techniques and compare the results. Check the [Prompt Engineering Guide](https://www.promptingguide.ai/techniques) for some inspiration.

* Before moving on, consider reading the [Map Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_map_output_converter) section of the Spring AI documentation.

#### 1.3 Structured Output: Lists

* Spring AI supports instructing a chat model to structure the output as a list and parse it into a generic `List` Java object.

* In the `ChatController` class, define a `chatList()` method accepting a `MusicQuestion` in the request body and returning a `List` object. Make it handle POST requests to the `/chat/list` endpoint.

```java
@RestController
class ChatController {

    @PostMapping("/chat/list")
    List<String> chatList(MusicQuestion question) {
    }

}
```

* Use the previously configured `ChatClient`, pass the input parameters in the prompt template, and call the chat model to get an answer. The user prompt template will be: "Tell me name and band of one musician famous for playing in a {genre} band. Consider only the musicians that play the {instrument} in that band.". Notice the usage of the `entity()` clause instead of `content()`, instructing Spring AI to configure the chat interaction for getting structured output.

```java
@RestController
class ChatController {

    @PostMapping("/chat/list")
    List<String> chatList(MusicQuestion question) {
        var userPromptTemplate = """
                Tell me the names of three musicians famous for playing in a {genre} band.
                Consider only the musicians that play the {instrument} in that band.
                """;

        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(userPromptTemplate)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .call()
                .entity(new ListOutputConverter(new DefaultConversionService()));
    }

}
```

* Test the newly created endpoint

```shell
http :8080/chat/list genre="rock" instrument="piano" -b
```

* How's the result? Is the model answering with additional text besides the list? Try refining the prompt to achieve the desired result. For example, you can use the few-shots technique and include some examples in the prompt for the model to understand better how it's supposed to generate an answer.

* You can try out different prompting techniques and compare the results. Check the [Prompt Engineering Guide](https://www.promptingguide.ai/techniques) for some inspiration.

* Before moving on, consider reading the [List Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_list_output_converter) section of the Spring AI documentation.

### 2. Multimodality

Some chat models support interactions based not only on text, but also on other type of media content, such as images or audio.

Spring AI supports multimodality and provides a convenient way to include additional media content to a chat prompt.

#### 2.1 Defining an image

* The project already includes a picture in `src/main/resources/tabby-cat.png`. That's what we'll use for this exercise.
* In the `ChatController` class, inject the picture as a `Resource` following a similar approach as you used for including external prompts.

```java
@RestController
class ChatController {

    private final ChatClient chatClient;
    private final Resource image;

    ChatController(ChatClient.Builder chatClientBuilder, @Value("classpath:tabby-cat.png") Resource image) {
        this.chatClient = chatClientBuilder.build();
        this.image = image;
    }

}
```

#### 2.2 Your first chat about images

* In the `ChatController` class, define a `chatImage()` method accepting a `String` in the request parameter and returning a `String` object. Make it handle GET requests to the `/chat/image` endpoint.

```java
@RestController
class ChatController {

    @GetMapping("/chat/image")
    String chatImage(@RequestParam(defaultValue = "What do you see in this picture?") String question) {
    }

}
```

* Use the previously configured `ChatClient` to call the chat model. As part of the user message, you can include both the textual input from the user and the image.
* You also need to configure a model with support for multimodality, such as `llava` or `moondream`, depending on the model you downloaded earlier. 

```java
@RestController
class ChatController {

    @GetMapping("/chat/image")
    String chatImage(@RequestParam(defaultValue = "What do you see in this picture?") String question) {
        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(question)
                        .media(MimeTypeUtils.IMAGE_PNG, image)
                )
                .options(ChatOptionsBuilder.builder()
                        .withModel("moondream")
                        .build())
                .call()
                .content();
    }

}
```

* Test the newly created endpoint

```shell
http :8080/chat/image -b
```

* You can ask any question about the picture

```shell
http :8080/chat/image question=="Is there an animal in the picture?" -b
```

* Before moving on, consider reading the [Multimodality](https://docs.spring.io/spring-ai/reference/api/multimodality.html) section of the Spring AI documentation.

### 3. Structured Data Extraction

One interesting use case for large language models is the capability of extracting information from unstructured data. In this exercise, we'll see how to extract details about a patient and save them as a structured patient journal object.

#### 3.1 Defining the context for data extraction

* Create a new `StructuredDataExtractionController` class, autowire a `ChatClient.Builder` object in the constructor, and use it to initialize a `ChatClient` object.

```java
@RestController
class StructuredDataExtractionController {

    private final ChatClient chatClient;

    StructuredDataExtractionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

}
```

* We will ask the model to extract structured data from free text, and we don't want it to get creative and make stuff up. For that reason, it's important to set the temperature to 0.0. This helps reduce hallucinations.

```java
@RestController
class StructuredDataExtractionController {

    private final ChatClient chatClient;

    StructuredDataExtractionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultOptions(ChatOptionsBuilder.builder()
                        .withTemperature(0.0)
                        .build())
                .build();
    }

}
```

#### 3.2 Extracting data

* The project already contains the object we want to use to extract data to (`PatientJournal`).
* In the `StructuredDataExtractionController` class, define an `extract()` method accepting a `String` in the request body and returning a `PatientJournal` object. Make it handle POST requests to the `/extract` endpoint.

```java
@RestController
class StructuredDataExtractionController {

    @PostMapping("/extract")
    PatientJournal extract(@RequestBody String input) {
    }

}
```

* Use the previously configured `ChatClient` to call the chat model. As part of the user message, instruct the model about the structured data extraction task. Make sure you instruct it to ignore any field the model cannot extract information for, instead of making stuff up.

```java
@PostMapping("/extract")
PatientJournal extract(@RequestBody String input) {
    return chatClient.prompt()
            .user(userSpec -> userSpec.text("""
                Extract structured data from the provided text.
                If you do not know the value of a field asked to extract,
                do not include any value for the field in the result.
    
                ---------------------
                TEXT:
                {text}
                ---------------------
                """)
                .param("text", input))
            .call()
            .entity(PatientJournal.class);
}
```

* Test the newly created endpoint

```shell
http --raw "I'm visiting Jon Snow. The blood pressure looks fine: 120/80. The temperature is 36 degrees. The diagnosis is: he knows nothing." :8080/extract
```

* Experiment with different inputs and prompt variations to see if you can always get a good result or if the model needs some additional help to ensure a stable outcome.
* Try extending the `PatientJournal` record with more fields and pass more data to the model. How good is the result in that case?

### 4. Text Classification

* If you're interested in the text classification use case, check out this [article](https://www.thomasvitale.com/text-classification-with-spring-ai/) showing how to implement it using Spring AI and Ollama.

## Conclusion

Congratulations! You've completed the lab on more advanced techniques for integrating Java applications with chat models!
