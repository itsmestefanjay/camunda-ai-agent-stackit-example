package io.github.itsmestefanjay.camunda.stackit.ai;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.itsmestefanjay.camunda.stackit.ai.ShopData.Customer;
import io.github.itsmestefanjay.camunda.stackit.ai.ShopData.Order;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShopToolsTest {

  private final ShopTools tools = new ShopTools();

  @Test
  void test_get_customer_returns_profile_for_known_id_as_expected() {
    // given
    String customerId = "C-1001";

    // when
    Map<String, Object> result = tools.getCustomer(customerId);

    // then
    assertThat(result).as("result variables").containsOnlyKeys("toolCallResult");
    assertThat(result.get("toolCallResult")).as("customer profile")
        .isInstanceOfSatisfying(Customer.class,
            customer -> assertThat(customer.name()).as("customer name").isEqualTo("Anna Schmidt"));
  }

  @Test
  void test_get_customer_returns_not_found_message_for_unknown_id_as_expected() {
    // given
    String customerId = "C-0000";

    // when
    Map<String, Object> result = tools.getCustomer(customerId);

    // then
    assertThat(result.get("toolCallResult")).as("not found message")
        .isEqualTo("No customer found with id C-0000");
  }

  @Test
  void test_get_order_status_returns_shipping_state_for_known_id_as_expected() {
    // given
    String orderId = "4711";

    // when
    Map<String, Object> result = tools.getOrderStatus(orderId);

    // then
    assertThat(result).as("result variables").containsOnlyKeys("toolCallResult");
    assertThat(result.get("toolCallResult")).as("order status")
        .isInstanceOfSatisfying(Order.class, order -> {
          assertThat(order.status()).as("order status value").isEqualTo("shipped");
          assertThat(order.carrier()).as("order carrier").isEqualTo("DHL");
        });
  }

  @Test
  void test_get_order_status_returns_not_found_message_for_unknown_id_as_expected() {
    // given
    String orderId = "0";

    // when
    Map<String, Object> result = tools.getOrderStatus(orderId);

    // then
    assertThat(result.get("toolCallResult")).as("not found message")
        .isEqualTo("No order found with id 0");
  }
}
