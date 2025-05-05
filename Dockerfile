FROM maven:3.9.9 AS MAVEN_TOOL_CHAIN
# TODO go to the correct path
COPY . /app
WORKDIR /app
RUN mvn clean package -X
