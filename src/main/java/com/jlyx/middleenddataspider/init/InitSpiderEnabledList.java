package com.jlyx.middleenddataspider.init;

import com.jlyx.middleenddataspider.metadata.EnableSpider;
import com.jlyx.middleenddataspider.spider.Spider;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Component
public class InitSpiderEnabledList implements CommandLineRunner, ApplicationContextAware {
    private volatile ApplicationContext applicationContext;
    public static List<Spider> enabledSpider = new ArrayList<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... strings) throws Exception {
        init();
    }

    public void init() {
        Collection<Spider> beanList = new LinkedList<>(this.applicationContext.getBeansOfType(Spider.class).values());
        for (Spider bean : beanList) {
            EnableSpider[] annotationsByType = bean.getClass().getAnnotationsByType(EnableSpider.class);
            if (annotationsByType == null || annotationsByType.length < 1) continue;
            enabledSpider.add(bean);
        }
    }
}
