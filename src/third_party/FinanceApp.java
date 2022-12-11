package third_party;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class FinanceApp {
  final int PORT = 4333;

  public FinanceApp() throws RemoteException {
    System.setProperty("java.rmi.server.logCalls", "true");
    System.setProperty("java.rmi.server.hostname", "127.0.0.1");
    Registry registry = LocateRegistry.createRegistry(PORT);
    FinanceService financeService =
            new FinanceServiceImpl();
    registry.rebind("FinanceService", financeService);
  }

  public static void main(String[] args) throws RemoteException {
    new FinanceApp();
  }
}
