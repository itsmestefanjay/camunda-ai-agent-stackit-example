package io.github.itsmestefanjay.camunda.stackit.ai;

import static io.camunda.process.test.api.CamundaAssert.assertThat;
import static io.camunda.process.test.api.CamundaAssert.assertThatProcessInstance;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.api.assertions.ProcessInstanceSelectors;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SupportAgentApplication.class)
@CamundaSpringProcessTest
@ActiveProfiles("judge")
@EnabledIfEnvironmentVariable(named = "STACKIT_AI_API_KEY", matches = ".+")
class SupportAgentJudgeIT {

  private static final String PROCESS_ID = "stackit-support-agent";
  private static final String GET_ORDER_STATUS = "Get_Order_Status";
  private static final String ASK_SUPPORT_TEAM = "Ask_Support_Team";
  private static final String REVIEW_REPLY = "Review_Reply";
  private static final String POLITE = """
      The reply is polite towards the customer: a courteous, respectful and friendly tone, \
      also if the customer is impatient. It does not blame the customer and has no sarcasm, \
      curt or dismissive wording. A greeting or thanks is welcome but not required.""";

  private final CamundaClient client;
  private final CamundaProcessTestContext context;

  @Autowired
  SupportAgentJudgeIT(CamundaClient client, CamundaProcessTestContext context) {
    this.client = client;
    this.context = context;
  }

  @Test
  void test_reply_for_shipped_order_states_carrier_and_delivery_date_as_expected() {
    // given
    supportTeamAnswers("Please answer from the order data only.");

    // when
    ProcessInstanceEvent instance = start("Where is my order 4711?");

    // then
    assertThat(instance)
        .hasActiveElements(REVIEW_REPLY)
        .hasCompletedElements(GET_ORDER_STATUS)
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", """
            The reply is in English and brief. It tells the customer that order 4711 \
            has been shipped with DHL and is expected to arrive on 14 November 2026 (any date \
            format). It does not state any other carrier, status or date.""")
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", POLITE);
  }

  @Test
  void test_reply_answers_in_language_of_customer_as_expected() {
    // given
    supportTeamAnswers("Please answer from the order data only.");

    // when
    ProcessInstanceEvent instance = start("Hallo, wo ist meine Bestellung 4713?");

    // then
    assertThat(instance)
        .hasActiveElements(REVIEW_REPLY)
        .hasCompletedElements(GET_ORDER_STATUS)
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", """
            The reply is written in German. It tells the customer that order 4713 has been \
            delivered by UPS, on 9 November 2026 if a date is given (any date format). \
            It is brief.""")
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", POLITE);
  }

  @Test
  void test_reply_for_unknown_order_invents_no_order_data_as_expected() {
    // given
    supportTeamAnswers("We have no order 9999. Ask the customer to check the order number"
        + " in the confirmation email.");

    // when
    ProcessInstanceEvent instance = start("Where is my order 9999?");

    // then
    assertThat(instance)
        .hasActiveElements(REVIEW_REPLY)
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", """
            The reply says that no order 9999 could be found and asks the customer to check \
            or provide the order number. It does not state any shipping status, carrier or \
            delivery date for order 9999.""")
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", POLITE);
  }

  @Test
  void test_agent_asks_support_team_about_discount_with_context_as_expected() {
    // given
    supportTeamAnswers("We cannot grant 20%. Offer a 10% voucher for the next order,"
        + " code SORRY10.");

    // when
    ProcessInstanceEvent instance =
        start("My order 4712 is taking forever. I want a 20% discount on it.");

    // then
    assertThat(instance)
        .hasActiveElements(REVIEW_REPLY)
        .hasCompletedElements(ASK_SUPPORT_TEAM)
        .hasLocalVariableSatisfiesJudge(ASK_SUPPORT_TEAM, "question", """
            The question asks the support team whether the customer can get a 20% discount \
            on order 4712. It gives enough context to answer without further lookups, at \
            least the order id and the customer's request.""")
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", """
            The reply offers a 10% voucher for the next order with the code SORRY10. \
            It does not promise a 20% discount. It is in English and brief.""")
        .hasLocalVariableSatisfiesJudge(REVIEW_REPLY, "reply", POLITE);
  }

  // registered before start; fires only if the agent picks the human tool
  private void supportTeamAnswers(String answer) {
    context
        .when(() -> assertThatProcessInstance(ProcessInstanceSelectors.byProcessId(PROCESS_ID))
            .hasActiveElements(ASK_SUPPORT_TEAM))
        .as("support team answers")
        .then(() -> context.completeUserTask(ASK_SUPPORT_TEAM, Map.of("answer", answer)));
  }

  private ProcessInstanceEvent start(String request) {
    return client.newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(Map.of("request", request))
        .send()
        .join();
  }
}
