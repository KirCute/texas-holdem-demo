FROM openjdk:11-jre-slim
COPY backend/target/texas-holdem-java-1.1.jar /data/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/data/app.jar"]