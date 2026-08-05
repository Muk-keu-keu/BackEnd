# CI 에서 ./gradlew clean build 로 만든 jar 만 담는다.
# 이미지 안에서 다시 빌드하지 않으므로 푸시가 빠르고 이미지도 가볍다.
FROM eclipse-temurin:21-jre

WORKDIR /app

# orderedAt / createdAt 이 LocalDateTime.now() 라 컨테이너 시간대가 그대로 찍힌다.
ENV TZ=Asia/Seoul

# build.gradle 에서 plain jar 를 껐으므로 여기 걸리는 건 실행 가능한 jar 하나뿐이다.
COPY build/libs/*.jar app.jar

EXPOSE 8700

# 컨테이너 메모리 한도를 기준으로 힙을 잡는다. -Xmx 를 고정하면 인스턴스를 바꿀 때마다 손봐야 한다.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
