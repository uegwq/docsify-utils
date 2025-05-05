FROM maven:4.0.0-jdk-17-alpine AS MAVEN_TOOL_CHAIN
# TODO go to the correct path
COPY . /app
WORKDIR /app
RUN echo $(ls)
RUN mvn clean package -X
