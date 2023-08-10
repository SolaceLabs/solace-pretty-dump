# pretty-dump for JSON and XML Solace messages
A useful utility that emulates SdkPerf `-md` output, echoing received Solace messages to the console, but pretty-printed for **JSON** and **XML**.


## Requirements

Java


## Building

```
./gradlew assemble
cd build/distributions
unzip PrettyDump.zip
cd PrettyDump
```

Or just download a Release zipfile with everything built.


## Running

```
bin/PrettyDump <host:port> <message-vpn> <client-username> <password> <topics | q:queue | b:queue> [indent]

 - If using TLS, remember "tcps://" before host
 - One of:
    - separated list of topics
    - "q:queueName" to consume from queue
    - "b:queueName" to browse a queue
 - Optional indent: integer, default==4; specifying 0 compresses output
 - Default charset is UTF-8. Override by setting: export PRETTY_DUMP_OPTS=-Dcharset=whatever
```


## Output

```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4
Binary Attachment:                      len=78

XML BytesMessage:
<apps>
    <version>23</version>
    <another>hello</another>
    <that>this</that>
</apps>

^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             5
Binary Attachment:                      len=159

JSON TextMessage:
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
