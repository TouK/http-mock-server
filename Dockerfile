FROM java:8

ADD mockserver/target/mockserver-1.1.1-SNAPSHOT-jar-with-dependencies.jar /mockserver.jar

EXPOSE 9999

CMD java -jar /mockserver.jar
