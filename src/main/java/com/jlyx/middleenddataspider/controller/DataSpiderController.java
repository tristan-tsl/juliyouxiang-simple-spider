package com.jlyx.middleenddataspider.controller;

import com.jlyx.middleenddataspider.spider.impl.ShixhSpider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dataSpider")
public class DataSpiderController {

    @Autowired
    ShixhSpider spider;

    @RequestMapping("/moni")
    public String moni() {
        spider.doSpider();
        return "OK";
    }
}
