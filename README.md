# Online Restaurant Service
[Please refer to the report [here](https://github.com/anshulrao/online-restaurant-service/blob/master/Report.pdf) for a more thorough dive into the project.]

## Architecture
![Architecture Diagram](https://user-images.githubusercontent.com/31268509/217649936-759206c3-1a02-426c-9f40-42c4c998ccd7.png)

## Components

### Server

#### OrderService

* Replicated - one is primary and the others are secondary (backup). only one secondary up at a
  time.
* Connects with KitchenService
* Handles User clients

#### KitchenService

* Single instance
* Handles Chef client
* Handles DeliveryAgent clients

### Client

#### User

* Connects to OrderService
* Multiple users

#### Chef

* Connects to KitchenService
* Only one instance
* Add (increment) items to the KitchenService database as they are made.
* Update KitchenService that order is ready for delivery

#### Delivery Agent

* Connects to KitchenService
* Multiple delivery agents
* Polls for orders that are ready and gets assigned to one
* Sends response "DONE" (case-insensitive) to the KitchenService once order is delivered

## Execution

NOTE: Start the `KitchenService` and `FinanceService` before `OrderService` and make sure you are inside
`%src` before executing the below commands.
Also, add the secondary server endpoints to `secondary-order-service.properties` so that `User` clients
are able to detect them.

* `> javac */*/*java`
* `> rmiregistry`
* `> java third_party.FinanceApp`
* `> java server.kitchen.KitchenApp`
* `> java server.order.OrderApp <port>`
* `> java client.chef.Chef`
* `> java client.delivery_agent.DeliveryAgent`
* `> java client.user.User <hostname> <port>`
