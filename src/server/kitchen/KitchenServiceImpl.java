package server.kitchen;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.order.OrderInstance;
import server.order.OrderStatus;

import static shared.Constants.ITEM_NAMES;

/**
 * Implementation of the KitchenService.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class KitchenServiceImpl extends UnicastRemoteObject
        implements KitchenService {
  static Set<String> ordersReady = new HashSet<>();
  static Set<String> ordersComplete = new HashSet<>();
  static Set<String> ordersAssigned = new HashSet<>();
  private final Logger logger;
  ConcurrentHashMap<String, Integer> itemCounts;
  ConcurrentLinkedDeque<OrderInstance> ordersPlaced;

  public KitchenServiceImpl() throws RemoteException {
    super();
    this.ordersPlaced = new ConcurrentLinkedDeque<>();
    this.logger = Logger.getLogger(KitchenServiceImpl.class.getName());
    initializeItemCounts();
  }

  /**
   * Initialize item counts to 0 in the beginning.
   */
  private void initializeItemCounts() {
    this.itemCounts = new ConcurrentHashMap<>();
    for (String item : ITEM_NAMES) {
      this.itemCounts.put(item, 0);
    }
    this.logger.log(Level.INFO, System.currentTimeMillis() +
            ": Initialized item counts to zero.");
  }

  /**
   * Remove the items' counts which have been ordered.
   *
   * @param order instance whose items have been ordered so have to be
   *              removed from list of items
   */
  private void removeItems(OrderInstance order) {
    HashMap<String, Integer> items = order.getItems();
    for (String item : items.keySet()) {
      int oldCount = this.itemCounts.get(item);
      this.itemCounts.put(item, oldCount - items.get(item));
    }
    this.logger.log(Level.INFO, System.currentTimeMillis() +
            ": Removed items that have been ordered.");
  }

  /**
   * Process the new order placed.
   *
   * @param newOrder the instance of the new order
   * @return true if the order was successfully added in the queue of placed
   * orders
   * @throws RemoteException
   * @throws InterruptedException
   */
  @Override
  public boolean processOrder(OrderInstance newOrder)
          throws RemoteException {
    removeItems(newOrder);
    ordersPlaced.add(newOrder);
    this.logger.log(Level.INFO, System.currentTimeMillis() +
            ": A new order with ID: " + newOrder.getOrderID() + "has been " +
            "added " +
            "to the placed orders' queue.");
    return true;
  }

  /**
   * Get the current status of order based on which set it is a part of
   * (ready or complete).
   *
   * @param orderID the order id for which status update is needed
   * @return the current status from ready or complete else null
   * @throws RemoteException
   */
  @Override
  public OrderStatus getOrderUpdate(String orderID) throws RemoteException {
    if (ordersComplete.contains(orderID)) {
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": Returning COMPLETE order status.");
      return OrderStatus.COMPLETE;
    } else if (ordersReady.contains(orderID)) {
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": Returning READY order status.");
      return OrderStatus.READY;
    }
    return null;
  }

  /**
   * Add new items, i.e., increment their count.
   *
   * @param itemName     name of item
   * @param itemQuantity quantity of items to be added
   * @return true if items are added else false
   * @throws RemoteException
   */
  @Override
  public boolean addItem(String itemName, int itemQuantity)
          throws RemoteException {
    try {
      int previousQuantity = itemCounts.getOrDefault(itemName, 0);
      itemCounts.put(itemName, previousQuantity + itemQuantity);
      this.logger.log(Level.INFO, System.currentTimeMillis() +
              ": Items count has been updated.");
    } catch (Exception e) {
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Item count could not be updated.");
      return false;
    }
    return true;
  }

  /**
   * Mark the order as ready for delivery.
   *
   * @param orderID order id of the order that is ready
   * @return true if added to set of ready order, false otherwise
   * @throws RemoteException
   */
  @Override
  public boolean orderReady(String orderID) throws RemoteException {
    try {
      ordersReady.add(orderID);
      this.logger.log(Level.WARNING, System.currentTimeMillis() +
              ": Order with ID = " + orderID + " is ready now!");
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Remove an order from the placed order queue.
   *
   * @return the placed order that is removed from queue
   * @throws RemoteException
   */
  @Override
  public OrderInstance dequeuePlacedOrder() throws RemoteException {
    if (ordersPlaced.isEmpty()) {
      return null;
    }
    return ordersPlaced.poll();
  }

  /**
   * Get the count of specific item.
   *
   * @param itemName item name whose count needs to be determined
   * @return count of item from map of item names and their current counts
   * @throws RemoteException
   */
  @Override
  public int getItemCount(String itemName) throws RemoteException {
    return itemCounts.get(itemName);
  }

  /**
   * Mark the order as delivered and complete.
   *
   * @param orderID order id of the order that is complete now
   * @return true if marked as delivered, false otherwise
   */
  @Override
  public boolean markDelivered(String orderID) {
    try {
      ordersComplete.add(orderID);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Find a ready order that is not yet assigned to any delivery agent.
   *
   * @return order id of a ready, unassigned order, otherwise null
   */
  @Override
  public synchronized String findReadyOrder() {
    for (String orderID : ordersReady) {
      if (!ordersComplete.contains(orderID) && !ordersAssigned.contains(orderID)) {
        ordersAssigned.add(orderID);
        this.logger.log(Level.WARNING, System.currentTimeMillis() +
                ": Ready order with ID = " + orderID + " is assigned now!");
        return orderID;
      }
    }
    return null;
  }
}
