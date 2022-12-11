package third_party;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class FinanceServiceImpl extends UnicastRemoteObject
        implements FinanceService {

  public FinanceServiceImpl() throws RemoteException {
    super();
  }

  /**
   * This is just a placeholder and not the real implementation. This service
   * is not part of our infrastructure and is third party, so it's
   * implementation is abstracted in reality.
   *
   * @param contact the details using which the payment will be made
   * @return
   * @throws RemoteException
   */
  @Override
  public boolean makePayment(String name, long contact, double amount)
          throws RemoteException {
    return true;
  }
}
