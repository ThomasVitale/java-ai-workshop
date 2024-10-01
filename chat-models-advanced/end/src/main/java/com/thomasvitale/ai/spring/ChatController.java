package com.thomasvitale.ai.spring;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ChatController {

    private final ChatClient chatClient;
    private final Resource image;

    ChatController(ChatClient.Builder chatClientBuilder, @Value("classpath:tabby-cat.png") Resource image) {
        this.chatClient = chatClientBuilder.build();
        this.image = image;
    }

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

    @GetMapping("/chat/image")
    String chatImage(@RequestParam(defaultValue = "What do you see in this picture? Give a short answer") String question) {
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
