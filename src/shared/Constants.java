package shared;

import java.util.List;
import java.util.Map;

public class Constants {
  public static final Map<String, Integer> INVENTORY_PRICE_MAP =
          initInventoryPriceMap();
  public static final List<String> ITEM_NAMES =
          List.of("Burger", "Fries", "Pasta", "Pizza");
  public static final String KITCHEN_HOST = "localhost";
  public static final int KITCHEN_PORT = 1234;
  public static final String KITCHEN_NAME = "KitchenService";
  public static final String ORDER_SERVICE_NAME = "OrderService";
  public static final String FAILURE_MESSAGE = "FAILED";
  public static final String EXIT = "EXIT";
  public static final String ORDER_SEC_SVC_PROP_FILE = "/Users/anshulrao" +
          "/IdeaProjects/FinalProject/secondary-order-service.properties";
  public static final String ORDER_ARCHIVE_DIR = "/Users/anshulrao" +
          "/IdeaProjects/FinalProject/data/";

  private static Map<String, Integer> initInventoryPriceMap() {
    return Map.of(
            "Burger", 10,
            "Fries", 5,
            "Pasta", 30,
            "Pizza", 25
    );
  }
}