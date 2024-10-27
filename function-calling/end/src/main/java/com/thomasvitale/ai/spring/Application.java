package com.thomasvitale.ai.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Description("Get the list of books written by the given author available in the library")
    Function<BookService.Author, List<BookService.Book>> booksByAuthor(BookService bookService) {
        logger.info("Calling function...");
        return bookService::getBooksByAuthor;
    }

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

}
