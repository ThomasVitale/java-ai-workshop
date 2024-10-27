package com.thomasvitale.ai.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner chatClient(ChatClient.Builder chatClientBuilder, @Value("classpath:tabby-cat.png") Resource image) {
        var chatClient = chatClientBuilder.build();

        return _ -> {
            logger.info(">>> Structured Output...");
            var question = new MusicQuestion("rock", "piano");
            var structuredResponse = chatClient.prompt()
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
            logger.info(structuredResponse.toString());

            logger.info(">>> Multimodality...");
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

            logger.info(">>> Structured Data Extraction...");
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

            logger.info("Chat completed.");
        };
    }

}

record MusicQuestion(String genre, String instrument) {}
record ArtistInfo(String name, String band) {}

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
