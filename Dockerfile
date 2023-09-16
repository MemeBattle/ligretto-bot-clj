FROM clojure:temurin-20-lein as builder

RUN mkdir /build

WORKDIR /build

COPY src src
COPY resources resources
COPY project.clj project.clj

RUN lein uberjar

FROM eclipse-temurin:20-jdk

RUN mkdir -p /opt/jmx_exporter
RUN wget -O /opt/jmx_exporter/jmx_prometheus_javaagent-0.16.1.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.16.1/jmx_prometheus_javaagent-0.16.1.jar
COPY jmx-config.yaml /opt/jmx_exporter/config.yaml

RUN mkdir /service

COPY entrypoint.sh /service/entrypoint.sh
COPY config.edn /service/config.edn

COPY --from=builder /build/target/app.jar /service/app.jar

EXPOSE 4201

ENTRYPOINT ["./service/entrypoint.sh"]
