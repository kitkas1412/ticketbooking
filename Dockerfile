# syntax=docker/dockerfile:1

# Build stage: đóng gói module start (kèm các module nó phụ thuộc) bằng Maven Wrapper.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY . .

# Cache ~/.m2 giữa các lần build để không tải lại dependency. Test đã chạy ở CI nên bỏ qua.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -pl start -am -DskipTests

# Tách JAR thành các layer (dependency ít đổi, code đổi thường xuyên) để image đẩy lên nhanh hơn.
RUN java -Djarmode=tools -jar start/target/start-*.jar extract --layers --launcher --destination extracted

# Runtime stage: chỉ chứa JRE và ứng dụng, chạy bằng user không phải root.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app

# Cổng HTTP của ứng dụng trong container.
EXPOSE 8080

# Giới hạn heap theo bộ nhớ của container thay vì của máy host.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
