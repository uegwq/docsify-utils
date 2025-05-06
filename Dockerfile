FROM maven:3.9.9
COPY . /app
ENTRYPOINT mvn package
