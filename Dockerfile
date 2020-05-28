FROM registry-vpc.cn-shenzhen.aliyuncs.com/jlyx/base_image-master:2790_201911271640
ADD target/*.jar app.jar
ADD startup.sh startup.sh
RUN bash -c 'touch app.jar'
RUN chmod 777 startup.sh
ARG IMAGE_PROJECT_TAG
ENV SW_AGENT_NAME ${IMAGE_PROJECT_TAG}
ENTRYPOINT ["./startup.sh"]