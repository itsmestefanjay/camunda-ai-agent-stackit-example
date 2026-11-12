package io.github.itsmestefanjay.camunda.stackit.ai;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = {"classpath*:/bpmn/*.bpmn", "classpath*:/forms/*.form"})
public class SupportAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(SupportAgentApplication.class, args);
  }
}
