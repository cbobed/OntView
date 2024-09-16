#!/bin/bash

# Script to clean, compile, and run the Java application using Maven

#mvn clean compile exec:java
mvn clean compile exec:java -Dexec.args="-Dprism.targetvram=2G -Xmx4G -Dprism.order=es2 -Dprism.forceGPU --add-opens javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED"


