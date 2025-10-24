FROM ckulka/baikal:latest

COPY --chown=101:101 config/ /var/www/baikal/config/
COPY --chown=101:101 Specific/ /var/www/baikal/Specific/
COPY --chown=101:101 db/ /var/www/baikal/db/

LABEL purpose="Pre-Configured Baikal for Integration Tests"
