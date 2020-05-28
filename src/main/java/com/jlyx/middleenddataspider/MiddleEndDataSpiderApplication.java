package com.jlyx.middleenddataspider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MiddleEndDataSpiderApplication {
    private static Logger logger = LoggerFactory.getLogger(MiddleEndDataSpiderApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MiddleEndDataSpiderApplication.class, args);
        logger.info("============ MIDDLE-END-DATA-SPIDER 系统启动成功 ===========");
    }

}
