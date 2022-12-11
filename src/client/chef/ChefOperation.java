package client.chef;

/**
 * The two operations a Chef can do:
 * Add (increment) items to the KitchenService database as they are made.
 * Update KitchenService that order is ready for delivery
 */
public enum ChefOperation {
  ADD, READY
}
