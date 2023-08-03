# pretty-dump
A useful utility that emulates SdkPerf `-md` output, echoing received Solace messages to the console, but pretty-printed for JSON and XML.


## Requirements

Java

## Building

```
./gradlew assemble
cd build/distributions
unzip pretty-dump.zip
cd pretty-dump
./bin/pretty-dump <host:port> <message-vpn> <client-username> <password> <topics | q:queue>
```

Either:
- comma separated list of topics
- or "q:queueName" for a queue


```
PrettyDump connected, and running. Press Ctrl-C to quit.
^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             4
Binary Attachment:                      len=78

BytesMessage XML:
<apps>
  <version>23</version>
  <another>hello</another>
  <stick>this</stick>
</apps>

^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
Destination:                            Topic 'test'
Priority:                               4
Class Of Service:                       USER_COS_1
DeliveryMode:                           DIRECT
Message Id:                             5
Binary Attachment:                      len=159

TextMessage JSON: {
    "firstName": "Aaron",
    "lastName": "Lee",
    "zipCode": "12345",
    "streetAddress": "Singapore",
    "birthdayDate": "1999/01/02",
    "customerId": "12345"
}
^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^
```
