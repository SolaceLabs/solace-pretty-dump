![PrettyDump Banner](src/prettydump5.png)
# Pretty-print for dumped Solace messages

A useful utility that emulates SdkPerf `-md` "message dump" output, echoing received Solace messages to the console, but colour pretty-printed for **JSON**, **XML**, **Protobuf**, and Solace **SDT** Maps and Streams. (And binary too).
Also with a display option for a minimal one-line-per-message view.  Supports Direct topic subscriptions, Queue consume, Queue browsing, and temporary Queue w/subs.  Now with Selector support!


- [Building](#building)
- [Running](#running)
- [Command-line parameters](#command-line-parameters)
- Subscribing options: [Direct topic subscriptions](#direct-subscriptions), [Queue consume](#queue-consume), [Browsing a queue](#browsing-a-queue), [TempQ with subs](#temporary-queue-with-subscriptions)
- [Output Indent options](#output-indent-options---the-6th-argument) ([One-line Mode](#one-line-indent--0))
- [One-line Mode: Runtime options](#one-line-mode-runtime-options)
- [Charset Encoding](#charset-encoding)
- [Error Checking](#error-checking)
- [Protobuf Stuff](#protobuf-stuff) & Distributed Trace
- [SdkPerf Wrap Mode](#sdkperf-wrap-mode)



## Requirements

- Java 8+  or  Docker
- Network access to a Solace broker (software, cloud, appliance)



## Building

```
./gradlew assemble
cd build/distributions
unzip prettydump.zip
cd prettydump/bin
```

Or just download a [Release distribution](https://github.com/SolaceLabs/pretty-dump/releases) with everything already built.

For Docker container usage, read the comments in [the Dockerfile](Dockerfile).


## Running

#### No args, default broker options
```
$ prettydump

PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.
Subscribed to Direct topic: '#noexport/>'

Starting. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message #1 ^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'hello/world'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Binary Attachment:                      len=26
SDT TextMessage, UTF-8 charset, JSON Object:
{
    "hello": "world" }

^^^^^^^^^^^^^^^^^^ End Message #1 ^^^^^^^^^^^^^^^^^^^^^^^^^^
```

#### Consume from a Solace Cloud queue
```
$ prettydump demo.messaging.solace.cloud demo-vpn user pw q:q1

PrettyDump initializing...
PrettyDump connected to VPN 'demo-vpn' on broker 'demo.messaging.solace.cloud'.
Attempting to bind to queue 'q1' on the broker... success!
```

#### Shorcut mode: localhost broker, wildcard topics, and one-line output
```
$ prettydump "solace/>" -30

PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.
Subscribed to Direct topic: 'solace/>'

solace/samples/testing       This is a text payload.
```




## Command-line parameters

```
$ prettydump -h  or  --help

Usage: prettydump [host:port] [vpn] [username] [password] [topics|[qbf]:queue] [indent]
   or: prettydump <topics|q:queue|b:queue|f:queue> [indent]    for "shortcut" mode

 - If using TLS, remember "tcps://" before host
 - Default parameters will be: localhost default foo bar "#noexport/>" 4
 - Subscribing options (param 5, or shortcut mode param 1), one of:
    - Comma-separated list of Direct topic subscriptions
       - Strongly consider prefixing with '#noexport/' if using DMR or MNR
    - q:queueName to consume from queue
    - b:queueName to browse a queue (all messages, or range of messages by ID)
    - f:queueName to browse/dump only first oldest message on a queue
 - Optional indent: integer, default = 4 spaces; specifying 0 compresses payload formatting
    - One-line mode: use negative indent value (trim topic length) for topic & payload only
       - Or use -1 for auto column width adjustment
       - Use negative zero -0 for topic only, no payload
 - Shortcut mode: first argument contains '>', '*', or starts '[qbf]:', assume default broker
    - e.g. prettydump "logs/>" -1  ~or~  prettydump q:q1  ~or~  prettydump b:dmq -0
    - Or if first argument parses as integer, select as indent, rest default options
 - One-line mode (negative indent) runtime options:
    - Press "t[ENTER]" to toggle payload trim to terminal width
    - Press "+[ENTER]" to enable topic level spacing/alignment ("-[ENTER]" to revert)
    - Press "[1-9][ENTER]" to highlight a particular topic level ("0[ENTER]" to revert)
Environment variable options:
 - Multiple colour schemes supported. Override by setting: export PRETTY_COLORS=whatever
    - Choose: "standard" (default), "vivid", "light", "minimal", "matrix", "off"
 - Default charset is UTF-8. Override by setting: export PRETTY_CHARSET=whatever
    - e.g. export PRETTY_CHARSET=ISO-8859-1  (or "set" on Windows)
SdkPerf Wrap mode: use any SdkPerf as usual, pipe command to " | prettydump wrap" to prettify
 - Note: add the 'bin' directory to your path to make it easier
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
`q:<queueName>`, e.g. `q:q1`.  You will receive a warning that messages will be removed from the queue after they are received.

```
Attempting to bind to queue 'q1' on the broker... success!

Will consume/ACK all messages on queue 'q1'. Use browse 'b:' command-line option otherwise.
Are you sure? [y|yes]: 
```

### Browsing a Queue

To non-destructively view the messages on a queue, use the browse option: `b:<queueName>`.  Once the app starts up
and initializes, you have the option of browsing
all messages, a single message based on Message ID, or a range of messages (either closed "`12345-67890`" or open-ended "`12345-`").

To find the ID of the messages on a queue, either use PubSub+ Manager, CLI, or SEMP:

![View Message IDs in PubSubPlus Manager](https://github.com/SolaceLabs/pretty-dump/blob/main/src/browse-msgs.png)

**NOTE:** Use `f:<queueName>` to browse just the _first/oldest_ message on the queue. Very useful for "poison pills" or "head-of-line blocking" messages.

```
$ prettydump aaron.messaging.solace.cloud aaron-demo-singapore me pw b:q1

PrettyDump initializing...
PrettyDump connected to VPN 'aaron-demo-singapore' on broker 'aaron.messaging.solace.cloud'.
Attempting to browse queue 'q1' on the broker... success!

Browse all messages -> press [ENTER],
 or enter specific Message ID,
 or range of IDs (e.g. "25909-26183" or "9517-"): 31737085

Starting. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message #1 ^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'bus_trak/gps/v2/004M/01398/001.31700/0103.80721/30/OK'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           NON_PERSISTENT
Message Id:                             31737085
Replication Group Message ID:           rmid1:102ee-0b5760c9706-00000000-01e444fd
Binary Attachment:                      len=173
TextMessage, UTF-8 charset, JSON Object:
{
    "psgrCap": 1,
    "heading": 228,
    "busNum": 1398,
    "latitude": 1.317,
    "rpm": 1515,
    "speed": 60,
    "routeNum": "4M",
    "longitude": 103.80721,
    "status": "OK" }

^^^^^^^^^^^^^^^^^^ End Message #1 ^^^^^^^^^^^^^^^^^^^^^^^^^^
Browsing finished!
Main thread exiting.
Shutdown detected, quitting...
```


### Temporary Queue with Subscriptions

This mode allows you to choose the topics you wish to subscribe to, but do so in a Guaranteed fashion by provisioning a temporary / non-durable queue and then adding the topic subscriptions to that.
```
$ prettydump 'tq:#noexport/billing/>,#noexport/orders/>'

Creating temporary Queue.
Subscribed tempQ to topic: '#noexport/billing/>'
Subscribed tempQ to topic: '#noexport/orders/>'
```





## Output Indent options - the 6th argument

### Regular: indent > 0

Valid vales are between 1 and 20.  Indent is default 4 in these examples:
```
^^^^^^^^^^^^^^^^^ Start Message #7 ^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728274
Binary Attachment:                      len=100
Raw BytesMessage, valid UTF-8 charset, XML document:
<apps>
    <version>23</version>
    <stick>this</stick>
    <nested>
        <level>deeper</level>
    </nested>
</apps>
^^^^^^^^^^^^^^^^^^ End Message #7 ^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^ Start Message #8 ^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728275
Binary Attachment:                      len=159
SDT TextMessage, UTF-8 charset, JSON Object:
{
    "firstName": "Aaron",
    "lastName": "Lee",
    "zipCode": "12345",
    "streetAddress": "Singapore",
    "customerId": "12345" }
^^^^^^^^^^^^^^^^^^ End Message #8 ^^^^^^^^^^^^^^^^^^^^^^^^^^
```

### Compact: indent = 0

Specifying "0" as the indent value will cause JSON and XML strings to be compressed, with all indentation and carriage returns removed.  Also, "`^^^ End Message ^^^`" breaks will be removed.

```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^^^^^ Start Message #4 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4729017
Binary Attachment:                      len=58
Raw BytesMessage, valid UTF-8 charset, XML document:
<apps><another>hello</another><that>this</that></apps>
^^^^^^^^^^^^^^^^^^^^^ Start Message #5 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4729156
Binary Attachment:                      len=159
SDT TextMessage, UTF-8 charset, JSON Object:
{"firstName":"Aaron","lastName":"Lee","zipCode":"12345","streetAddress":"Singapore","customerId":"12345"}
```



### One-Line: indent < 0

Removes the majority of SdkPerf-like message metadata printing, only showing the topic and payload per-line.
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

Use `-1` for "auto-indenting", where the amount of indent will vary dynamically with topic length.



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


## One-Line mode: runtime options

When using one-line mode, you can do some extra stuff:

 - press 't' [ENTER] to enable trim for message payloads... will cause the payload to get truncated at the terminal width
```
bus_trak/gps/v2/022A/01228/001.32266/0103.69693/21/OK       {"psgrCap":0.75,"heading":176,"busNum"…
bus_trak/gps/v2/036X/01431/001.37858/0103.92294/32/STOPPED  {"psgrCap":0.5,"heading":288,"busNum":…
bus_trak/gps/v2/012A/01271/001.38968/0103.76101/31/STOPPED  {"psgrCap":0,"heading":254,"busNum":12…
bus_trak/gps/v2/002B/01387/001.27878/0103.82159/32/OK       {"psgrCap":0.75,"heading":272,"busNum"…
```
 - press '+' [ENTER] to add spacing to the topic hierarchy display, more of a "column" view of the topic levels
    - press '-' [ENTER] to go back to regular (compressed) topic display
```
#STATS./VPN..../sgdemo1./aaron.............../vpn_stats
#STATS./SYSTEM./sgdemo1./stats_client_detail
#STATS./SYSTEM./sgdemo1./MSG-SPOOL
#STATS./VPN..../sgdemo1./singtelbusdemo....../vpn_msg-spool
#STATS./SYSTEM./sgdemo1./REDUNDANCY
```
 - press '[1-9]' [ENTER] to highlight a specific level of the topic hierarchy
    - press '0' [ENTER] to go back to regular full-topic highlighting




## Charset Encoding

When publishing (Structured Data Type) **TextMessages** in Solace, the Strings are encoded as UTF-8.  However if publishing Strings as
raw **BytesMessages**, the binary encoding is determined by the publisher.  For example, if using our JavaScript API,
`message.setBinaryAttachment(String)` is encoded as Latin 1 / ISO-8859-1. (you should really use a `TextEncoder` and `Uint8Array` in JS).
Therefore, the String `"The currency is £ Pound Sterling."` will end up encoding the Pound sign as `0xA3` instead of UTF-8 `0xC2A3`. 
If PrettyDump detects an invalid encoding, it will replace the invalid characters with a highlighted upside-down question mark
"¿", and also provide a binary dump of the message to help you locate the invalid character(s):

```
⋮
Binary Attachment:                      len=36
Raw BytesMessage, Non UTF-8 encoded string:
The currency is in ¿ Pound Sterling.
    54 68 65 20 63 75 72 72    65 6e 63 79 20 69 73 20    The curr  ency is
    69 6e 20 a3 20 50 6f 75    6e 64 20 53 74 65 72 6c    in · Pou  nd Sterl
    69 6e 67 2e                                           ing.

^^^^^^^^^^^^^^^^^^^^^^ End Message #3 ^^^^^^^^^^^^^^^^^^^^^^
```



## Error Checking

As part of the parsing for JSON and XML, it detects if the payload is malformed and prints out some text to indicate that.
This can be helpful if trying to hand-code a payload and don't get it quite right.

```
^^^^^^^^^^^^^^^^^^^^^ Start Message #2 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'bad/xml/test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
HTTP Content Type:                      application/xml
Binary Attachment:                      len=43
Raw BytesMessage, UTF-8 charset, INVALID XML payload:
SaxParserException - org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 23;
 The markup in the document following the root element must be well-formed.
<bad>not having</bad><a>closing bracket</a

^^^^^^^^^^^^^^^^^^^^^^ End Message #2 ^^^^^^^^^^^^^^^^^^^^^^

```



## Protobuf Stuff

PrettyDump comes preloaded with Protobuf decoders for:
- Solace Distributed Trace messages
- Sparkplug B

Check the `protobuf.properties` file for topic-to-class mapping.  PrettyDump uses topic dispatch to
selectively choose which Protobuf decoder to use based on the incoming messages' topics.

Check the README file in the `schemas` directory for more information on how to compile and include
your custom Protobuf decoders.


### Distributed Trace

If you want to demo/test the Solace broker's Distributed Trace functionality without connecting the
OpenTelemetry Collector and deploying a backend such as Jaeger or DataDog, you can use PrettyDump to 
sniff the messages from the `#telemetry-xxxxx` (or whatever name) queue.  Simply bind to the queue
(either `q:xxxxx` or `b:xxxxx`) 
with whatever username and password you created with the telemetry profile.  PrettyDump will decode
the messages and print them out for you:
```
Raw BytesMessage, SpanData ProtoBuf:
    'trace_id' (BYTES): [7a f5 37 bf 0e f0 00 02 ce 9a c0 59 b2 57 0f e5]
    'span_id' (BYTES): [9b c7 5c 9f 66 de 57 2a]
    'start_time_unix_nano' (SFIXED64): 1719524089241349700 (Thu 2024-06-27 14:34:49.241 PDT)
    'end_time_unix_nano' (SFIXED64): 1719524089241357600 (Thu 2024-06-27 14:34:49.241 PDT)
    'topic' (STRING): "q1/a/b/c"
    'client_name' (STRING): "AaronsThinkPad3/20033/000f0001/6OZBwoOWb7"
    'client_username' (STRING): "default"
    'host_ip' (BYTES): [ac 11 00 02] (172.17.0.2)
    'host_port' (UINT32): 55555
    'peer_ip' (BYTES): [ac 11 00 01] (172.17.0.1)
    'peer_port' (UINT32): 49382
    'broker_receive_time_unix_nano' (SFIXED64): 1719524089241258000 (Thu 2024-06-27 14:34:49.241 PDT)
    'enqueue_events' (MESSAGE[]): [
      (MESSAGE):
        'time_unix_nano' (SFIXED64): 1719524089241355000 (Thu 2024-06-27 14:34:49.241 PDT)
        'queue_name' (STRING): "q1" ]
    'router_name' (STRING): "solace1081"
    'message_vpn_name' (STRING): "default"
    'replication_group_message_id' (BYTES): [01 33 ed ce 7c 36 16 84 36 00 00 00 00 00 00 05 c9]
    'protocol' (STRING): "SMF"
    'protocol_version' (STRING): "3.0"
    'priority' (UINT32): 4
    'binary_attachment_size' (UINT32): 510
    'solos_version' (STRING): "10.8.1.126"
```



## SdkPerf Wrap Mode

If you really need to use SdkPerf (e.g. for additional features like request-reply, publish-on-receive, or basic performance
testing), you can use PrettyDump in "wrap" mode to beautify your console: it will pretty-print any displayed message content, 
as well as a few other goodies.  Simply type your SdkPerf command, and then pipe `|` to `prettydump wrap`:


```
$ ./sdkperf_java.sh -cip=0 -stl=a/b/> -ptl=a/b/c -mn=100000 -mr=10000 | prettydump wrap

 __________                 __    __           __      __
 \______   \_______   _____/  |__/  |_ ___.__./  \    /  \____________  ______
  |     ___/\_  __ \_/ __ \   __\   __<   |  |\   \/\/   /\_  __ \__  \ \____ \
  |    |     |  | \/\  ___/|  |  |  |  \___  | \        /  |  | \// __ \|  |_> >
  |____|     |__|    \___  >__|  |__|  / ____|  \__/\  /   |__|  (____  /   __/
                         \/            \/            \/               \/|__|
PrettyDump WRAP mode for SdkPerf enabled... 😎

PUB MR(5s)=    0↑, SUB MR(5s)=    0↓, CPU=0
PUB MR(5s)=10009↑, SUB MR(5s)= 9970↓, CPU=0
PUB MR(5s)= 9993↑, SUB MR(5s)= 9989↓, CPU=0
PUB MR(5s)=10004↑, SUB MR(5s)=10023↓, CPU=0
```
