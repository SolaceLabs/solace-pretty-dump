#!/bin/sh

./gradlew clean assemble
cd build/distributions
unzip -q prettydump.zip
cd ../..

