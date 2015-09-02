#!/bin/sh

mvn clean install
mvn -f mockserver/pom.xml package assembly:single

docker build -t mockserver .
