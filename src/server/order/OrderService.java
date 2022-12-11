package server.order;

import java.io.IOException;
import java.rmi.Remote;
import java.util.HashMap;

/**
 * Interface of OrderService which is responsible for dealing with user
 * requests.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public interface OrderService extends Remote {

  /**
   * Get the latest menu.
   *
   * @return the menu
   * @throws IOException
   * @throws ClassNotFoundException
   */
  String getMenu() throws IOException, ClassNotFoundException;

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
  String placeOrder(String name, long contact,
                    HashMap<String, Integer> itemsNeeded) throws IOException,
          InterruptedException, ClassNotFoundException;

  /**
   * Get the status of the order from the KitchenService and report that
   * to the user.
   *
   * @param orderID order id of the order whose status is to be fetched
   * @return order status
   * @throws IOException
   * @throws ClassNotFoundException
   */
  OrderStatus getOrderStatus(String orderID) throws IOException,
          ClassNotFoundException;
}
