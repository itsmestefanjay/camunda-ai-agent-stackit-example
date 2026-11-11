package io.github.itsmestefanjay.camunda.stackit.ai;

import java.util.List;
import java.util.Optional;

/** In-memory sample data of a fictional online shop. */
public final class ShopData {

  /** A shop customer. */
  public record Customer(String id, String name, String email, String tier) {}

  /** A customer order with its shipping state. */
  public record Order(String id, String customerId, String status, String carrier, String eta) {}

  private static final List<Customer> CUSTOMERS = List.of(
      new Customer("C-1001", "Anna Schmidt", "anna.schmidt@example.com", "gold"),
      new Customer("C-1002", "Ben Carter", "ben.carter@example.com", "standard"),
      new Customer("C-1003", "Chloé Martin", "chloe.martin@example.com", "standard"));

  private static final List<Order> ORDERS = List.of(
      new Order("4711", "C-1001", "shipped", "DHL", "2026-11-14"),
      new Order("4712", "C-1002", "processing", "not assigned yet", "unknown"),
      new Order("4713", "C-1003", "delivered", "UPS", "2026-11-09"));

  private ShopData() {}

  /** Customer by id. */
  public static Optional<Customer> findCustomer(String customerId) {
    return CUSTOMERS.stream().filter(customer -> customer.id().equals(customerId)).findFirst();
  }

  /** Order by id. */
  public static Optional<Order> findOrder(String orderId) {
    return ORDERS.stream().filter(order -> order.id().equals(orderId)).findFirst();
  }
}
