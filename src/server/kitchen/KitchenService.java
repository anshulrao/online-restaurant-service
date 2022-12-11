package server.kitchen;

import java.rmi.Remote;
import java.rmi.RemoteException;

import server.order.OrderInstance;
import server.order.OrderStatus;

/**
 * Interface of KitchenService which is at the core of the architecture and
 * handles requests from the chef, assigns deliveries to the delivery agents
 * and processes orders from the OrderService.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public interface KitchenService extends Remote {

  /**
   * Process the new order placed.
   *
   * @param newOrder the instance of the new order
   * @return true if the order was successfully added in the queue of placed
   * orders
   * @throws RemoteException
   * @throws InterruptedException
   */
  boolean processOrder(OrderInstance newOrder) throws RemoteException,
          InterruptedException;

  /**
   * Get the count of specific item.
   *
   * @param itemName item name whose count needs to be determined
   * @return count of item from map of item names and their current counts
   * @throws RemoteException
   */
  int getItemCount(String itemName) throws RemoteException;

  /**
   * Add new items, i.e., increment their count.
   *
   * @param itemName     name of item
   * @param itemQuantity quantity of items to be added
   * @return true if items are added else false
   * @throws RemoteException
   */
  boolean addItem(String itemName, int itemQuantity) throws RemoteException;

  /**
   * Mark the order as ready for delivery.
   *
   * @param orderID order id of the order that is ready
   * @return true if added to set of ready order, false otherwise
   * @throws RemoteException
   */
  boolean orderReady(String orderID) throws RemoteException;

  /**
   * Remove an order from the placed order queue.
   *
   * @return the placed order that is removed from queue
   * @throws RemoteException
   */
  OrderInstance dequeuePlacedOrder() throws RemoteException;

  /**
   * Mark the order as delivered and complete.
   *
   * @param orderID order id of the order that is complete now
   * @return true if marked as delivered, false otherwise
   */
  boolean markDelivered(String orderID) throws RemoteException;

  /**
   * Find a ready order that is not yet assigned to any delivery agent.
   *
   * @return order id of a ready, unassigned order, otherwise null
   */
  String findReadyOrder() throws RemoteException;

  /**
   * Get the current status of order based on which set it is a part of
   * (ready or complete).
   *
   * @param orderID the order id for which status update is needed
   * @return the current status from ready or complete else null
   * @throws RemoteException
   */
  OrderStatus getOrderUpdate(String orderID) throws RemoteException;
}
