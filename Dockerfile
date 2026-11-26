FROM eclipse-temurin:25-jre-ubi10-minimal

WORKDIR /app
COPY target/stockflow-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
