# Estagio 1: build com JDK 17 e Maven Wrapper
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Estagio 2: execucao com JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home heaven
COPY --from=build /app/target/heaven.jar app.jar
USER heaven
# A plataforma define PORT; o padrao da aplicacao e 8080.
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
