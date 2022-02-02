FROM adoptopenjdk:11-jre-hotspot
COPY ./target/*.jar /app.jar

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
