# PrettyDump for JSON and XML Solace messages
A useful utility that emulates SdkPerf `-md` output, echoing received Solace messages to the console, but pretty-printed for **JSON** and **XML**.
Now also with a display option for a compressed, one-line-per-message view.

**Latest release: 2023/10/23**

- [Building](#building)
- [Running](#running)
- [Command-line parameters](#command-line-parameters)
- [Subscribing options](#subscribing-options---the-5th-argument)
- [Output Indent options](#output-indent-options---the-6th-argument)
- [Error checking](#error-checking)

## Requirements

Java 8+


## Building

```
./gradlew assemble
cd build/distributions
unzip PrettyDump.zip
cd PrettyDump
```

Or just download a [Release distribution](https://github.com/SolaceLabs/pretty-dump/releases) with everything already built.


## Running

```
$ bin/PrettyDump

PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.
Subscribed to Direct topic: '#noexport/>'

Starting. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'hello/world'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             42
Binary Attachment:                      len=26
TextMessage, JSON Object:
{
    "hello": "world"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```

```
$ bin/PrettyDump demo.messaging.solace.cloud demo-vpn user pw q:q1

PrettyDump initializing...
PrettyDump connected to VPN 'demo-vpn' on broker 'demo.messaging.solace.cloud'.
Attempting to bind to queue 'q1' on the broker... success!
```

```
$ bin/PrettyDump "shortcut/>" -30

PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.
Subscribed to Direct topic: 'shortcut/>'
```




## Command-line parameters

```
$ bin/PrettyDump -h   or  --help

Usage: PrettyDump [host:port] [message-vpn] [username] [password] [topics|q:queue|b:queue] [indent]

 - If using TLS, remember "tcps://" before host
 - Default parameters will be: localhost default aaron pw "#noexport/>" 4
    - If client-username 'default' is enabled in VPN, you can use any username
 - Subscribing options, one of:
    - comma-separated list of Direct topic subscriptions
    - q:queueName to consume from queue
    - b:queueName to browse a queue
       - Can browse all messages, or specific messages by ID
 - Optional indent: integer, default = 4 spaces; specifying 0 compresses payload formatting
    - Use negative indent value (column width) for one-line topic & payload only
       - Use negative zero ("-0") for only topic, no payload
 - Shortcut mode: if the first argument contains '>', assume topics and localhost default broker
    - e.g. ./bin/PrettyDump "test/>" -30
    - If zero parameters, assume localhost default broker and subscribe to "#noexport/>"
 - Default charset is UTF-8. Override by setting: export PRETTY_DUMP_OPTS=-Dcharset=whatever
    - e.g. export PRETTY_DUMP_OPTS=-Dcharset=Shift_JIS  (or "set" on Windows)
```


## Subscribing options - the 5th argument

### Direct Subscription(s)

By default, PrettyDump will subscribe to the "catch-all" multi-level wildcard topic `>` with the `#noexport/` prefix
(see [Event Mesh considerations](#event-mesh--dmr--mnr-considerations) below).
This subscription will show most (not all!) messages going through your VPN.

Specify a single topic subscription, or a comma-separated list: e.g. `"bus_trak/door/v1/>, bus_trak/gps/v2/007/*/>"` (spaces will be stripped out).
 Remember to "quote" the whole argument if using the `>` wildcard as most shells treat this as a special character.

If you want to see ***all*** messages going through the VPN, then override the 5th argument with `">, #*/>"` and this will also display any "hidden" messages
such as those published directly to queues, point-to-point messages, request-reply, REST responses in gateway mode, SolCache communication messages, etc.

```
Subscribed to Direct topic: '>'
Subscribed to Direct topic: '#*/>'
```

**NOTE:** all subscriptions are added as *Deliver Always* (DA) so as not to interfere with any DTO / round-robin messaging.

#### Event Mesh / DMR / MNR considerations

If connecting to a mesh of brokers, take care that adding subscriptions could pull (lots of?) data from remote brokers.  This is because
subscriptions, by default, are automatically propagated (exported) to other brokers in the mesh.  To ensure you
only subscribe to data from the broker you connect to, prefix each subscription with `#noexport/`.  E.g. `"#noexport/>, #noexport/#*/>"`
or `"#noexport/bus_trak/>"`. 
See the [Solace docs](https://docs.solace.com/Messaging/No-Export.htm) for more details.


### Queue Consume

To connect to a queue and consume (e.g. the messages will be ACKed and removed), then override the 5th argument with
`q:queueName`, e.g. `q:q1`.  You will receive a warning that messages will be removed from the queue after they are received.

```
Attempting to bind to queue 'q1' on the broker... success!

Will consume/ACK all messages on queue 'q1'. Use browse 'b:' command-line option otherwise.
Are you sure? [y|yes]: 
```

### Browsing a Queue

To non-destructively view the messages on a queue, use the browse option: `b:queueName`.  You have the option of browsing
all messages, a single message based on Message ID, or a range of messages (either closed "`12345-67890`" or open-ended "`12345-`").

To find the ID of the messages on a queue, either use PubSub+ Manager, CLI, or SEMP:

![View Message IDs in PubSubPlus Manager](https://github.com/SolaceLabs/pretty-dump/blob/main/src/browse-msgs.png)


Or to just browse the first/oldest message on the queue, enter "1" or some other low number.

```
$ bin/PrettyDump aaron.messaging.solace.cloud aaron-demo-singapore me pw b:q1

PrettyDump initializing...
PrettyDump connected to VPN 'aaron-demo-singapore' on broker 'aaron.messaging.solace.cloud'.
Attempting to browse queue 'q1' on the broker... success!

Browse all messages -> press [ENTER],
 or enter specific Message ID,
 or range of IDs (e.g. "25909-26183" or "9517-"): 31737085

Starting. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'bus_trak/gps/v2/004M/01398/001.31700/0103.80721/30/OK'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           NON_PERSISTENT
Message Id:                             31737085
Replication Group Message ID:           rmid1:102ee-0b5760c9706-00000000-01e444fd
Binary Attachment:                      len=173
TextMessage, JSON Object:
{
    "psgrCap": 1,
    "heading": 228,
    "busNum": 1398,
    "latitude": 1.317,
    "rpm": 1515,
    "speed": 60,
    "routeNum": "4M",
    "longitude": 103.80721,
    "status": "OK"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
Browsing finished!
Main thread exiting.
Shutdown detected, quitting...
```


## Output Indent options - the 6th argument

### Regular: indent > 0

Valid vales are between 1 and 20.  Indent is default 4 in these examples:
```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728274
Binary Attachment:                      len=100
BytesMessage, XML:
<apps>
    <version>23</version>
    <stick>this</stick>
    <nested>
        <level>deeper</level>
    </nested>
</apps>
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728275
Binary Attachment:                      len=159
TextMessage, JSON Object:
{
    "firstName": "Aaron",
    "lastName": "Lee",
    "zipCode": "12345",
    "streetAddress": "Singapore",
    "customerId": "12345"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```

### Compact: indent = 0

```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4729017
Binary Attachment:                      len=58
BytesMessage, XML:
<apps><another>hello</another><that>this</that></apps>
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4729156
Binary Attachment:                      len=159
TextMessage, JSON Object:
{"firstName": "Aaron","lastName": "Lee","zipCode": "12345","streetAddress": "Singapore","customerId": "12345"}
```



### One-Line: indent < 0

Valid values are between -1 and -250, and specify how far right to indent the payload.  indent = -36 in this example.

```
pq-demo/stats/pq/sub-pq_3-c222      {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0,"ackd":0,"gaps":0,"flow":"FLOW_ACTIVE"}
pq/3/pub-44e7/e7-7/0/_
pq-demo/stats/pq/pub-44e7           {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2,"resendQ":0,"keys":8,"activeFlow":true}
pq/3/pub-44e7/e7-5/0/_
pq-demo/stats/pq/sub-pq_3-c222      {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0,"ackd":0,"gaps":0,"flow":"FLOW_ACTIVE"}
solace/samples/jcsmp/hello/aaron    +...4..probability...>���..from...aaron...age.......
pq/3/pub-44e7/e7-3/0/_
pq-demo/stats/pq/pub-44e7           {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2,"resendQ":0,"keys":8,"activeFlow":true}
pq/3/pub-44e7/e7-0/0/_
```

### One-Line, Topic only: indent = "-0"

Specifying -0 for the indent only prints out the topic.  I've often used ` | grep Destination` with SdkPerf `-md` output,
this does essentially the same thing.

```
bus_trak/gps/v2/013B/01058/001.37463/0103.93459/13/STOPPED
bus_trak/gps/v2/036M/01154/001.37578/0103.93151/31/OK
bus_trak/gps/v2/002A/01151/001.32270/0103.75160/30/OK
bus_trak/door/v1/033M/01434/open
bus_trak/gps/v2/033M/01434/001.42483/0103.71193/31/STOPPED
bus_trak/gps/v2/011X/01390/001.39620/0103.84082/11/OK
bus_trak/gps/v2/035B/01286/001.40101/0103.88913/12/OK
bus_trak/door/v1/005B/01464/open
bus_trak/gps/v2/006A/01291/001.29687/0103.78305/21/STOPPED
```


## Error checking

As part of the parsing for JSON and XML, it detects if the payload is malformed and prints out some text to indicate that.
This can be helpful if trying to hand-code a payload and don't get it quite right.

```
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'xml/bad/test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             9
Binary Attachment:                      len=43
BytesMessage, INVALID XML:
<bad>not having</bad><a>closing bracket/a>

^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```
