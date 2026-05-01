FROM maven:3.9-eclipse-temurin-21-alpine As build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/auth-service-*.war app.war

EXPOSE  8843 8080

ENTRYPOINT [ "java", "-jar", "app.war" ]
