# ---------- 1) 建置 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 先只複製 pom，讓相依套件下載層可被快取
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- 2) 執行 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /build/target/terminal-report.jar app.jar
USER appuser
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Taipei -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
