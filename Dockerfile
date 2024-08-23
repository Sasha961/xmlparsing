FROM maven:3.9.5-amazoncorretto-17 AS builder
ADD ./ ./
RUN mvn clean package -DskipTests


From amazoncorretto:17
COPY --from=builder /target/xml-parsing-0.1.15.jar xml-parsing-0.1.15.jar
CMD ["java", "-jar","xml-parsing-0.1.15.jar"]