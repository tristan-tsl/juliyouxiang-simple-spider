package com.jlyx.middleenddataspider.jobhandler;

import com.jlyx.middleenddataspider.service.DataSpiderService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@JobHandler(value = "dataSpiderHandler")
@Component
public class DataSpiderHandler extends IJobHandler {

    private Logger logger = LoggerFactory.getLogger(DataSpiderHandler.class);

    @Autowired
    private DataSpiderService dataSpiderService;

    @Override
    public ReturnT<String> execute(String s) {
        try {

            long begin = System.currentTimeMillis();
            logger.info("call dataSpiderService...............begin");
            dataSpiderService.scrapingData();
            logger.info("call dataSpiderService run timer {} s, ThreadName:{}", ((System.currentTimeMillis() - begin) / 1000), Thread.currentThread().getName());
            return SUCCESS;
        } catch (Exception e) {
            logger.error("",e);
            return FAIL;
        }
    }
}
