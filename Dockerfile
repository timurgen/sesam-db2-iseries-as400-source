FROM openjdk:13-slim
COPY ./target/sesam-db2-source-1.0-SNAPSHOT.jar /opt/sesam-db2-source-1.0-SNAPSHOT.jar

# RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 

ENTRYPOINT ["java"]
CMD ["-XX:MinRAMPercentage=50", "-XX:MaxRAMPercentage=80", "-verbose:gc", "-XshowSettings:vm", "-XX:+UnlockExperimentalVMOptions",  "-XX:+UseZGC", "-Xlog:gc*", "-jar", "/opt/sesam-db2-source-1.0-SNAPSHOT.jar"]
EXPOSE 8080:8080