package client.delivery_agent;

import java.io.IOException;
import java.rmi.Naming;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.utils.ServiceNotFoundException;
import server.kitchen.KitchenService;

import static shared.Constants.KITCHEN_HOST;
import static shared.Constants.KITCHEN_NAME;
import static shared.Constants.KITCHEN_PORT;

/**
 * Delivery agent client responsible for delivering orders.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class DeliveryAgent {
  static final String DONE = "DONE";
  private final Logger logger;
  KitchenService kitchenService;

  public DeliveryAgent() throws ServiceNotFoundException {
    this.logger = Logger.getLogger(DeliveryAgent.class.getName());
    try {
      this.kitchenService = (KitchenService) Naming.lookup("//" +
              KITCHEN_HOST + ":" + KITCHEN_PORT + "/" + KITCHEN_NAME);
    } catch (Exception e) {
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Could not locate the KitchenService. Exiting. " +
              "Refer: " + e);
      throw new ServiceNotFoundException("Could not locate the " +
              "KitchenService.");
    }
  }

  public static void main(String[] args) throws ServiceNotFoundException,
          IOException, InterruptedException {
    new DeliveryAgent().execute();
  }

  /**
   * Wait for the delivery agent to enter "done" for the order assigned
   * to the agent.
   */
  private void waitForDone() {
    Scanner scanner = new Scanner(System.in);
    do {
      String input = scanner.next();
      if (input.trim().equalsIgnoreCase(DONE)) {
        break;
      }
    } while (true);
  }

  public void execute() throws IOException, InterruptedException {
    while (true) {
      // poll for a ready order from the KitchenService
      String orderID = this.kitchenService.findReadyOrder();
      if (orderID == null) { // sleep for a second if no order found
        Thread.sleep(1000);
      } else {
        System.out.println();
        System.out.println("*****************************************" +
                "*****************************************");
        System.out.println("A new delivery has been assigned!");
        System.out.println("ORDER ID: " + orderID);
        System.out.println("*****************************************" +
                "*****************************************");
        waitForDone(); // wait until delivery agent delivers the order
        this.kitchenService.markDelivered(orderID);
        this.logger.log(Level.INFO, System.currentTimeMillis() +
                ": Order with ID = " + orderID + " has been delivered!");
      }
    }
  }
}
