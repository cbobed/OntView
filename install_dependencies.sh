#!/bin/bash

# Script to install the necessary JAR files before launching the application

echo "Installing maven..."
apt-cache search maven
sudo apt-get install maven

echo "Installing pedviz_0.15.jar..."
mvn install:install-file -Dfile=lib/pedviz_0.15.jar -DgroupId=org.sid.ontview -DartifactId=pedviz -Dversion=0.15 -Dpackaging=jar

if [ $? -ne 0 ]; then
  echo "Error installing pedviz_0.15.jar"
  exit 1
fi

echo "Installing KCE-0.0.1-SNAPSHOT.jar..."
mvn install:install-file -Dfile=lib/kce/KCE-0.0.1-SNAPSHOT.jar -DgroupId=org.sid.ontview -DartifactId=kce -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar

if [ $? -ne 0 ]; then
  echo "Error installing KCE-0.0.1-SNAPSHOT.jar"
  exit 1
fi

echo "Initializing Git LFS..."
git lfs install

# echo "Fetching LFS files (tutorial video)..."
# git lfs pull

echo "Dependency installation completed."

