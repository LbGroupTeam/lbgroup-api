# ==================================
# STAGE 1: FASE DE COMPILAÇÃO (BUILD)
# Esta etapa usa o JDK e o Maven para compilar o código.
# ==================================
# Usamos o Eclipse Temurin com JDK 21 e Maven como imagem base.
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia os arquivos de configuração do Maven primeiro para cache de dependências
# Isso é uma prática recomendada para aproveitar o cache do Docker (se pom.xml não mudar, não precisa baixar dependências de novo)
COPY pom.xml .

# Copia o código-fonte
COPY src ./src

# Compila o projeto (cria o JAR na pasta target)
RUN mvn package -DskipTests

# ==================================
# STAGE 2: FASE DE EXECUÇÃO (RUNTIME)
# Esta etapa usa apenas o JRE para executar o JAR final.
# ==================================
# Usamos o JRE 21 mais leve (ex: Alpine) para a imagem final.
FROM eclipse-temurin:21-jre-alpine

# Define o diretório de trabalho
WORKDIR /app

# Cria um usuário e grupo não-root para rodar a aplicação por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia o JAR compilado da fase 'build' para esta fase de 'runtime'
# Você precisa ajustar 'nome-do-seu-arquivo.jar' para o nome exato que seu build Maven gera.
# Geralmente é algo como 'java-api-1.0.0.jar' ou 'api.jar'
ARG JAR_FILE=target/*.jar
COPY --from=build /app/${JAR_FILE} app.jar

# Expõe a porta que sua aplicação usa (ajuste se for diferente de 8080)
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
