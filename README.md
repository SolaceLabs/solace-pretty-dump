# pretty-dump for JSON and XML Solace messages
A useful utility that emulates SdkPerf `-md` output, echoing received Solace messages to the console, but pretty-printed for **JSON** and **XML**.


## Requirements

Java 8


## Building

```
./gradlew assemble
cd build/distributions
unzip PrettyDump.zip
cd PrettyDump
```

Or just download a [Release distribution](https://github.com/SolaceLabs/pretty-dump/releases) with everything built.


## Running

```
Usage: PrettyDump <host:port> <message-vpn> <client-username> <password> <topics | q:queue | b:queue> [indent]

 - If using TLS, remember "tcps://" before host
 - One of:
    - comma-separated list of Direct topic subscriptions
    - "q:queueName" to consume from queue
    - "b:queueName" to browse a queue
       - Can browse all messages, or specific messages by ID
 - Optional indent: integer, default=4; specifying 0 compresses payload formatting
    - Use negative indent value (column width) for ultra-compact topic & payload only
 - Default charset is UTF-8. Override by setting: export PRETTY_DUMP_OPTS=-Dcharset=whatever
    - e.g. export PRETTY_DUMP_OPTS=-Dcharset=Shift_JIS  (or "set" on Windows)
```


## Output

### Regular, indent = 4

```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728274
Binary Attachment:                      len=78
XML, BytesMessage:
<apps>
    <version>23</version>
    <another>hello</another>
    <that>this</that>
</apps>

^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4728275
Binary Attachment:                      len=159
JSON Object, TextMessage:
{
    "firstName": "Aaron",
    "lastName": "Lee",
    "zipCode": "12345",
    "streetAddress": "Singapore",
    "birthdayDate": "1999/01/02",
    "customerId": "12345"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```

### Compact, indent = -35

```
pq-demo/stats/pq/sub-pq_3-c222       {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0,"ackd":0,"gaps":0,"flow":"FLOW_ACTIVE"}
pq/3/pub-44e7/e7-7/0/_          
pq-demo/stats/pq/pub-44e7            {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2,"resendQ":0,"keys":8,"activeFlow":true}
pq/3/pub-44e7/e7-5/0/_          
pq-demo/stats/pq/sub-pq_3-c222       {"red":0,"oos":0,"queueName":"pq/3","slow":0,"rate":0,"ackd":0,"gaps":0,"flow":"FLOW_ACTIVE"}
solace/samples/jcsmp/hello/aaron     +...4..probability...>���..from...aaron...age.......
pq/3/pub-44e7/e7-3/0/_          
pq-demo/stats/pq/pub-44e7            {"prob":0,"paused":false,"delay":0,"nacks":0,"rate":2,"resendQ":0,"keys":8,"activeFlow":true}
pq/3/pub-44e7/e7-0/0/_          
```

