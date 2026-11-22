# Registry: docker.jozanapp.ir/jozan/school-backend-api:latest
FROM eclipse-temurin:21-jdk
EXPOSE 8080
COPY target/school-backend-api-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
