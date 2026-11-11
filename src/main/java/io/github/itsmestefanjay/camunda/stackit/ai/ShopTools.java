package io.github.itsmestefanjay.camunda.stackit.ai;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Job workers behind the agent's service-task tools; results go to {@code toolCallResult}. */
@Component
public class ShopTools {

  static final String RESULT = "toolCallResult";

  /** Customer profile, or a not-found message the agent can relay. */
  @JobWorker(type = "get-customer")
  public Map<String, Object> getCustomer(@Variable String customerId) {
    return toolCallResult(ShopData.findCustomer(customerId), "No customer found with id " + customerId);
  }

  /** Order status, or a not-found message the agent can relay. */
  @JobWorker(type = "get-order-status")
  public Map<String, Object> getOrderStatus(@Variable String orderId) {
    return toolCallResult(ShopData.findOrder(orderId), "No order found with id " + orderId);
  }

  // not found is an answer for the agent, not a job failure
  private static Map<String, Object> toolCallResult(Optional<?> found, String notFound) {
    return Map.of(RESULT, found.map(Object.class::cast).orElse(notFound));
  }
}
