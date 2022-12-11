package client.chef;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.utils.ServiceNotFoundException;
import server.kitchen.KitchenService;
import server.order.OrderInstance;

import static shared.Constants.EXIT;
import static shared.Constants.ITEM_NAMES;
import static shared.Constants.KITCHEN_HOST;
import static shared.Constants.KITCHEN_NAME;
import static shared.Constants.KITCHEN_PORT;

/**
 * Chef client responsible for adding items and preparing orders.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class Chef {
  private final Logger logger;
  KitchenService kitchenService;

  public Chef() throws ServiceNotFoundException {
    // initialize logger
    this.logger = Logger.getLogger(Chef.class.getName());
    try {
      // lookup the kitchen service
      this.kitchenService = (KitchenService) Naming.lookup(
              "//" + KITCHEN_HOST + ":" + KITCHEN_PORT + "/" +
                      KITCHEN_NAME);
    } catch (Exception e) {
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Could not locate the KitchenService. Exiting. " +
              "Refer: " + e);
      throw new ServiceNotFoundException("Could not locate the " +
              "KitchenService.");
    }
  }

  /**
   * The main method.
   */
  public static void main(String[] args) throws ServiceNotFoundException,
          IOException {
    new Chef().execute();
  }

  /**
   * Read the command-line input from the user. The user can
   * enter one of the following:-
   * - ADD <item-name> <item-count>
   * - READY <order-id>
   * - EXIT
   *
   * @return the entered input from the user
   */
  private String readInputFromUser() throws IOException {
    BufferedReader stdinReader = new BufferedReader(
            new InputStreamReader(System.in));
    System.out.println(
            "1. ADD <item-name> <item-count>\n2. READY <order-id>\n3. EXIT");
    // remove any leading or trailing whitespaces
    return stdinReader.readLine().trim();
  }

  /**
   * Check if the input text entered by the user is correct and follows
   * the right format and then return the input split via space delimiter.
   *
   * @param text the entered input text from the user
   * @return the inputs in a String array
   */
  private String[] checkAndParseInput(String text) {
    String[] inputs = text.split(" ");
    ChefOperation op;
    // check if the operation entered is a valid operation
    try {
      op = ChefOperation.valueOf(inputs[0]);
    } catch (Exception e) {
      throw new IllegalArgumentException("Operation should be one of ADD or " +
              "READY.");
    }
    // check values in case of ADD operation
    if (op == ChefOperation.ADD) {
      try {
        String name = inputs[1];
        Integer.parseInt(inputs[2]);
        if (!ITEM_NAMES.contains(name)) {
          throw new IllegalArgumentException("Invalid item name!");
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("Value is not entered or is " +
                "corrupt");
      }
    }
    return inputs;
  }

  /**
   * Process the response received from server (KitchenService).
   * If true was returned then all went well, else it was a failure.
   */
  private void processResponse(boolean response) {
    if (response) {
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": Response received from server, the operation was executed " +
              "successfully :)");
    } else {
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": Response received from server, the operation failed :(");
    }
  }

  /**
   * Poll placed orders from the KitchenService's queue.
   *
   * @throws RemoteException
   * @throws InterruptedException
   */
  private void pollOrders() throws RemoteException, InterruptedException {
    while (true) {
      OrderInstance order = kitchenService.dequeuePlacedOrder();
      if (order != null) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" +
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println("A new order has been placed!");
        System.out.println(order);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" +
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      }
      Thread.sleep(1000);
    }
  }

  /**
   * The primary function executed for every client where it sends and
   * receives response from the server.
   *
   * @throws IOException
   */
  public void execute() throws IOException {
    // poll for new orders placed in a separate thread
    new Thread(() -> {
      try {
        pollOrders();
      } catch (RemoteException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).start();
    while (true) {
      String text = readInputFromUser();
      String[] inputs;
      if (text.equals(EXIT)) {
        System.exit(0);  // if chef enters EXIT then exit.
      }
      try {
        inputs = checkAndParseInput(text);
      } catch (IllegalArgumentException e) {
        this.logger.log(Level.WARNING, System.currentTimeMillis() +
                ": Incorrect input from user.");
        System.out.println("Try Again!");
        continue;
      }
      ChefOperation op = ChefOperation.valueOf(inputs[0]);
      if (op == ChefOperation.ADD) {
        String name = inputs[1];
        int count = Integer.parseInt(inputs[2]);
        boolean success = kitchenService.addItem(name, count);
        processResponse(success);
      } else {
        String orderID = inputs[1];
        boolean success = kitchenService.orderReady(orderID);
        processResponse(success);
      }
    }
  }
}
