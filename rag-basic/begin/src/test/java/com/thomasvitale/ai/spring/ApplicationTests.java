package com.thomasvitale.ai.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ApplicationTests {

    @Test
    void evaluateRelevancy() {
    }

}
