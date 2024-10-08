package com.thomasvitale.ai.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    ChatResponse chat(String query) {
        return chatClient.prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()
                    //.withFilterExpression("location == 'North Pole'")
                    //.withSimilarityThreshold(0.25)
                    //.withTopK(3)
                ))
                .user(query)
                .call()
                .chatResponse();
    }

}
