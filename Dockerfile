# Build stage: biên dịch và đóng gói bằng Maven với JDK 21.
# Các đường dẫn COPY còn theo cấu trúc cũ, cần đồng bộ với dự án nhiều module.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY ../pom.xml ./

# Tải trước dependency để tận dụng Docker layer cache.
RUN mvn dependency:go-offline

COPY src src

RUN mvn clean package -DskipTests -B

# Runtime stage: chỉ chứa JRE và file JAR đã build.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Cổng HTTP của ứng dụng trong container.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

