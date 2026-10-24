FROM prom/node-exporter:v1.12.1 AS exporter

FROM ubuntu:24.04

COPY --from=exporter /bin/node_exporter /usr/local/bin/node_exporter

RUN chmod +x /usr/local/bin/node_exporter

EXPOSE 9100

CMD ["/usr/local/bin/node_exporter"]