# Integrating Java with Chat Models - Advanced

## Introduction

Welcome to this hands-on lab on more advanced techniques for integrating Java applications with Large Language Models (LLMs). In this lab, you'll learn how to instruct a chat model to return structured output (such as JSON), define multimodal prompts, and design a service to extract structured data from a free-form text.

## Prerequisites

Before starting the lab, please ensure you have the following installed:

* Java 23 (install using [SDKMAN!](https://sdkman.io/jdks) recommended)
* IDE for Java (Visual Studio Code or IntelliJ IDEA recommended)
* [Ollama](https://ollama.com/) for running large language models locally.

## Learning Goals

By the end of this lab, you will be able to:

* Implement structured output generation using different data types (Beans, Maps, Lists)
* Utilize Spring AI's entity conversion capabilities for parsing model outputs
* Create multimodal prompts incorporating both text and images
* Implement a structured data extraction service from free-form text
* Fine-tune model behavior using runtime options like temperature settings.

## Exercises

Use the project in the `begin` folder as a starting point. If you get stuck, refer to the `end` folder for the final
state of the project.

The project is configured as a standalone CLI application. You can run the application as follows.

```shell
./gradlew bootRun
```

### 1. Structured Outputs

Some chat models support generating content compliant with certain data types, such as following JSON or XML schemas.

Spring AI makes it possible to declare which type we would like the output to be compliant with.
Under the hood, it augments our prompt with instructions for the model on how to structure the output based
on our type declaration.

There are two phases:

1. Spring AI enhances the prompt with formatting instructions that the model will follow to structure the output.
2. Spring AI will parse the text returned by the model and deserialize it into a Java object of the desired data type.

Spring AI provides the StructuredOutputConverter API to define the desired output format and parse the model's response
into a Java object. There are built-in converters for common data types like beans, maps, and lists, but you can also
implement your own.

#### 1.1 Bootstrapping the Ollama integration

Spring AI supports instructing a chat model to structure the output based on a Java type definition
and parse it into a Java object (bean).

The project is already equipped with the Spring AI Ollama integration. Inspect the `build.gradle` file to check the
configured dependencies. Then, open the `application.yml` file and configure the chat model to use (`llama3.2`).
You can also configure Spring AI to download the model automatically at startup time if it's not available locally.

```yaml
spring:
  ai:
    ollama:
      init:
        pull-model-strategy: when_missing
        chat:
          additional-models:
            - llava-phi3
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

#### 1.2 Defining the data types for input and structured output

In the `Application` class, define two records: `MusicQuestion` and `ArtistInfo`. The `MusicQuestion` record will
represent the user input whereas the `ArtistInfo` record will represent the structured output from the chat model.

```java
@SpringBootApplication
public class Application {
}

record MusicQuestion(String genre, String instrument){}
record ArtistInfo(String name, String band) {}
```

#### 1.3 Structured Output: Beans

Let's use the previously initialized `ChatClient` to call the chat model and get a structured output in the form of a Java bean.
The user prompt will be composed of a template with placeholders for the input parameters coming from a `MusicQuestion` object.

Notice the usage of the `entity()` clause instead of `content()`, instructing Spring AI to configure the chat interaction
for getting structured output.

```java
@Bean
CommandLineRunner chatClient(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var question = new MusicQuestion("rock", "piano");
        var response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("""
                                Tell me the name of one musician playing the {instrument} in a {genre} band.
                                """)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .call()
                .entity(ArtistInfo.class);
        logger.info(response.toString());
    };
}
```

When working with structured output, we want the model to be as precise as possible. We can set the temperature to 0.0 to reduce format errors and hallucinations.

```java
@Bean
CommandLineRunner chatClient(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var question = new MusicQuestion("rock", "piano");
        var response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("""
                                Tell me the name of one musician playing the {instrument} in a {genre} band.
                                """)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .options(ChatOptionsBuilder.builder()
                        .withTemperature(0.0)
                        .build())
                .call()
                .entity(ArtistInfo.class);
        logger.info(response.toString());
    };
}
```

This approach works across different models. However, some model services provide dedicated support for generating
JSON-compliant answers. In those cases, you might want to rely on that to get a better result.
Ollama, OpenAI, and Mistral AI provide dedicated support for JSON. Since this is not a generic capability,
the generic `ChatOptions` you used in previous exercises will not let you enable the JSON mode in Ollama
(but it might in the future, check out this [feature request](https://github.com/spring-projects/spring-ai/issues/1271) on the Spring AI GitHub).
Instead, you'll need to use the specialized `OllamaOptions`.

```java
@Bean
CommandLineRunner chatClient(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var question = new MusicQuestion("rock", "piano");
        var response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("""
                                Tell me the name of one musician playing the {instrument} in a {genre} band.
                                """)
                        .param("genre", question.genre())
                        .param("instrument", question.instrument())
                )
                .options(OllamaOptions.builder()
                        .withFormat("json")
                        .withTemperature(0.0)
                        .build())
                .call()
                .entity(ArtistInfo.class);
        logger.info(response.toString());
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

How's the result? Is it the expected answer? If not, try refining the prompt to achieve the desired result.
For example, you can use the few-shots technique and include some examples in the prompt for the model
to understand better how it's supposed to generate an answer.

Try removing the Ollama-specific option for handling JSON content. Is the result still good?

You can try out different prompting techniques and compare the results. Check the
[Prompt Engineering Guide](https://www.promptingguide.ai/techniques) for some inspiration.

Before moving on, consider reading the [Structured Output API](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_structured_output_api),
[Bean Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_bean_output_converter),
and [Built-in JSON mode](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_built_in_json_mode) sections of the Spring AI documentation.

You might also be interested in reading the [Map Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_map_output_converter)
and [List Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html#_list_output_converter) sections of the Spring AI documentation.

### 2. Multimodality

Some chat models support interactions based not only on text, but also on other type of media content, such as images or audio.

Spring AI supports multimodality and provides a convenient way to include additional media content to a chat prompt.

#### 2.1 Defining an image

The project already includes a picture in `src/main/resources/tabby-cat.png`. That's what we'll use for this exercise.

Load the image file as a `Resource` inside the `CommandLineRunner` bean, following a similar approach
as you used for including external prompts.

```java
@Bean
CommandLineRunner chat(
        ChatClient.Builder chatClientBuilder,
        @Value("classpath:tabby-cat.png") Resource image
) {
}
```

#### 2.2 Your first chat about images

Let's use the previously initialized `ChatClient` to call the chat model and get a multimodal response.
As part of the user message, you can include both the textual input from the user and the image.

You also need to configure a model with support for multimodality, such as `llava-phi3`.
The default model used in the project, `llama3.2`, does not support multimodality,
but it's easy to switch to a model that does directly where you call it.

```java
@Bean
CommandLineRunner chat(
        ChatClient.Builder chatClientBuilder,
        @Value("classpath:tabby-cat.png") Resource image
) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var response = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("What do you see in this picture? Give a short answer")
                        .media(MimeTypeUtils.IMAGE_PNG, image)
                )
                .options(ChatOptionsBuilder.builder()
                        .withModel("llava-phi3")
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

You can ask any question about the picture. For example, you can ask if there's an animal in the picture.
Feel free to experiment with different prompts and see how the model responds.
You can also try different images and see how the model reacts to them.

Before moving on, consider reading the [Multimodality](https://docs.spring.io/spring-ai/reference/api/multimodality.html)
section of the Spring AI documentation.

### 3. Structured Data Extraction

One interesting use case for large language models is the capability of extracting information from unstructured data.
In this exercise, we'll see how to extract details about a patient and save them as a structured patient journal object.

#### 3.1 Defining the data types for structured data extraction

In the `Application` class, define a `PatientJournal` record with fields for the patient's name,
observations, and diagnosis. We will ask the model to extract structured data from free text
and return a `PatientJournal` object.

```java
@SpringBootApplication
public class Application {
}

record PatientJournal(String fullName, List<Observation> observations, Diagnosis diagnosis) {
    record Observation(Type type, String content) {}
    record Diagnosis(String content) {}

    enum Type {
        BODY_WEIGHT,
        TEMPERATURE,
        VITAL_SIGNS,
        OTHER
    }
}
```

#### 3.2 Extracting data

Let's use the previously initialized `ChatClient` to call the chat model and get structured output in the form of a Java bean.
As part of the user message, instruct the model about the structured data extraction task. Make sure you instruct it
to ignore any field the model cannot extract information for, instead of making stuff up.

We are asking the model to extract structured data from free text, and we don't want it to get creative and make stuff up.
For that reason, it's also important to set the temperature to 0.0. This helps reduce hallucinations.

```java
@Bean
CommandLineRunner chat(ChatClient.Builder chatClientBuilder) {
    var chatClient = chatClientBuilder.build();

    return _ -> {
        var unstructuredData = """
                I'm visiting Jon Snow. The blood pressure looks fine: 120/80.
                The temperature is 36 degrees. The diagnosis is: he knows nothing.
                """;
        var structuredData = chatClient.prompt()
                .user(userSpec -> userSpec.text("""
                        Extract structured data from the provided text.
                        If you do not know the value of a field asked to extract,
                        do not include any value for the field in the result.
                        
                        ---------------------
                        TEXT:
                        {text}
                        ---------------------
                        """)
                        .param("text", unstructuredData))
                .options(ChatOptionsBuilder.builder()
                        .withTemperature(0.0)
                        .build())
                .call()
                .entity(PatientJournal.class);
        logger.info(structuredData.toString());
    };
}
```

Run the application and check the console output for the model's response.

```shell
./gradlew bootRun
```

Experiment with different inputs and prompt variations to see if you can always get a good result 
or if the model needs some additional help to ensure a stable outcome.

Try also extending the `PatientJournal` record with more fields and pass more data to the model.
How good is the result in that case?

### 4. Text Classification

If you're interested in the text classification use case, check out this [article](https://www.thomasvitale.com/text-classification-with-spring-ai/)
showing how to implement it using Spring AI and Ollama.

## Conclusion

Congratulations! You've completed the lab on more advanced techniques for integrating Java applications with chat models!
