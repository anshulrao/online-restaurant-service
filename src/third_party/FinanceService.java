package third_party;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FinanceService extends Remote {
  boolean makePayment(String name, long contact, double amount)
          throws RemoteException;
}
