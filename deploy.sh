#!/bin/bash
./mvnw clean package && \
docker build -f src/main/docker/Dockerfile.jvm -t taaja/redcat . && \
docker save -o taaja-redcat.tar taaja/redcat && \
scp taaja-redcat.tar taaja@taaja.io:/home/taaja && \
ssh taaja@taaja.io 'docker load --quiet --input /home/taaja/taaja-redcat.tar && cd /home/taaja/deployment/taaja && docker-compose up -d'