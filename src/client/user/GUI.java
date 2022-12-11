package client.user;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import client.utils.Pair;
import client.utils.ServiceNotFoundException;

import static shared.Constants.ITEM_NAMES;

/**
 * The UI that the user sees and uses to place and track orders.
 *
 * @author Anshul Rao <rao.ans@northeastern.edu>
 */
public class GUI implements ActionListener {

  private final JPanel user_info_pnl = new JPanel();
  private final JPanel items_pnl = new JPanel();
  private final JPanel btns_pnl = new JPanel();
  private final User user;
  private final JTextPane menu_pane = new JTextPane();
  HashMap<String, Pair<JLabel, JTextField>> items;
  private JFrame frame;
  private JButton refresh_btn;
  private JButton submit_btn;
  private JTextField name_tf, contact_tf;

  public GUI(User user) throws IOException, NotBoundException,
          ClassNotFoundException, ServiceNotFoundException {
    super();
    this.user = user;
    this.items = new HashMap<>();
    renderGUI();
  }

  private SimpleAttributeSet getMenuStyle() {
    SimpleAttributeSet attrSet = new SimpleAttributeSet();
    StyleConstants.setAlignment(attrSet, StyleConstants.ALIGN_CENTER);
    StyleConstants.setForeground(attrSet, Color.blue);
    StyleConstants.setFontFamily(attrSet, "lucida typewriter bold");
    StyleConstants.setFontSize(attrSet, 18);
    return attrSet;
  }

  private void setMenuPane(String menu) {
    menu_pane.setPreferredSize(new Dimension(400, 200));
    menu_pane.setText(menu);
    StyledDocument doc = menu_pane.getStyledDocument();
    doc.setParagraphAttributes(0, doc.getLength(), getMenuStyle(),
            false);
  }

  private void setUserPanel() {
    user_info_pnl.setPreferredSize(new Dimension(200, 200));
    JLabel name_lbl = new JLabel("Name");
    name_lbl.setPreferredSize(new Dimension(50, 30));
    name_tf = new JTextField(10);
    name_tf.setPreferredSize(new Dimension(150, 30));
    JLabel contact_lbl = new JLabel("Contact");
    contact_lbl.setPreferredSize(new Dimension(50, 30));
    contact_tf = new JTextField(10);
    contact_tf.setPreferredSize(new Dimension(150, 30));
    user_info_pnl.add(name_lbl);
    user_info_pnl.add(name_tf);
    user_info_pnl.add(contact_lbl);
    user_info_pnl.add(contact_tf);
  }

  private void setItemsPanel() {
    items_pnl.setPreferredSize(new Dimension(200, 200));
    for (String item : ITEM_NAMES) {
      JLabel item_lbl = new JLabel(item);
      item_lbl.setPreferredSize(new Dimension(50, 25));
      JTextField item_tf = new JTextField(10);
      item_tf.setPreferredSize(new Dimension(50, 25));
      items.put(item, Pair.of(item_lbl, item_tf));
    }
    for (String item : ITEM_NAMES) {
      items_pnl.add(items.get(item).first);
      items_pnl.add(items.get(item).second);
    }
  }

  private void setButtonsPanel() {
    btns_pnl.setPreferredSize(new Dimension(400, 100));
    refresh_btn = new JButton("Refresh");
    refresh_btn.setPreferredSize(new Dimension(400, 30));
    refresh_btn.addActionListener(this);
    submit_btn = new JButton("Submit");
    submit_btn.setPreferredSize(new Dimension(400, 30));
    submit_btn.addActionListener(this);
    btns_pnl.add(refresh_btn);
    btns_pnl.add(submit_btn);
  }

  private void renderGUI() throws IOException, NotBoundException,
          ClassNotFoundException, ServiceNotFoundException {
    frame = new JFrame("Online Restaurant Service");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setPreferredSize(new Dimension(400, 500));
    frame.setMinimumSize(new Dimension(400, 500));

    // menu goes at the top
    setMenuPane(user.viewMenu());
    // user details in the left
    setUserPanel();
    // items to be ordered in the right
    setItemsPanel();
    // refresh and submit button at the bottom
    setButtonsPanel();

    // adding components to the frame.
    frame.getContentPane().add(BorderLayout.NORTH, menu_pane);
    frame.getContentPane().add(BorderLayout.WEST, user_info_pnl);
    frame.getContentPane().add(BorderLayout.EAST, items_pnl);
    frame.getContentPane().add(BorderLayout.SOUTH, btns_pnl);
    frame.pack();
    frame.setVisible(true);
  }

  void showMessage(String message) {
    JOptionPane.showMessageDialog(frame, message);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == refresh_btn) {
      // get updated menu and order status if refresh button is pressed
      try {
        setMenuPane(user.viewMenu());
        user.checkOrderStatus();
      } catch (IOException | NotBoundException | ClassNotFoundException |
               ServiceNotFoundException ex) {
        throw new RuntimeException(ex);
      }
    } else if (e.getSource() == submit_btn) {
      // try to place order if submit button is pressed
      try {
        user.orderItems(name_tf, contact_tf, items);
      } catch (InterruptedException | ClassNotFoundException | IOException |
               ServiceNotFoundException | NotBoundException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
