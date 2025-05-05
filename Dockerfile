FROM maven:3.5.2-jdk-8-alpine AS MAVEN_TOOL_CHAIN
# TODO go to the correct path
COPY . /app
WORKDIR /app

RUN mvn clean package
RUN mvn clean package -X
