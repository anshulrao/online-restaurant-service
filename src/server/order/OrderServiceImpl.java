package server.order;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.kitchen.KitchenService;
import third_party.FinanceService;

import static shared.Constants.FAILURE_MESSAGE;
import static shared.Constants.INVENTORY_PRICE_MAP;
import static shared.Constants.ITEM_NAMES;
import static shared.Constants.KITCHEN_HOST;
import static shared.Constants.KITCHEN_NAME;
import static shared.Constants.KITCHEN_PORT;
import static shared.Constants.ORDER_ARCHIVE_DIR;

/**
 * Implementation of the OrderService.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class OrderServiceImpl extends UnicastRemoteObject
        implements OrderService {
  final String FINANCE_HOST = "127.0.0.1";
  final int FINANCE_PORT = 4333;
  final String FINANCE_SERVICE_NAME = "FinanceService";
  private final Logger logger;
  ConcurrentHashMap<String, OrderInstance> orderData;
  KitchenService kitchenService;
  FinanceService financeService;

  public OrderServiceImpl() throws IOException, NotBoundException,
          ClassNotFoundException {
    super();
    this.logger = Logger.getLogger(OrderServiceImpl.class.getName());
    this.orderData = new ConcurrentHashMap<>();
    connectToKitchen();  // connect to KitchenService
    connectToFinance();  // connect to FinanceService
    syncData();
  }

  /**
   * Sync the data to the last most stable state.
   *
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("unchecked")
  private void syncData() throws IOException, ClassNotFoundException {
    try {
      String currentDt = new SimpleDateFormat("yyyyMMdd").format(new Date());
      File file = new File(ORDER_ARCHIVE_DIR + currentDt);
      ObjectInputStream s = new ObjectInputStream(new FileInputStream(file));
      this.orderData =
              (ConcurrentHashMap<String, OrderInstance>) s.readObject();
      this.logger.log(Level.INFO, System.currentTimeMillis() + ": " +
              "OrderService has been synced to its last stable state.");
      s.close();
    } catch (FileNotFoundException e) {
      this.logger.log(Level.INFO, System.currentTimeMillis() + ": " +
              "No previous saved state to sync to.");
    }
  }

  /**
   * Save the current state.
   *
   * @param data the data to be saved
   * @return
   */
  private boolean saveState(ConcurrentHashMap<String, OrderInstance> data) {
    try {
      String currentDt = new SimpleDateFormat("yyyyMMdd").format(new Date());
      File file = new File(ORDER_ARCHIVE_DIR + currentDt);
      ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(file));
      s.writeObject(data);
      s.close();
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": State saved.");
    } catch (Exception e) {
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": State could not be saved.");
      return false;
    }
    return true;
  }

  /**
   * Connect to the KitchenService.
   *
   * @throws RemoteException
   * @throws NotBoundException
   */
  private void connectToKitchen() throws RemoteException,
          NotBoundException {
    Registry registry = LocateRegistry.getRegistry(KITCHEN_HOST,
            KITCHEN_PORT);
    // lookup the KitchenService using registry
    this.kitchenService = (KitchenService) registry.lookup(KITCHEN_NAME);
    this.logger.log(Level.INFO, System.currentTimeMillis() + ": The " +
            "OrderService has successfully connected to KitchenService.");
  }

  /**
   * Connect to the FinanceService.
   *
   * @throws RemoteException
   * @throws NotBoundException
   */
  private void connectToFinance() throws RemoteException,
          NotBoundException {
    Registry registry = LocateRegistry.getRegistry(FINANCE_HOST, FINANCE_PORT);
    // lookup the KitchenService using registry
    this.financeService =
            (FinanceService) registry.lookup(FINANCE_SERVICE_NAME);
  }

  /**
   * Get the latest menu.
   *
   * @return the menu
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public String getMenu() throws IOException, ClassNotFoundException {
    syncData();
    StringBuilder menu = new StringBuilder("***MENU***\n\n");
    for (String item : ITEM_NAMES) {
      menu.append(item).append(" * PRICE: ").append(INVENTORY_PRICE_MAP.get(item))
              .append("$ * ").append("COUNT:").append(kitchenService
                      .getItemCount(item))
              .append("\n");
    }
    this.logger.log(Level.INFO, System.currentTimeMillis() + ": " +
            "Returning the latest menu to the user.");
    return menu.toString();
  }

  /**
   * Check if the given item is available for the given count.
   *
   * @param itemName name of item
   * @param needed   count of items needed
   * @return true if item with given count is available, else false
   * @throws RemoteException
   */
  private boolean isItemAvailable(String itemName, int needed)
          throws RemoteException {
    int available = kitchenService.getItemCount(itemName);
    return available >= needed;
  }

  /**
   * Place the order.
   *
   * @param name        name of the user placing the order
   * @param contact     contact of the user placing the order
   * @param itemsNeeded items requested by the user
   * @return order id if order was placed, else return failure message
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public synchronized String placeOrder(String name, long contact,
                                        HashMap<String, Integer> itemsNeeded)
          throws IOException, ClassNotFoundException {
    syncData();
    double amount = 0;
    for (String item : itemsNeeded.keySet()) {
      if (isItemAvailable(item, itemsNeeded.get(item))) {
        amount += INVENTORY_PRICE_MAP.get(item) * itemsNeeded.get(item);
        this.logger.log(Level.INFO, System.currentTimeMillis() + ": " +
                "Total amount for the order = " + amount);
      } else {
        this.logger.log(Level.WARNING, System.currentTimeMillis() + ": " +
                "Order could not be placed!");
        return FAILURE_MESSAGE;
      }
    }
    boolean isPaid = this.financeService.makePayment(name, contact, amount);
    if (isPaid) {
      this.logger.log(Level.INFO, System.currentTimeMillis() + ": " +
              "Payment has been processed!");
    }
    OrderInstance newOrder = new OrderInstance(amount, name, contact,
            itemsNeeded);
    orderData.put(newOrder.getOrderID(), newOrder);
    this.logger.log(Level.INFO, System.currentTimeMillis() + ": " +
            "Order has been placed!");
    saveState(orderData);
    // start processing of order in a separate thread
    new Thread(() -> {
      try {
        kitchenService.processOrder(newOrder);
      } catch (RemoteException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).start();
    return newOrder.getOrderID();
  }

  /**
   * Get the status of the order from the KitchenService and report that
   * to the user.
   *
   * @param orderID order id of the order whose status is to be fetched
   * @return order status
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public OrderStatus getOrderStatus(String orderID) throws IOException,
          ClassNotFoundException {
    syncData();
    try {
      OrderStatus orderStatus = kitchenService.getOrderUpdate(orderID);
      if (orderStatus != null) {
        OrderInstance instance =
                orderData.get(orderID);
        instance.setOrderStatus(orderStatus);
        orderData.put(orderID, instance);
        saveState(orderData);
      }
      return orderData.get(orderID).getOrderStatus();
    } catch (Exception e) {
      return OrderStatus.INVALID;
    }
  }
}