FROM maven:3.9.9
COPY . /app
WORKDIR /app
RUN mvn package
