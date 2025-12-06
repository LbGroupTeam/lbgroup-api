############################################
# 1) Build da aplicação (Maven)
############################################
FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /app

# Copia o arquivo pom.xml e baixa dependências
COPY pom.xml .
RUN mvn -B dependency:resolve dependency:resolve-plugins

# Copia todo o código fonte
COPY src ./src

# Compila e cria o JAR final
RUN mvn -B -DskipTests package


############################################
# 2) Runtime - imagem final otimizada
############################################
FROM eclipse-temurin:17-jre

WORKDIR /app

############################################
# Variáveis já preenchidas conforme solicitado
############################################
ENV INFOBIP_API_KEY="10575aebdbece683f7f7da0ce6645c0a-774d228c-a256-40ec-a4a6-e75d0b209368"
ENV GIT_SHA="9f60a1522b728e45473675cc6723a6b46e9daa0e"

LABEL info.infobip_key="10575aebdbece683f7f7da0ce6645c0a-774d228c-a256-40ec-a4a6-e75d0b209368"
LABEL info.git_sha="9f60a1522b728e45473675cc6723a6b46e9daa0e"

############################################
# Copia o JAR gerado
############################################
COPY --from=build /app/target/*.jar app.jar

# Porta padrão do Spring Boot
EXPOSE 8080

############################################
# Comando de execução
############################################
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
