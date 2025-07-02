# Estágio de build com Maven para compilar o backend Java
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar o código-fonte e os arquivos de configuração
COPY . .

# Executar o Maven para compilar o JAR (sem rodar os testes)
RUN mvn clean package -DskipTests

# Estágio de execução, baseado na imagem do OpenJDK
FROM openjdk:21-jdk-slim

WORKDIR /app

# Configuração do diretório de dados e permissões
RUN mkdir -p /app/data/spreadsheets && chmod -R 777 /app/data/spreadsheets

# Instalação de dependências para o Selenium e o Google Chrome
RUN apt-get update && \
    apt-get install -y wget curl unzip libgconf-2-4 libnss3 libxss1 libappindicator3-1 libasound2 fonts-liberation libgbm1 libvulkan1 xdg-utils && \
    wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    dpkg -i google-chrome-stable_current_amd64.deb && \
    apt-get install -f -y && \
    rm google-chrome-stable_current_amd64.deb

# Estágio do Selenium com o JAR copiado do estágio anterior
FROM selenium/standalone-chrome:latest

WORKDIR /app

# Copiar o JAR gerado no estágio de build
COPY --from=build /app/target/crawler-bancos-0.0.1-SNAPSHOT.jar /app/crawler-bancos.jar

# Expor a porta para o backend
EXPOSE 8080

# Comando para rodar o backend
CMD ["java", "-jar", "/app/crawler-bancos.jar"]

# Variável de ambiente para o diretório de planilhas
ENV SHEET_DOWNLOAD_DIR=/app/data/spreadsheets

# Cria o diretório de logs e garante permissões
RUN mkdir -p /app/logs && chmod -R 777 /app/logs

# Entrypoint do container
ENTRYPOINT ["java", "-jar", "/app/crawler-bancos.jar"]