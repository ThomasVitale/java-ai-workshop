package com.thomasvitale.ai.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.RelevancyEvaluator;
import org.springframework.ai.model.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ApplicationTests {

    @Autowired
    ChatService chatService;

    @Autowired
    ChatClient.Builder chatClientBuilder;

    @Test
    void evaluateRelevancy() {
        String question = "What is Iorek's biggest dream?";
        ChatResponse response = chatService.chat(question);
        String answer = response.getResult().getOutput().getContent();
        List<Content> contextDocuments = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);

        RelevancyEvaluator relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, contextDocuments, answer);
        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);
    
        System.out.println(evaluationResponse);

        assertThat(evaluationResponse.isPass()).isTrue();
    }
    
}
