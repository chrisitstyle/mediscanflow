FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon
RUN cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1)" app.jar


FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]