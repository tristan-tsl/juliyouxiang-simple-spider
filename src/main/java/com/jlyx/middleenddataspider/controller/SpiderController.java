package com.jlyx.middleenddataspider.controller;

import com.jlyx.middleenddataspider.init.InitSpiderEnabledList;
import com.jlyx.middleenddataspider.spider.Spider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/spider")
public class SpiderController {
    private Logger logger = LoggerFactory.getLogger(SpiderController.class);
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    @RequestMapping("/net_trigger")
    public String net_trigger() {
        for (Spider spider : InitSpiderEnabledList.enabledSpider) {
            executorService.execute(() -> {
                logger.info("\n\n爬取数据..........................................开始");
                try {
                    spider.doSpider();
                } catch (Exception e) {
                    logger.error("", e);
                }
                logger.info("\n\n爬取数据..........................................结束");
            });
        }
        return "success";
    }
}
