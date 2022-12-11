package client.user;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import client.utils.Pair;
import client.utils.ServiceNotFoundException;
import server.order.OrderService;
import server.order.OrderStatus;

import static client.user.UserOperation.CHECK;
import static client.user.UserOperation.ORDER;
import static client.user.UserOperation.VIEW;
import static shared.Constants.FAILURE_MESSAGE;
import static shared.Constants.ITEM_NAMES;
import static shared.Constants.ORDER_SEC_SVC_PROP_FILE;
import static shared.Constants.ORDER_SERVICE_NAME;

/**
 * User client responsible for placing orders.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class User {
  final int MAX_TRIES = 2; // maximum number of tries to connect to secondary
  private final GUI gui;
  private final Logger logger;
  private final int TIMEOUT = 5;
  OrderService orderService;
  OrderService secondaryOrderService;
  private String currentOrderID = null;
  // service

  public User(String hostname, int port) throws ServiceNotFoundException,
          IOException, NotBoundException, ClassNotFoundException {
    this.logger = Logger.getLogger(User.class.getName());
    try {
      Registry registry = LocateRegistry.getRegistry(hostname, port);
      // lookup the service using registry
      this.orderService = (OrderService) registry.lookup(ORDER_SERVICE_NAME);
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Connected to the primary service at port ");
    } catch (Exception e) {
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Could not locate the OrderService. Exiting. " +
              "Refer: " + e);
      throw new ServiceNotFoundException("Could not locate the " +
              "OrderService.");
    }
    int trial = 1;
    // try connecting to one of the secondary servers for backup.
    Set<Pair<String, Integer>> alreadyChecked = null;
    while (trial < MAX_TRIES) {
      Pair<String, Integer> secondaryInfo =
              findSecondarySecSvcInfo(alreadyChecked);
      Registry backupRegistry = LocateRegistry.getRegistry(secondaryInfo.first,
              secondaryInfo.second);
      try {
        this.secondaryOrderService =
                (OrderService) backupRegistry.lookup(ORDER_SERVICE_NAME);
        this.logger.log(Level.WARNING, System.currentTimeMillis() +
                ": Connected to the secondary service at port " +
                secondaryInfo.second);
        break;
      } catch (Exception ignored) {
        alreadyChecked.add(Pair.of(secondaryInfo.first, secondaryInfo.second));
        trial += 1;
      }
    }
    gui = new GUI(this);
  }

  /**
   * The main method.
   *
   * @throws ServiceNotFoundException
   * @throws IOException
   * @throws NotBoundException
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) throws ServiceNotFoundException,
          IOException, NotBoundException, ClassNotFoundException {
    int port;
    String hostname;
    if (args.length < 2) {
      throw new IllegalArgumentException("Please enter hostname and port " +
              "number.");
    }
    // check hostname
    try {
      InetAddress.getByName(args[0]);
      hostname = args[0];
    } catch (Exception e) {
      throw new IllegalArgumentException("Hostname is invalid.");
    }
    // check port
    try {
      port = Integer.parseInt(args[1]);
    } catch (Exception e) {
      throw new IllegalArgumentException("Port number should be an integer.");
    }
    new User(hostname, port);
  }

  /**
   * Find a secondary server IP address and port, connection to which has
   * not been tried yet.
   */
  private Pair<String, Integer> findSecondarySecSvcInfo(Set<Pair<String,
          Integer>> alreadyChecked) throws IOException {
    InputStream inStream = new FileInputStream(ORDER_SEC_SVC_PROP_FILE);
    Properties prop = new Properties();
    prop.load(inStream);
    String[] nodes = prop.getProperty("nodes").split(",");
    for (String node : nodes) {
      String[] info = node.split(":");
      String ip = info[0];  // ip address of node
      int port = Integer.parseInt(info[1]);  // port of node
      if (alreadyChecked == null) {
        return Pair.of(ip, port);
      } else if (!alreadyChecked.contains(Pair.of(ip, port))) {
        return Pair.of(ip, port);
      }
    }
    return Pair.of("", 0);
  }

  /**
   * Once a response is received from the OrderService, notify the user.
   *
   * @param response the response from the server that needs to be checked
   */
  private void processResponse(String response) {
    if (response.equals(FAILURE_MESSAGE)) {
      this.logger.log(Level.WARNING, System.currentTimeMillis() + ": Order " +
              "could not be placed.");
      gui.showMessage("Oops! Order could not be placed. :(");
    } else {
      currentOrderID = response;
      this.logger.log(Level.INFO, System.currentTimeMillis() + ": Order " +
              "has been placed.");
      gui.showMessage("Order Placed! Your Order ID: " + response);
    }
  }

  /**
   * Get the latest menu from OrderService (with the current count of items).
   *
   * @return menu
   * @throws IOException
   * @throws NotBoundException
   * @throws ClassNotFoundException
   * @throws ServiceNotFoundException
   */
  String viewMenu() throws IOException,
          NotBoundException, ClassNotFoundException, ServiceNotFoundException {
    String menu;
    menu = (String) getResponse(VIEW);
    this.logger.log(Level.INFO, System.currentTimeMillis() + ": Got the menu" +
            " from the OrderService.");
    return menu;
  }

  /**
   * Attempt placing the order that the user has submitted.
   *
   * @param name_tf    the name text field
   * @param contact_tf the contact text field
   * @param items      the count of items needed by user
   * @throws IOException
   * @throws InterruptedException
   * @throws ClassNotFoundException
   * @throws ServiceNotFoundException
   * @throws NotBoundException
   */
  void orderItems(JTextField name_tf, JTextField contact_tf,
                  HashMap<String,
                          Pair<JLabel, JTextField>> items) throws IOException,
          InterruptedException, ClassNotFoundException,
          ServiceNotFoundException, NotBoundException {
    // can place the next order only after the current order is complete
    if (currentOrderID != null) {
      gui.showMessage("You already have an active order!");
      return;
    }
    String name = name_tf.getText();
    long contact;
    try {
      contact = Long.parseLong(contact_tf.getText());
    } catch (Exception e) {
      gui.showMessage("Please enter valid contact!");
      return;
    }
    HashMap<String, Integer> itemsNeeded = new HashMap<>();
    // populate a map of items needed using the data from text fields
    try {
      for (String item : ITEM_NAMES) {
        itemsNeeded.put(
                item, Integer.parseInt(items.get(item).second.getText())
        );
      }
    } catch (Exception e) {
      gui.showMessage("Please enter valid item counts!");
      return;
    }
    String response = (String) getResponse(ORDER, name, contact, itemsNeeded);
    processResponse(response);
  }

  /**
   * Check the status of order placed by pinging the OrderService.
   *
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws ServiceNotFoundException
   * @throws NotBoundException
   */
  void checkOrderStatus() throws IOException, ClassNotFoundException,
          ServiceNotFoundException, NotBoundException {
    if (currentOrderID == null) {
      gui.showMessage("You have no active order!");
    } else {
      OrderStatus orderStatus = (OrderStatus) getResponse(CHECK,
              currentOrderID);
      gui.showMessage("Order ID: " + currentOrderID + "\nStatus: " +
              orderStatus.name());
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": Got the status of order from the OrderService = " +
              orderStatus.name());
      if (orderStatus == OrderStatus.COMPLETE) {
        currentOrderID = null;
      }
    }
  }

  /**
   * Add a layer before the remote method call to introduce a timeout of 5
   * seconds, i.e., if the server does not respond within 5 seconds, we move
   * on and do not keep waiting endlessly.
   *
   * @return the response received from server or null if it times out
   */
  private Object getResponse(UserOperation op, String name, long contact,
                             HashMap<String, Integer> items, String orderID) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Object> future = executor.submit(new Task(name, contact, items,
            orderID, op));
    Object result = null;
    try {
      result = future.get(TIMEOUT, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Server was taking too long to respond.");
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    executor.shutdownNow();
    return result;
  }

  private Object getResponse(UserOperation op) {
    return getResponse(op, "", 0L, null, "");
  }

  private Object getResponse(UserOperation op, String orderID) {
    return getResponse(op, "", 0L, null, orderID);
  }

  private Object getResponse(UserOperation op, String name, long contact,
                             HashMap<String, Integer> items) {
    return getResponse(op, name, contact, items, "");
  }

  class Task implements Callable<Object> {
    String name;
    long contact;
    HashMap<String, Integer> items;
    String orderID;
    UserOperation op;

    public Task(String name, long contact, HashMap<String, Integer> items,
                String orderID, UserOperation op) {
      this.name = name;
      this.contact = contact;
      this.items = items;
      this.orderID = orderID;
      this.op = op;
    }

    @Override
    public Object call() throws Exception {
      if (this.op == UserOperation.VIEW) {
        try {
          return orderService.getMenu();
        } catch (Exception e) {
          return secondaryOrderService.getMenu();
        }
      } else if (this.op == UserOperation.ORDER) {
        try {
          return orderService.placeOrder(name, contact, items);
        } catch (ConnectException e) {
          return secondaryOrderService.placeOrder(name, contact, items);
        }
      } else if (this.op == UserOperation.CHECK) {
        try {
          return orderService.getOrderStatus(currentOrderID);
        } catch (ConnectException e) {
          return secondaryOrderService.getOrderStatus(currentOrderID);
        }
      }
      return null;
    }
  }
}