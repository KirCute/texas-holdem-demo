FROM openjdk:11-jre-slim
COPY backend/target/texas-holdem-demo-1.2.jar /data/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/data/app.jar"]