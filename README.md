👋🏼 If you find this utiliy useful, please give it a ⭐ above!  Thanks! 🙏🏼

![PrettyDump Banner](src/prettydump5.png)
# Pretty-print for dumped Solace messages

A useful utility that emulates SdkPerf `-md` "message dump" output, echoing received Solace messages to the console, but colour pretty-printed for **JSON**, **XML**, **Protobuf**, and Solace **SDT** Maps and Streams. (And binary too).
Also with a display option for a minimal one-line-per-message view.  Supports Direct topic subscriptions, Queue consume, Queue browsing, and temporary Queue w/subs.  Now with Selector and client-side Filtering support, and much higher performance than JMSToolBox.


- [Building](#building)
- [Running](#running)
- [Command-line parameters](#command-line-parameters)
- Subscribing options: [Direct topic subscriptions](#direct-subscriptions), [Queue consume](#queue-consume), [Browsing a queue](#browsing-a-queue), [TempQ with subs](#temporary-queue-with-subscriptions)
- [Output Indent options](#output-indent-options-the-6th-argument) ([One-line Mode](#one-line-mode-indent--0))
- [Count, Selectors and Filtering](#count-selectors-and-filtering)
- [Additional Parameters](#additional-parameters)
- [One-line Mode: Runtime options](#one-line-mode-runtime-options)
- [Charset Encoding](#charset-encoding)
- [Error Checking](#error-checking)
- [Protobuf Stuff](#protobuf-stuff) & Distributed Trace
- [SdkPerf Wrap Mode](#sdkperf-wrap-mode)
- [Miscellaneous](#miscellaneous)
- [Tips and Tricks](#tips-and-tricks)


## Requirements

- Java 8+  or  Docker
- Network access to a Solace broker (software, cloud, appliance)



## Building

```
./gradlew clean assemble
cd build/staged/bin
prettydump
```

Or just download a [Release distribution](https://github.com/SolaceLabs/pretty-dump/releases) with everything already built.



## Running

**N.B.** for those using Windows PowerShell or Command Prompt, see [Tips and Tricks](#windows) at the bottom.

For Docker: `docker run -it --rm solace-pretty-dump:latest broker vpn user pw ">" 4`

#### No args, default broker options
```
$ prettydump

PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.

Subscribed to Direct topic: '#noexport/>'

Starting. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^^^^^ Start Message #1 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'hello/world'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Type:                           SDT TextMessage
Binary Attachment:                      len=26 bytes
UTF-8 charset, JSON Object:
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
$ prettydump "solace/>" -30 --trim

PrettyDump initializing...
PrettyDump connected to VPN 'default' on broker 'localhost'.

Indent=-30 (one-line mode, topic width=30, auto-trim payload)
Subscribed to Direct topic: 'solace/>'

solace/samples/testing           This is a short text payload.
solace/test/long/topic/truncat…  Longer text payload that will get trimmed to terminal wid…
```




## Command-line parameters

```
$ prettydump -h or --help   or -hm for more

Usage: prettydump [host] [vpn] [user] [pw] [topics|[qbf]:queueName|tq:topics] [indent]
   or: prettydump <topics|[qbf]:queueName|tq:topics> [indent]  for "shortcut" mode

 - Default protocol TCP; for TLS use "tcps://"; or "ws://" or "wss://" for WebSocket
 - Default parameters will be: localhost:55555 default foo bar '#noexport/>' 2
 - Subscribing options (arg 5, or shortcut mode arg 1), one of:
    • Comma-separated list of Direct topic subscriptions
       - Automatic "#noexport/" prefixes added for DMR/MNR; disable with --export
    • q:queueName to consume from queue
    • b:queueName to browse a queue (all messages, or range by MsgSpoolID or RGMID)
    • f:queueName to browse/dump only first oldest message on a queue
    • tq:topics   provision a tempQ with optional topics  (can use NOT '!' topics)
 - Indent: integer, default==2; ≥ 0 normal, = 00 no payload, ≤ -0 one-line mode
 - Shortcut mode: first arg looks like a topic, or starts '[qbf]:', assume defaults
    • Or if first arg parses as integer, select as indent, rest default options
 - Additional args: --count, --filter, --selector, --trim, --ts, --raw, --compressed
 - Any JCSMP Session property (use --defaults to see all)
 - Environment variables for decoding charset and colour mode

prettydump -hm  for more help on indent, additional parameters, charsets, and colours
prettydump -he  for examples
```


## Subscribing options: the 5th argument

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
ALL messages, or a range of messages using the Message Spool ID or Repication Group Message ID (either closed "`12345-67890`" or open-ended "`12345-`").

To find the Message Spool ID or RGMID of the messages on a queue, either use PubSub+ Manager, CLI, or SEMP:

```
solace> show queue q1 message-vpn default messages newest detail
```
![View Message IDs in PubSubPlus Manager](https://github.com/SolaceLabs/pretty-dump/blob/main/src/browse-msgs.png)

I'm working on some bash shell scripts to help with this stuff.

**NOTE:** Use `f:<queueName>` to browse just the _first/oldest_ message on the queue. Very useful for "poison pills" or "head-of-line blocking" messages.  This the same as regular browse with `--count=1`.

```
$ prettydump aaron.messaging.solace.cloud aaron-demo-singapore me pw b:q1 --count=1

PrettyDump initializing...
PrettyDump connected to VPN 'aaron-demo-singapore' on broker 'aaron.messaging.solace.cloud'.

Attempting to browse queue 'q1' on the broker... success!

Browse all messages -> press [ENTER],
 or to/from or range of MsgSpoolIDs (e.g. "10659-11061" or "9817-" or "-10845"),
 or to/from RGMID (e.g. "-rmid1:3477f-a5ce52..."): 31737085-


Starting. Press Ctrl-C to quit.
🔎 MsgSpoolId outside of range. Recv'd=8960, Filtered=8960, Printed=0
^^^^^^^^^^^^^^^^^^^^^ Start Message #1 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'bus_trak/gps/v2/004M/01398/001.31700/0103.80721/30/OK'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           NON_PERSISTENT
Message Id:                             31737085
Replication Group Message ID:           rmid1:102ee-0b5760c9706-00000000-01e444fd
Message Type:                           SDT TextMessage
Binary Attachment:                      len=173
UTF-8 charset, JSON Object:
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

^^^^^^^^^^^^^^^^^^^^^^ End Message #1 ^^^^^^^^^^^^^^^^^^^^^^
1 messages received. Quitting.
Browsing finished!
Main thread exiting.
Shutdown detected, quitting...
```


### Temporary Queue with Subscriptions

This mode allows you to choose the topics you wish to subscribe to, but do so in a Guaranteed fashion by provisioning a temporary / non-durable queue and then adding the topic subscriptions to that.
```
$ prettydump 'tq:#noexport/billing/>, #noexport/orders/>, !#noexport/orders/cancelled/>'

Creating temporary Queue.
Queue name: #P2P/QTMP/v:solace1081/2ed38d19-ae36-4e0e-ba90-06eea306bb9a
Subscribed tempQ to topic: '#noexport/billing/>'
Subscribed tempQ to topic: '#noexport/orders/>'
Subscribed tempQ to *NOT* topic: '!#noexport/orders/cancelled/>'
```





## Output Indent options: the 6th argument

### Regular: indent > 0

Valid vales are between 1 and 8.  Indent is default 2 in these examples:
```
^^^^^^^^^^^^^^^^^^^^^ Start Message #7 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Type:                           Raw BytesMessage
Binary Attachment:                      len=100
Valid UTF-8 charset, XML document:
<apps>
  <version>23</version>
  <stick>this</stick>
  <nested>
    <level>deeper</level>
  </nested>
</apps>

^^^^^^^^^^^^^^^^^^^^^^ End Message #7 ^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^^^^^ Start Message #8 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Type:                           SDT TextMessage
Binary Attachment:                      len=159
UTF-8 charset, JSON Object:
{
  "firstName": "Aaron",
  "lastName": "Lee",
  "zipCode": "12345",
  "streetAddress": "Singapore",
  "customerId": "12345" }

^^^^^^^^^^^^^^^^^^^^^^ End Message #8 ^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^^^^^ Start Message #9 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'POST/bin/zip'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
HTTP Content Encoding:                  zip
Message Type:                           Raw BytesMessage
Binary Attachment:                      len=114 bytes
Non UTF-8 encoded string:
00000   50 4b 03 04 14 00 00 00   08 00 da 6c ec 58 51 fa    PK······  ···l·XQ·
00010   1d 1b cc 00 00 00 33 01   00 00 10 00 1c 00 6a 73    ······3·  ······js
00020   6f 6e 2d 70 72 65 74 74   79 2e 6a 73 6f 6e 55 54    on-prett  y.jsonUT
00030   04 e8 03 00 00 04 e8 03   00 00 65 4f cd 4a c3 40    ········  ··eO·J·@
00040   10 3e 67 9f 62 d8 73 91   a6 49 89 f1 22 b9 88 85    ·>g·b·s·  ·I··"···
00050   d2 4b c5 8b 78 18 e3 14   17 92 1d 9d dd 69 09 a5    ·K··x···  ·····i··
00060   00 00 00 00 01 00 01 00   56 00 00 00 16 01 00 00    ········  V·······
00070   00 00 ·· ·· ·· ·· ·· ··   ·· ·· ·· ·· ·· ·· ·· ··    ··

^^^^^^^^^^^^^^^^^^^^^^ End Message #4 ^^^^^^^^^^^^^^^^^^^^^^
```

### Compact: indent = 0

Specifying "0" as the indent value will cause JSON and XML strings to be compressed, with all indentation and carriage returns removed.  Also, "`^^^ End Message ^^^`" breaks will be removed.

```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^^^^^ Start Message #1 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Type:                           Raw BytesMessage
Binary Attachment:                      len=58 bytes, valid UTF-8 charset, XML document
<apps><another>hello</another><that>this</that></apps>
^^^^^^^^^^^^^^^^^^^^^ Start Message #2 ^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Type:                           SDT TextMessage
Binary Attachment:                      len=159, UTF-8 charset, JSON Object
{"firstName":"Aaron","lastName":"Lee","zipCode":"12345","streetAddress":"Singapore","customerId":"12345"}
```


### No Payload: indent = '00', '000', or '0000'

These modes will still parse the payloads (for validation and possible filtering using regex Filter `--filter` option) but not display them.
 - `00` will still pretty-print the User Properties
 - `000` will compact the User Properties onto the same line
 - `0000` will not display the User Properties at all (just a count)




### One-Line Mode: indent < 0

Removes the majority of SdkPerf-like message metadata printing, only showing the topic and payload per-line.
Valid values are between -1 and -250, and specify how many chars of the topic are displayed.  indent = -35 in this example.

```
pq-demo/stats/pq/sub-pq_3-c222       {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0}
pq/3/pub-44e7/e7-7/0/_               <EMPTY> Raw BytesMessage
pq-demo/stats/pq/pub-44e7            {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2}
pq/3/pub-44e7/e7-5/0/_               <EMPTY> Raw BytesMessage
pq-demo/stats/pq/sub-pq_3-c222       {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0}
solace/samples/jcsmp/hello/aaron     {probability(Float)0.4,from(String)"aaron",where(Topic)a/b/c}
pq/3/pub-44e7/e7-3/0/_               <EMPTY> Raw BytesMessage
pq-demo/stats/pq/pub-44e7            {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2}
pq/3/pub-44e7/e7-0/0/_               <EMPTY> Raw BytesMessage
```

 - Use `-1` for "auto-indenting", where the amount of indent will vary dynamically with topic length
 - Use `-2` for two-line mode, topic on one line, payload on another ("minimal" colour mode is good for this)
 - `-3` .. `-250` will trim the topic length to that amount
 - Change `-` to `+` in the argument to enable topic level alignment


### One-Line, Topic only: indent = "-0"

Specifying -0 for the indent only prints out the topic.  I've often used ` | grep Destination` with SdkPerf `-md` output,
this does essentially the same thing.

```
bus_trak/gps/v2/013B/01058/001.37463/0103.93459/13/STOPPED
bus_trak/gps/v2/002A/01151/001.32270/0103.75160/30/OK
bus_trak/door/v1/033M/01434/open
bus_trak/gps/v2/011X/01390/001.39620/0103.84082/11/OK
bus_trak/gps/v2/035B/01286/001.40101/0103.88913/12/OK
bus_trak/door/v1/005B/01464/close
```






## Count, Selectors and Filtering

PrettyDump now supports Selectors (broker-side) when consuming or browsing a queue, and also client-side Filtering which you specify as a regex against the entire message contents.  As well as a Count feature to minimize the number of messages dumped to the console.



### Count

Specified using `--count=n` anywhere in the arguments, this allows you to:

 - when `n > 0`: stop PrettyDump after receiving _n_ messages
 - when `n < 0`: cause PrettyDump to buffer the last _n_ messages, and dump them out when the program terminates

This allows you to do such things as:
 - consume/ACK the first 5 messages off a queue: `q:q1 --count=5`
 - browse the last 50 messaes on a queue: `b:q1 --count=-50` (must wait until filtering has stopped)
 - during program development / debugging, have PrettyDump tracking the last 500 messages `'>' --count -500`, and when an error is detected, Ctrl+C PrettyDump to show the last 500 messages for analysis

This can be combined with the Selector and Filter features below to allow even more advanced filtering capabilities.


### Selectors

Selectors can be very useful if you wish to filter the messages at the broker based on certain header metadata or user properties.  Selectors are specified using an SQL-92 like syntax.  Solace doesn't recommend them for general runtime usage due to performance considerations and the fact our topic routing capability is far superior to other JMS brokers.  However, they can certainly come in handy when browsing queues for certain messages.

A Selector is specified by the command line argument `--selector="blah"` when runnning PrettyDump.  This argument can appear anywhere in the arguments, and won't impact the other ones.  Note that Selectors don't work with Direct topic subscriptions, but does with queue consume, queue browsing, and temporary queues with subscriptions.  However!  Selectors are performed on the egress Flow, which means that any messages not matching the Selector will be left on the queue.  For example, a tempQ subscribed to `>` but with a very narrow Selector could fill up quickly.

Use "first message" browse mode `f:queueName` to stop after the first message that matches the Selector.  Or browse with count option `b:queueName --cout=10` to dump the first 10 messages that match the Selector.


### Client-side Filtering

As requested by a user, PrettyDump supports the ability to search/filter for specific words or patterns occuring within the entire message output, and only display messages that match this Filter.  This is far less performant than using a Selector (broker-side) as it has to pull down the message first and parse it before it can apply the Filter (client-side).  However, Filters can be used with Direct messages, and can be used in conjunction with Selectors.

Currently, the Filter is treated as a regular expression / regex, and the entire message contents (headers, properties, payload(s)) are evaluated against it.  So a filter of `Aaron` will search each message looking for the case-sensitive word "Aaron".  A filter of `(?i)Aaron.*Singapore` will look for the case-insensitive word "aaron" followed somewhere by "singapore".

A Filter is specified by the command line argument `--filter="blah"` when runnning PrettyDump.  This argument can appear anywhere in the arguments, and won't impact the other ones.

```
^^^^^^^^^^^^^^^^^^^^ End Message #26710 ^^^^^^^^^^^^^^^^^^^
🔎 Msg filtered. Recv'd=33808, Filtered=33805, Printed=3
```


### A word on performance

I filled up a queue with > 1M messages in it.  I used a browser and Selector to find a message I knew was at the back of the queue (looking for its Application Message ID / JMS Message ID).  It literally took a few seconds.  I ran the same test, using a client-side Filter, set to something equivalient: `^AppMessageID: +aaron123$` and it took maybe a minute or two to scrub through all the messages on the queue.  So: Selectors are great for browsing... Solace just doesn't recommend them for live data routing as typically same behaviour can be achieved through topic subscriptions, which are much more performant.



## Additional Parameters

There are a number of (argument order doesn't matter) parameters that have been added, and are documented in `prettydump -hm`.  Specifically:

- `--selector="blah"` See [Selectors](#selectors) above
- `--filter="blah"` See [Client-side Filtering](#client-side-filtering) above
- `--count=n` See [COunt](#count) above
- `--raw` Do not perform any pretty-print formatting on text string payloads, just leave alone. This applies to JSON and XML. Binary encodings such as SDTMaps, SDTStreams, Protobuf, etc. will be unaffected.
- `--dump` Binary dump (à la SdkPerf `-md`) all message payloads to screen. This parameter overrides `--raw`.
- `--trim` In one-line mode, trim payloads to console width to keep terminal display nice and neat 
- `--ts` Print the time-of-day the message was received by PrettyDump (not actual message timestamp). Works in both regular and one-line mode. Very useful if needing to observe timings between messages or exactly when live messages were received.
- `--export` By default, PrettyDump adds `#noexport/` prefix to every topic subscription, to help not overload DMR/MNR links by subscribing to things accidentally.  See https://docs.solace.com/Messaging/No-Export.htm.  Use this to disable.
- `--compressed` Tell PrettyDump you want to connect using streaming compression (not payload compression new feature). This is super useful when connecting over long RTT / WAN links. For non-TLS, this is port 55003.
- `--defaults` Print all the JCSMPProperties that you might be able to override.  Or check the docs: https://docs.solace.com/API-Developer-Online-Ref-Documentation/java/com/solacesystems/jcsmp/JCSMPProperties.html




## One-Line mode: runtime options

When using one-line mode, you can do some extra stuff:

 - press 't' [ENTER] to enable trim for message payloads... will cause the payload to get truncated at the terminal width
 - this can also be enabled with arg `--trim` when starting
```
bus_trak/gps/v2/022A/01228/001.32266/0103.69693/21/OK       {"psgrCap":0.75,"heading":176,"busNu…(len=178)
bus_trak/gps/v2/036X/01431/001.37858/0103.92294/32/STOPPED  {"psgrCap":0.5,"heading":288,"busNum…(len=175)
bus_trak/gps/v2/012A/01271/001.38968/0103.76101/31/STOPPED  {"psgrCap":0,"heading":254,"busNum":…(len=181)
bus_trak/gps/v2/002B/01387/001.27878/0103.82159/32/OK       {"psgrCap":0.75,"heading":272,"busNu…(len=177)
```


 - press '+' [ENTER] to add spacing to the topic hierarchy display, more of a "column" view of the topic levels
 - press '-' [ENTER] to go back to regular (compressed) topic display
 - this can also be enabled with by changing indent argument from `-` to `+` when starting
```
#STATS...../VPN..../sgdemo1.../aaron.............../vpn_stats
#STATS...../SYSTEM./sgdemo1.../stats_client_detail
#STATS...../SYSTEM./sg3501vmr./MSG-SPOOL
#STATS...../VPN..../sgdemo1.../singtelbusdemo....../vpn_msg-spool
#STATS...../SYSTEM./sgdemo1.../REDUNDANCY
#STATSPUMP./STATS../sg3501vmr./poller-stats......../show-msg-spool
```

 - press '1..n' [ENTER] to highlight a specific level of the topic hierarchy
 - press '0' [ENTER] to go back to regular full-topic highlighting
 - obviously you need a colour mode enabled to see this






## Charset Encoding

When publishing (Structured Data Type) **TextMessages** in Solace, the Strings are encoded as UTF-8.  However if publishing Strings as
raw **BytesMessages**, the binary encoding is determined by the publisher.  For example, if using our JavaScript API,
`message.setBinaryAttachment(String)` is encoded as Latin 1 / ISO-8859-1. (you should really use a `TextEncoder` and `Uint8Array` in JS).
Therefore, the String `"The currency is £ Pound Sterling."` will end up encoding the Pound sign as `0xA3` instead of UTF-8 `0xC2A3`. 
If PrettyDump detects an invalid encoding, it will replace the invalid characters with a highlighted upside-down question mark
"¿", and also provide a binary dump of the message to help you locate the invalid character(s):

```
⋮
Binary Attachment:                      len=56 bytes
Non UTF-8 charset, XML document:
<value>
  <price>3.99</price>
  <currency>¿</currency>
</value>
00000   3c 76 61 6c 75 65 3e 3c   70 72 69 63 65 3e 33 2e    <value><  price>3.
00010   39 39 3c 2f 70 72 69 63   65 3e 3c 63 75 72 72 65    99</pric  e><curre
00020   6e 63 79 3e 9c 3c 2f 63   75 72 72 65 6e 63 79 3e    ncy>·</c  urrency>
00030   3c 2f 76 61 6c 75 65 3e   ·· ·· ·· ·· ·· ·· ·· ··    </value>

^^^^^^^^^^^^^^^^^^^^^^ End Message #4 ^^^^^^^^^^^^^^^^^^^^^^
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
Message Type:                           Raw BytesMessage
Binary Attachment:                      len=43 bytes
Valid UTF-8 charset, INVALID XML payload:
SaxParserException - org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 23;
 The markup in the document following the root element must be well-formed.
<bad>not having<a>closing bracket</a</bad>

^^^^^^^^^^^^^^^^^^^^^^ End Message #2 ^^^^^^^^^^^^^^^^^^^^^^
```



## Protobuf Stuff

PrettyDump comes preloaded with Protobuf decoders for:
- Sparkplug B
- OpenTelemtry OTLP Traces
- Solace Distributed Trace messages (from `#telemetry-default` queue)

Check the `protobuf.properties` file for topic-to-class mapping.  PrettyDump uses [topic dispatch](https://github.com/aaron-613/jcsmp-topic-dispatch) to
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
Note that this is not the full detail of information sent by the Collector to the observability backend... the Collector does further enriching of the data.


### OpenTelemetry

You can also see the output of the Collector.  If you modify your OTel configuration file to send HTTP instead of gRPC, and point the Collector at a Solace broker's
REST/HTTP API port, you can subscribe to the topics (with your VPN in either in REST Messaging mode or REST Gateway mode).




## SdkPerf Wrap Mode

If you really need to use SdkPerf (e.g. for additional features like request-reply, publish-on-receive, or basic performance
testing), you can use PrettyDump in "wrap" mode to beautify your console: it will pretty-print any displayed message content, 
as well as a few other goodies.  Simply type your SdkPerf command, and then pipe `|` to `prettydump wrap`:  (looks better with colour!)


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


## Miscellaneous

### Subscriber ACK window size

PrettyDUmp sets the subscriber window size to 20.  This provides moderate performance.  Override this with environment variable `PRETTY_SUB_ACK_WINDOW_SIZE`, range 1..255.  Note that consumer ACKs are asynchronous, and the API will only send them once a second (by default) unless the ACK window has closed 60% (by default).  Setting the window size to 1 means that ACKs will be flushed immediately, which is quicker but might incur a performance penalty at higher message rates due to chattier comms.  However, when looking at Open Telemetry distributed trace logs, the broker `send` span will complete significantly faster.





## Tips and Tricks


### Negative Subscriptions

Solace has the concept of negative subscriptions (or subscription exceptions), but only for Guaranteed consumers (that is, subscriptions on Endpoints)... it doesn't work for Direct consumers.  If you want to see all the traffic on your broker _except_ for some specified topics, then use a temporary queue `tq:` and add subscriptions such as:
```
$ prettydump 'tq:>, !bus/> !stats/>'
```
This will configure a temporary queue to use, subscribed to everything, but if the topic matches one of the NOT topics, it won't match.  So in this example, you would receive everything that's not a `bus` topic or a `stats` topic.  Could be useful to see all the messages that your regular apps are _not_ subscribed to.

See: https://docs.solace.com/Messaging/Guaranteed-Msg/System-Level-Subscription-Exception-Config.htm



### Browse the end of a Queue

**Don't need to do this anymore!  Use `-count=-100` argument instead!**

Using SEMP, you can query the broker for details of the last _n_ messages sitting on a queue, and use those values to tell PrettyDump to skip all the messages before that (note: PrettyDump still needs to pull all the messages off the queue to evaluate them).  Check out the `show-last-nth-msg-spool-id.sh` script in the `scripts` directory to do this.
```
curl -u admin:admin http://localhost:8080/SEMP -d '<rpc><show><queue><name>q1</name><vpn-name>default</vpn-name><messages/><newest/><count/><num-elements>100</num-elements></queue></show></rpc>'

<rpc-reply semp-version="soltr/10_8_1VMR">
  <rpc>
    <show>
      <queue>
        <queues>
          <queue>
            <name>q1</name>
            <message-vpn>default</message-vpn>
            <spooled-messages>
              <spooled-message>
                <message-id>1047544</message-id>
                <message-sent>no</message-sent>
              </spooled-message>
              <spooled-message>
                <message-id>1047543</message-id>
                <message-sent>no</message-sent>
              </spooled-message>
...
```

Then use one of the Message Spool ID when you browse `b:` to skip all messages until you get here, e.g.:  (note the trailing `-` for "start range")
```
$ prettydump b:q1

Browse all messages -> press [ENTER],
 or to/from or range of MsgSpoolIDs (e.g. "10659-11061" or "9817-" or "-10845"),
 or to/from RGMID (e.g. "-rmid1:3477f-a5ce52..."): 1043984-    <--
```


### Browsing to end still too slow?

If you have a queue with millions of messages, it might take too long to browse to the end, and is also a lot of wasted bandwidth to pull everything off the queue.  So I made another utility that utilizes Solace's "copy message" capability.  The script uses SEMPv1 to query the message details from the back of the queue (newest messages), and then SEMPv1 to copy each message one-by-one to a destination queue.  The intention is to use this in conjunction with a temporary queue `tq:` mode.

Note that the copied messages are _new_ messages, and as such will have different Message Spool IDs and different RGMIDs.  But the contents of the messages with be exactly the same.



### One-Line Dump w/dynamically spaced topics

One of the most common ways I run this when developing an app and just want to sniff the broker is to use one-line mode, auto indent `+1` with spacing, and enable payload trim with argument `--trim`.  You can always press `+` or `-` `[ENTER]` during runtime to toggle between spacing modes, or `t` `[ENTER]` to toggle payload trim.
```
pq-demo/proc..../pq12/sub-pq12-4049/8f-7/0/_  <EMPTY> Raw BytesMessage
pq-demo/stats.../pq../sub-pq12-4049           {"red":0,"oos":0,"queueName":"pq12","slow":0,"…(len=189)
pq12.../pub-dc8f/8f-4/0............/_         <EMPTY> Raw BytesMessage
pq-demo/proc..../pq12/sub-pq12-4049/8f-4/0/_  <EMPTY> Raw BytesMessage
pq-demo/stats.../pq../pub-dc8f                {"prob":0,"paused":false,"delay":0,"nacks":0,"r…(len=85)
```

And use "vivid" colour mode `export PRETTY_COLORS=vivid` for nice rainbow topic level colouring.




### Windows

If running on Windows PowerShell or Command Prompt, make sure you enable UTF-8 charset / code point encoding to see all the emojis!  Windows _still_ uses its default (and ancient) `Windows-1252` encoding.  Also enable full 256 colour output!
```
C:\> chcp 65001
C:\> set PRETTYDUMP_OPTS=-Dsun.stdout.encoding=utf-8
C:\> set TERM=xterm-256color
C:\> prettydump.bat

 ~or~

PS C:\> chcp 65001
PS C:\> $Env:PRETTYDUMP_OPTS='-Dsun.stdout.encoding=utf-8'
PS C:\> $Env:TERM='xterm-256color'
PS C:\> .\prettydump
```

See: https://en.wikipedia.org/wiki/Windows-1252









