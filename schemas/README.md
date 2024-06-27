# ProtoBuf Definitions




There are 3 options for including your Protobuf definitions with PrettyDump:

1. Build the Java source files, copy them into the `src/main/java` directory, and rebuild
1. Compile the Java source files into class files, and copy into `protobuf/protobufs/protobufs` directory, and rebuild
    - Or, if using the prebuilt distribution, you can simply copy the class files into `lib/protobufs` directory
1. Build the class files into a JAR and include inside the `lib` directory, and rebuild
    - If using the prebuilt distribution, you can copy the JAR files into `lib` directory, **but** you'll need to update the classpath variable of the scripts in the `bin` directory


## Step 1: build Java source files

First, you need to download the ProtoBuf compiler.  You will find this on GitHub at https://github.com/protocolbuffers/protobuf/releases

Choose the appropriate release for your system. For example: `protoc-25.3-linux-x86_64.zip`

Unzip the compiler.  And then copy your `.proto` definitions files into the same directory.

Make a directory to store the Java source files, packages, directories.  E.g. `mkdir src`

Run the compiler for each of your `.proto` definitions to build the Java source files.
For example, if your definition file is `receive_v1.proto`, then:
```
bin/protoc --java_out src receive_v1.proto
```

Or just:
```
bin/protoc --java_out src *.proto
```

If you wish, you can copy these source files (everything under `src`) into `src/main/java` and rebuild the PrettyDump project, then you're done.  Otherwise, continue to Step 2.


## Step 2: compile into class files

For this step, you will need a Java compiler `javac` on your path.  These steps were tested to work with JDK 8 and JDK 11.
I try to use JDK 8 for compiling distributions in order to work with the most people.

First, you will need to download the most recent v3 Protobuf Java JAR library.  Either from a central Maven repository, or from within
the distribution of PrettyDump once it's compiled and downloads the required library.

- https://central.sonatype.com/artifact/com.google.protobuf/protobuf-java/versions
- https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java

Next, make a directory to store the compiled Java classes.  E.g. `mkdir classes`

Then, build the Java source files into class files:
```
find ./src/ -type f -name "*.java" -exec javac -cp "./protobuf-java-3.25.3.jar:./src/" -d ./classes '{}' +
```

If using Command Prompt on Windows, the command would be different.


## Step 3: build into a JAR if you want

ls classes | xargs jar cf solace-dt-protobufs.jar -C classes

Then copy the JAR into the `lib` directory and rebuild




## Step 4: update the topic-to-proto mapping

Locate the file `protobuf.properties` (either inside the `lib` directory of the built distribution, or inside `./schemas/classes/classes` dir).  Add one line for each topic subscription -> ProtoBuf definition Java class.  This may take a bit of investigation into the generated source files.  But should look something like:
```
solace/topic/to/match/>=<java.package.name>.<generatedClassName>$<internalClassNameForMessageType>
```






