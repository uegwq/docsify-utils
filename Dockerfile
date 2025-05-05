FROM maven:3.9.9 AS MAVEN_TOOL_CHAIN
COPY . /app
WORKDIR /app
RUN mvn package
