#!/bin/sh

mvn -f mockserver/pom.xml clean package assembly:single

docker build -t mockserver .
