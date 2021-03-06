#Introduction
The following is a general guide on the usage of flow. If you prefer a complete example, check out the code contained in the flow-samples directory. 

Flow's API follows that of an actor based system, where each actor is assigned specific functions involved in serial communication. The two main actor types are the serial "manager" and serial "operators".

The manager is a singleton actor that is instantiated once per actor system, a reference to it may be obtained with the code `IO(Serial)`. It is typically used to open serial ports (see following section).

Serial operators are created once per open serial port and serve as an intermediate between client code and native code dealing with serial data transmission and reception. They isolate the user from threading issues and enable the reactive dispatch of incoming data. A serial operator is said to be "associated" to its underlying open serial port.

The messages understood by flow's actors are all contained in the `com.github.jodersky.flow.Serial` object. They are well documented and should serve as the entry point when searching the API documentation.

#Opening a port
A serial port is opened by sending an `Open` message to the serial manager. The response varies on the outcome of opening the underlying serial port.
  1. In case of failure, the serial manager will respond with a `CommandFailed` message to the original sender. The message contains details on the reason to why the opening failed.
  2. In case of success, the sender is notified with an `Opened` message. This message is sent from an operator actor, spawned by the serial manager. It is useful to capture the sender (=operator) of this message as all further communication with the newly opened port must pass through the operator.
  
```scala
val settings = SerialSettings(
  port = "/dev/ttyXXX",
  baud = 115200
)

IO(Serial) ! Serial.Open(settings)

def receive = {
  case CommandFailed(cmd: Open, reason: AccessDeniedException) => println("you're not allowed to open that port!")
  case CommandFailed(cmd: Open, reason) => println("could not open port for some other reason: " + reason.getMessage)
  case Opened(settings, op) => {
    val operator = sender //notice that a reference to the operator is also sent as a parameter to Opened, we could have equally said operator = op
    //do stuff with the operator
  }
}
```
  
  
#Communicating

## Writing data
Writing data is as simple as sending a `Write` message containing data to an operator. The type of data is an instance of `akka.util.ByteString`:
```scala
operator ! Write(data)
```

To receive an acknowledgement when data has been sent (this means queued in the kernel buffer; no guarantees can be made on the actual transmission of the data), the sender may add an additional `ack` parameter to a `Write` message:
```scala

case class MyPacketAck(id: Int) extends Serial.Event

operator ! Write(data, MyPacketAck(0))
operator ! Write(data, MyPacketAck(1))
operator ! Write(data, MyPacketAck(2))

def receive = {
  case MyPacketAck(x) => println("Wrote packet #" + x)
}

```
In that case, the sender will be notified with `MyPacketAck(0)`, `MyPacketAck(1)` and `MyPacketAck(2)` once the data has been sent.

## Receiving data
To be notified when data is received on a serial port, a client actor must first register with its associated operator:
```scala
operator ! Register(client)
```
From that moment on, the client will receive `Received` messages when data comes in through the serial port:
```scala
def receive = {
  case Received(data) => println("got data: " + data.toString)
}
```
To unregister, simply send an `Unregister` message.

#Closing a port
A port is closed by sending a `Close` message to its operator:
```scala
operator ! Serial.Close
```
The operator will close the underlying serial port and respond with a final `Closed` message before terminating.
