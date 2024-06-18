# No Java, no problem

How to build a Docker image of PrettyDump with custom Protobuf definitions.



## Step 0: Load in the latest PrettyDump docker image:

```bash
$ docker load -i ./solace-pretty-dump-latest.tgz
Loaded image: solace-pretty-dump:latest
```




## Step 1: build Protobuf Java source files

First, you need to download the ProtoBuf compiler.  You will find this on GitHub at https://github.com/protocolbuffers/protobuf/releases

Choose the appropriate release for your system. For example: `protoc-25.3-linux-x86_64.zip`

Unzip the compiler.  And then copy your `.proto` definitions files into the same directory.

Make a directory to store the Java source files, packages, directories.  E.g.:
```bash
$ mkdir src
```

Run the compiler for each of your `.proto` definitions to build the Java source files.
For example, if your definition file is `receive_v1.proto`, then:
```bash
$ bin/protoc --java_out src receive_v1.proto
```

Or just use wildcards if you have lots of files:
```bash
$ bin/protoc --java_out src *.proto
```

It should (?) generate one Java source file for each Protobuf defintion.




## Step 2: build Docker container to use as image baseline

```bash
$ docker create --name pretty-baseline solace-pretty-dump:latest
9c9ddb0b9d52dd29df4bf176db5826d5076a3eb81722409b839d19e80a995872

$ docker ps -a | grep pretty
CONTAINER ID   IMAGE                       COMMAND              CREATED         NAMES
b4e9d2dd3640   solace-pretty-dump:latest   "./bin/prettydump"   9 seconds ago   pretty-baseline
```

Copy out required Protobuf JAR file (used for building the Java class files in Step 4), and properties file (Step 5).  The first command is to verify the exact version that's inside the container (you can't use wildcards with `docker cp`).
```bash
$ docker export pretty-baseline | tar t | grep protobuf-java
opt/pretty/lib/protobuf-java-3.25.3.jar

$ docker cp pretty-baseline:opt/pretty/lib/protobuf-java-3.25.3.jar .
Successfully copied 1.88MB to /home/alee/dev/proto/.

$ docker cp pretty-baseline:opt/pretty/lib/classes/protobuf.properties .
Successfully copied 3.07kB to /home/alee/dev/proto/.
```




## Step 3: download a JDK

Goto OpenJDK website: https://jdk.java.net/22/.  Grab the link for the build for your OS (right-click, copy link address).  
Then download it, and unzip it:

```bash
$ wget https://download.java.net/java/GA/jdk22/<somebighash>/36/GPL/openjdk-22_linux-aarch64_bin.tar.gz
$ tar zxf openjdk-22_linux-x64_bin.tar.gz
```

This should make a directory called `jdk-22`




## Step 4: build the class files

```bash
$ mkdir classes
$ find ./src/ -type f -name "*.java" -exec ./jdk-22/bin/javac -cp "./protobuf-java-3.25.3.jar:./src/" -d ./classes '{}' +
```

This should make a bunch of Java class files under the `classes` directory

```bash
$ ls -R1 classes
<SNIP>
classes/com/blah/proto/package/name:
v1

classes/com/blah/proto/package/name/v1:
'ReceiveV1$SpanData$1.class'
'ReceiveV1$SpanData$TransactionEvent$TransactionIdCase.class'
'ReceiveV1$SpanData$Builder$UserPropertiesConverter.class'
'ReceiveV1$SpanData$TransactionEvent$Type$1.class'
'ReceiveV1$SpanData$Builder.class'
'ReceiveV1$SpanData$TransactionEvent$Type.class'
'ReceiveV1$SpanData$TransactionEvent$LocalTransactionId$Builder.class'
'ReceiveV1$SpanData.class'
'ReceiveV1$SpanData$TransactionEvent$LocalTransactionId.class'
'ReceiveV1$SpanDataOrBuilder.class'
'ReceiveV1$SpanData$TransactionEvent$LocalTransactionIdOrBuilder.class'
ReceiveV1.class
```






## Step 5: update the topic-to-protobuf properties file

The `protobuf.properties` file (Step 2) is used by PrettyDump to figure out which messages (based on topics / subscriptions) should use which Protobuf deserializers.  There is some gathering of info for this step.

_**You will need:**_ `[topic-subscription] = [package-name].[class-name]$[proto-message-name]`

In each `.proto` definitions file, it should list the _package_ name:
```bash
$ grep package receive_v1.proto
package com.blah.proto.package.name.v1;
```


The generated _class_ name should be a PascalCase version of the `.proto` filename.  E.g.`receive_v1.proto` -> "ReceiveV1".
This should also be seen inside the corresponding `classes` subdirectory:
```bash
$ ls -R1 classes | grep "\\.class" | grep -v "\\$"
ReceiveV1.class
```


The _Message_ name can also be found inside the `.proto` definition as a top-level Message object:

```bash
$ grep "^message" receive_v1.proto
message SpanData {
```

**NOTE:** I'm not sure if it is possible/allowed to have multiple "root level" Message definitions inside a single `.proto` file..? 🤔

You should also be able to see a generated `.class` file that matches this Message:
```bash
$ ls -R1 classes | grep ReceiveV1.SpanData.class
ReceiveV1$SpanData.class
```

(note the "$" sign, an inner class).

So now you can add an entry / entries to the `protobuf.properties` file that tells PrettyDump that when a message is received with a topic that matches a particular subscription, to use this
specific Protobuf definition.  E.g.:

```
root/topic/to/match/> = com.blah.proto.package.name.v1.ReceiveV1$SpanData
```

Use of topic subscription wildcards in effect:
 - `*` single-level wildcard, can have a prefix
 - `>` multi-level 1-or-more wildcard, must occur at the end following a `/`
 - `#` (MQTT-style) multi-level 0-or-more wildcard, must occur at the end following a `/`

Repeat Step 5 for all Protobuf definitions and all Solace topics that match.


## Step 6: copy the modified files back into the container

```bash
$ docker cp protobuf.properties pretty-baseline:opt/pretty/lib/classes
Successfully copied 3.07kB to pretty-baseline:opt/pretty/lib/classes

$ docker cp classes/* pretty-baseline:opt/pretty/lib/classes
Successfully copied 634kB to pretty-baseline:opt/pretty/lib/classes
```




## Step 7: build a new Docker image from the modified container

You need the container ID of the `pretty-baseline` container.  User `docker ps -a` to find it.
```bash
$ docker ps -a | grep pretty
CONTAINER ID   IMAGE                       COMMAND
b4e9d2dd3640   solace-pretty-dump:latest   "./bin/prettydump"

$ docker commit b4e9d2dd3640 pretty-modded
sha256:b542460354cbaf155a2cf0ca83208acd6b63dd9c58e15ce00c6d7fecde03d68d

$ docker images | grep pretty
pretty-modded             latest                      b542460354cb       17 seconds ago   121MB
solace-pretty-dump        latest                      4103ff6066d1       4 hours ago      120MB
```

You now have a modified PrettyDump image to use with your custom Protobuf definitions! 🎉  Share it with your friends and colleagues..!
```bash
$ docker image save pretty-modded:latest > prettydump-modded.tar
$ gzip prettydump-modded.tar
$ mv prettydump-modded.tar.gz prettydump-modded.tgz
```






## Step 8: run the new Docker image!

To run with the "standard" 16-color palette, simply do:

```bash
$ docker run -it --rm pretty-modded 192.168.10.20 vpn user pw ">"
```

Substituting your broker URL, VPN name, username, and password as appropriate. Supports Direct topic subscriptions, consuming from a queue, and browsing a queue.  For help with command line options, check [the README on GitHub](https://github.com/SolaceLabs/pretty-dump?tab=readme-ov-file#command-line-parameters), or use `-h` or `--help`:

```bash
$ docker run -it --rm pretty-modded -h
Usage: prettydump [host:port] [msg-vpn] [username] [password] [topics|q:queue|b:queue|f:queue] [indent]
   or: prettydump <topics|q:queue|b:queue|f:queue> [indent]    for "shortcut" mode
...
```


### MOAR COLORS!

To run PrettyDump with an extended 256-color palette, use the additional environment variables:

```bash
$ docker run -it --rm -e "PRETTY_COLORS=vivid" -e "TERM=xterm-256color" pretty-modded:latest ...

     ~ or, for white/light coloured backgrounds ~

$ docker run -it --rm -e "PRETTY_COLORS=light" -e "TERM=xterm-256color" pretty-modded:latest ...
```

Probably easiset to just make an `alias`:
```bash
$ alias pretty='docker run -it --rm -e "PRETTY_COLORS=vivid" -e"TERM=xterm-256color" pretty-modded:latest'
$ pretty public.messaging.solace.cloud public public public ">"
```




