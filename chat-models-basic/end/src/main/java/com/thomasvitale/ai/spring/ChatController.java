package com.thomasvitale.ai.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
class ChatController {

    private final ChatClient chatClient;

    private final Resource systemMessageResource;

    ChatController(ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/system-message.st") Resource systemMessageResource) {
        this.chatClient = chatClientBuilder.build();
        this.systemMessageResource = systemMessageResource;
    }

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "What did Gandalf say to the Balrog?") String question) {
        return chatClient.prompt(question)
                .call()
                .content();
    }

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

    @PostMapping("/chat/external")
    String chatExternal(@RequestBody String input) {
        return chatClient.prompt()
                .system(systemMessageResource)
                .user(input)
                .call()
                .content();
    }

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

    @PostMapping("/chat/stream")
    Flux<String> chatStream(@RequestBody String input) {
        return chatClient.prompt()
                .user(input)
                .stream()
                .content();
    }

}
