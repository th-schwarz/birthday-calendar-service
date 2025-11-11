FROM ghcr.io/kozea/radicale:latest

COPY --chown=1000:1000 config/ /etc/radicale/
COPY --chown=1000:1000 data/ /data/

LABEL purpose="Pre-Configured radicale for Integration Tests"