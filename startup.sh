#!/usr/bin/env bash
echo "启动服务"
java -jar /app.jar ${CLOUD_EUREKA_DEFAULTZONE} ${JASYPT_ENCRYPTOR_PASSWORD}