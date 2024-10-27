package com.thomasvitale.ai.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    CommandLineRunner chatModel(ChatModel chatModel) {
        return _ -> {
            var response = chatModel.call("What is the capital of Denmark?");
            System.out.println(response);
        };
    }

    @Bean
    CommandLineRunner chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
        var chatClient = chatClientBuilder.build();

        return _ -> {
            logger.info(">>> Chat - Basic...");
            var response = chatClient
                    .prompt("What is the capital of Denmark?")
                    .call()
                    .content();
            logger.info(response);

            logger.info(">>> Chat - Roles...");
            response = chatClient.prompt()
                    .system("You are a helpful assistant who always answers like a pirate.")
                    .user("What is the capital of Denmark?")
                    .call()
                    .content();
            logger.info(response);

            logger.info(">>> Chat - External Prompt...");
            response = chatClient.prompt()
                    .system(systemMessageResource)
                    .user("What is the capital of Denmark?")
                    .call()
                    .content();
            logger.info(response);

            logger.info(">>> Chat - Template...");
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

            logger.info(">>> Chat - Options...");
            response = chatClient
                    .prompt("What is the meaning of life? Give me a one-sentence, philosophical answer.")
                    .options(ChatOptionsBuilder.builder()
                            .withTemperature(0.9)
                            .build())
                    .call()
                    .content();
            logger.info(response);

            logger.info(">>> Chat - Stream...");
            chatClient
                    .prompt("What is the meaning of life? Give me a one-sentence, philosophical answer.")
                    .options(ChatOptionsBuilder.builder()
                            .withTemperature(0.9)
                            .build())
                    .stream()
                    .content()
                    .doOnEach(signal -> logger.info(signal.get()))
                    .blockLast();

            logger.info(">>> Chat - Memory...");
            var chatClientWithMemory = chatClientBuilder
                    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                    .build();
            var chatId = "007";

            response = chatBot(chatClientWithMemory, chatId, "My name is Bond. James Bond.");
            logger.info(response);

            response = chatBot(chatClientWithMemory, chatId, "What's my name?");
            logger.info(response);

            response = chatBot(chatClientWithMemory, chatId, "I was counting on your discretion. Please, do not share my name!");
            logger.info(response);

            logger.info("Chat completed.");
        };
    }

    private String chatBot(ChatClient chatClient, String chatId, String input) {
        return chatClient.prompt()
                .user(input)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }

}
