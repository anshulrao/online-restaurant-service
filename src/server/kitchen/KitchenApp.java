package server.kitchen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static shared.Constants.KITCHEN_NAME;
import static shared.Constants.KITCHEN_PORT;

/**
 * KitchenService Application.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class KitchenApp {
  Logger logger;

  public KitchenApp() {
    this.logger = Logger.getLogger(KitchenApp.class.getName());
    try {
      startServer();
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      this.logger.log(Level.SEVERE, System.currentTimeMillis() +
              ": Kitchen could not be started at port " + KITCHEN_PORT +
              ". Refer: " + sw);
    }
  }

  /**
   * The main method.
   */
  public static void main(String[] args) {
    new KitchenApp();
  }

  private void startServer() throws IOException {
    //  we will log calls to track which clients are connected and using
    //  the service / making calls.
    System.setProperty("java.rmi.server.logCalls", "true");
    Registry registry = LocateRegistry.createRegistry(KITCHEN_PORT);
    KitchenServiceImpl kitchenService =
            new KitchenServiceImpl();
    registry.rebind(KITCHEN_NAME, kitchenService);
    this.logger.log(Level.INFO, System.currentTimeMillis() +
            ": KitchenService started at port " + KITCHEN_PORT + ".");
  }
}
