FROM java:8

ADD mockserver/target/mockserver-2.0.0-jar-with-dependencies.jar /mockserver.jar

EXPOSE 9999

CMD java -jar /mockserver.jar
