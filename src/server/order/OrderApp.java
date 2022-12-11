package server.order;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static shared.Constants.ORDER_SERVICE_NAME;

/**
 * OrderService Application.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class OrderApp {
  Logger logger;

  public OrderApp(int port) {
    this.logger = Logger.getLogger(OrderApp.class.getName());
    try {
      startServer(port);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      this.logger.log(Level.SEVERE, System.currentTimeMillis() +
              ": OrderService could not be started at port " + port +
              ". Refer: " + sw);
    }
  }

  public static void main(String[] args) {
    int port;
    // read port number from the command line
    if (args.length < 1) {
      throw new IllegalArgumentException("Enter server port number.");
    }
    try {
      port = Integer.parseInt(args[0]);  // the port
    } catch (Exception e) {
      throw new IllegalArgumentException("Port numbers should be an integer.");
    }
    new OrderApp(port);
  }

  private void startServer(int port) throws IOException, NotBoundException,
          ClassNotFoundException {
    System.setProperty("java.rmi.server.logCalls", "true");
    System.setProperty("java.rmi.server.hostname", "127.0.0.1");
    Registry registry = LocateRegistry.createRegistry(port);
    OrderServiceImpl orderService =
            new OrderServiceImpl();
    registry.rebind(ORDER_SERVICE_NAME, orderService);
    this.logger.log(Level.INFO, System.currentTimeMillis() +
            ": OrderService started at port " + port + ".");
  }
}
