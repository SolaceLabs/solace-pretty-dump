/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/6.8.3/userguide/building_java_projects.html
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    java
    eclipse
    idea
}

version = "1.1.0"

eclipse {
    classpath {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
    jdt {
        //if you want to alter the java versions (by default they are configured with gradle java plugin settings):
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}


repositories {
    mavenCentral()
    flatDir {
        dirs("include")  // extra JARs
        //dirs("protobuf/protobufs/protobufs")  // some class files
    }
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir("schemas/classes/classes")   // so eclipse can find the properties file
        }
    }
}

buildscript {
    dependencies {
        // classpath("config")
        //classpath fileTree(dir: 'libs', include: '*.jar')
        //classpath(fileTree(mapOf("dir" to "config", "include" to listOf("*.props"))))
    }
}

distributions {
    main {
        //distributionBaseName = "prettydump"
        distributionClassifier = "beta"
        contents {
            from("README.md")
            from("LICENSE")
            from("scripts") {
                into("scripts")
	    }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec> {
     // uncomment this out if you want faster compiled code with all assertion checks removed
     //enableAssertions = false
}

dependencies {

    // versions after 10.22 have about 40 netty deps
    implementation("com.solacesystems:sol-jcsmp:10.+")
    //implementation("com.solacesystems:sol-jcsmp:10.22.+")
    //runtimeOnly("org.slf4j:slf4j2-log4j12:1.7.6')
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.+")


    //implementation("com.solacesystems:sol-jcsmp:10.22.0")
    //implementation("org.json:json:20230227")
    // XML stuff...
//    implementation("org.dom4j:dom4j:2+")

    implementation("org.fusesource.jansi:jansi:2+")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.apache.logging.log4j:log4j-api:2.+")
    implementation("org.apache.logging.log4j:log4j-core:2.+")
    // needed to 'bridge' the JCSMP API logs from JCL to log4j
    runtimeOnly("commons-logging:commons-logging:1.3.2")
    runtimeOnly("org.apache.logging.log4j:log4j-jcl:2.+")

    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    implementation("com.google.protobuf:protobuf-java:3.+")

    implementation("org.apache.avro:avro:1.+")

    implementation(fileTree(mapOf("dir" to "include", "include" to listOf("*.jar"))))
//    implementation(fileTree(mapOf("dir" to "protobuf/jars", "include" to listOf("*.jar"))))
    //implementation(files("protobuf/classes"))
    //implementation(files("protobuf/jars"))
    //implementation(files("protobuf/protobufs/protobufs"))
    //implementation(files("protobuf/protobufs"))
    implementation(files("schemas/classes"))


//    implementation("com.solace:solace-opentelemetry-jcsmp-integration:1.1.0")
//    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.29.0")
//    implementation("io.opentelemetry:opentelemetry-semconv:1.29.0-alpha")


    testImplementation("junit:junit:4.+")
    testImplementation("org.json:json:20230227")
}




tasks.jar {
    manifest {
        archiveBaseName.set("solace-pretty-dump")
    }
}

tasks.installDist {
    destinationDir = file(layout.buildDirectory.dir("staged"))
}

tasks.assemble {
    dependsOn("installDist")
}

application {
    // Define the main class for the application.
    mainClass.set("com.solace.labs.aaron.PrettyDump")
    //classpath += files("config/")
    //project.logger.lifecycle("my message visible by default")
    //project.logger.lifecycle($runtimeClasspath)
    applicationDefaultJvmArgs = listOf("-ea")
}

fun createAdditionalScript(name: String, configureStartScripts: CreateStartScripts.() -> Unit) =
  tasks.register<CreateStartScripts>("startScripts$name") {
    configureStartScripts()
    applicationName = name
    outputDir = File(project.layout.buildDirectory.get().asFile, "scripts")
    classpath = tasks.getByName("jar").outputs.files + configurations.runtimeClasspath.get()
    //defaultJvmOpts = [ "-ea" ]  // enable assertions
    //defaultJvmOpts = listOf("-ea")  // enable assertions
    defaultJvmOpts = listOf("networkaddress.cache.ttl=0")  // disable DNS caching
    //defaultJvmOpts = listOf("-ea").iterator().asSequence().toList()  // enable assertions
  }.also {
    application.applicationDistribution.into("bin") {
      from(it)
      fileMode = 0b000_111_101_101
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
  }

//createAdditionalScript("PrettyDumpWrap") {
//  mainClass = "com.solace.labs.aaron.PrettyWrap"
//}

//createAdditionalScript("bar") {
//  mainClassName = "path.to.BarKt"
//}


//task(createStartScripts(type: CreateStartScripts)) {
//  outputDir = file("build/sample")
//  mainClass = "org.gradle.test.Main"
//  applicationName = "myApp"
//  classpath = files("path/to/some.jar")
//}

