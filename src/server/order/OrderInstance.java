package server.order;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An instance of order that has the ID, status, user's name and contact,
 * amount, items.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class OrderInstance implements Serializable {
  static AtomicInteger counter = new AtomicInteger();
  String orderID;
  OrderStatus orderStatus;
  String name;
  double amount;
  HashMap<String, Integer> items;
  long userContact;

  public OrderInstance(double amount, String name, long userContact,
                       HashMap<String,
                               Integer> items, OrderStatus status) {
    this.orderID = "O-" + counter.incrementAndGet() + "" +
            System.currentTimeMillis();
    this.orderStatus = status;
    this.name = name;
    this.userContact = userContact;
    this.amount = amount;
    this.items = items;
  }

  public OrderInstance(double amount, String name, long userContact,
                       HashMap<String,
                               Integer> items) {
    this(amount, name, userContact, items, OrderStatus.PLACED);
  }

  public String getOrderID() {
    return orderID;
  }

  public void setOrderID(String orderID) {
    this.orderID = orderID;
  }

  public OrderStatus getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(OrderStatus orderStatus) {
    this.orderStatus = orderStatus;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public long getUserContact() {
    return userContact;
  }

  public HashMap<String, Integer> getItems() {
    return items;
  }

  @Override
  public String toString() {
    return "OrderInstance{" +
            "orderID='" + orderID + '\'' +
            ", items=" + items +
            '}';
  }
}
