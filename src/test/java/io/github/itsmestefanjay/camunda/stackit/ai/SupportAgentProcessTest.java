package io.github.itsmestefanjay.camunda.stackit.ai;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.annotation.JobWorker;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.AdHocSubProcess;
import io.camunda.zeebe.model.bpmn.instance.FlowElement;
import io.camunda.zeebe.model.bpmn.instance.ServiceTask;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SupportAgentProcessTest {

  private static final String RESOURCE = "/bpmn/support-agent.bpmn";

  @Test
  void test_process_model_is_valid_as_expected() throws IOException {
    // given
    BpmnModelInstance model = readModel();

    // when
    Bpmn.validateModel(model);

    // then
    ModelElementInstance process = model.getModelElementById("stackit-support-agent");
    assertThat(process).as("process element").isNotNull();
  }

  @Test
  void test_agent_contains_exactly_the_three_tools_as_expected() throws IOException {
    // given
    BpmnModelInstance model = readModel();

    // when
    List<String> toolIds = agent(model).getChildElementsByType(FlowElement.class).stream()
        .map(FlowElement::getId)
        .toList();

    // then
    assertThat(toolIds).as("tool ids")
        .containsExactlyInAnyOrder("Get_Customer", "Get_Order_Status", "Ask_Support_Team");
  }

  @Test
  void test_service_task_job_types_match_job_workers_as_expected() throws IOException {
    // given
    BpmnModelInstance model = readModel();
    Set<String> workerTypes = Arrays.stream(ShopTools.class.getDeclaredMethods())
        .filter(method -> method.isAnnotationPresent(JobWorker.class))
        .map(SupportAgentProcessTest::jobType)
        .collect(Collectors.toSet());

    // when
    List<String> taskTypes = agent(model).getChildElementsByType(ServiceTask.class).stream()
        .map(task -> task.getSingleExtensionElement(ZeebeTaskDefinition.class).getType())
        .toList();

    // then
    assertThat(taskTypes).as("service task job types").containsExactlyInAnyOrderElementsOf(workerTypes);
  }

  @Test
  void test_agent_uses_ai_agent_subprocess_job_type_as_expected() throws IOException {
    // given
    BpmnModelInstance model = readModel();

    // when
    String type = agent(model).getSingleExtensionElement(ZeebeTaskDefinition.class).getType();

    // then
    assertThat(type).as("agent task definition type")
        .isEqualTo("io.camunda.agenticai:aiagent:subprocess:2");
  }

  private static String jobType(Method method) {
    return method.getAnnotation(JobWorker.class).type();
  }

  private static AdHocSubProcess agent(BpmnModelInstance model) {
    return model.getModelElementById("Support_Agent");
  }

  private static BpmnModelInstance readModel() throws IOException {
    try (InputStream stream = SupportAgentProcessTest.class.getResourceAsStream(RESOURCE)) {
      assertThat(stream).as("bpmn resource " + RESOURCE).isNotNull();
      return Bpmn.readModelFromStream(stream);
    }
  }
}
