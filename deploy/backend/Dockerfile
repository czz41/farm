FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/openjdk:8-jdk-alpine
WORKDIR /app
COPY springboot-init-0.0.1-SNAPSHOT.jar app.jar
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]