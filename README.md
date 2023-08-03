# pretty-dump
A useful utility that emulates SdkPerf `-md` output, echoing received Solace messages to the console, but pretty-printed for JSON.


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
