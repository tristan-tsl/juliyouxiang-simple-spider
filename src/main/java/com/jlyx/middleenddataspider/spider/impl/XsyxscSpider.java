package com.jlyx.middleenddataspider.spider.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jlyx.middleenddataspider.metadata.EnableSpider;
import com.jlyx.middleenddataspider.model.CustomException;
import com.jlyx.middleenddataspider.spider.Spider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EnableSpider
@Component
public class XsyxscSpider implements Spider {
    private Logger logger = LoggerFactory.getLogger(XsyxscSpider.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String COMPETITION_ID = "xsyxsc";
    private static final String COMPETITION_NAME = "湖南兴盛优选电子商务有限公司";
    public static final String REFERER = "https://servicewechat.com/wx6025c5470c3cb50c/98/page-frame.html";
    @Value("${spider.xsyxsc.base_url}")
    private String base_url;
    @Value("${spider.xsyxsc.store_id}")
    private String store_id;
    @Value("${spider.xsyxsc.area_id}")
    private String area_id;

    public RestTemplate getRestTemplate() {
        List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new ArrayList<>();
        clientHttpRequestInterceptors.add((httpRequest, bytes, clientHttpRequestExecution) -> {
            HttpHeaders headers = httpRequest.getHeaders();
            headers.add("referer", REFERER);
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        });
        restTemplate.setInterceptors(clientHttpRequestInterceptors);
        return restTemplate;
    }

    @Override
    public void doSpider() {
        logger.info("目标为" + COMPETITION_NAME);
        List<JSONObject> customer_classify = spider_customer_classify(); // middleend_data_spider_customer_classify
        logger.info("customer_classify = " + customer_classify);
        store_customer_classify(customer_classify);
//
        List<JSONObject> customer_product = spider_customer_product(customer_classify);    // middleend_data_spider_customer_product
        logger.info("customer_product = " + customer_product);
        store_customer_product(customer_product);
    }


    /**
     * 商品分类
     *
     * @return
     */
    private List<JSONObject> spider_customer_classify() {
        logger.info("爬取商品分类数据................开始");
        try {
//            HashMap<String, String> requestData = new HashMap<>();
//            requestData.put("page_type", "1");
//            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/user/product/indexWindows?storeId=" + store_id + "&areaId=" + area_id + "&openBrandHouse=OPEN&userKey=", "", String.class);
            String responseEntityContent = responseEntity.getBody();
            logger.info("responseEntityContent = " + responseEntityContent);
            JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
            if (!"success".equals(parse.get("rspCode"))) {
                throw CustomException.build();
            }
            JSONObject rows = ((JSONObject) parse.get("data"));

            return ((JSONArray) rows.get("brandHouseWindows")).toJavaList(JSONObject.class).stream().map(t -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", t.get("brandWindowId"));
                jsonObject.put("name", t.get("windowName"));
                return jsonObject;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("",e);
        }
        logger.info("爬取商品分类数据................结束");
        return null;
    }

    /**
     * 商品
     *
     * @return
     */
    private List<JSONObject> spider_customer_product(List<JSONObject> customer_classify) {
        logger.info("爬取商品数据................开始");
        ArrayList<JSONObject> resResult = new ArrayList<>();
        try {
            for (JSONObject customer_classify_item : customer_classify) {
                String id = customer_classify_item.get("id").toString();
                String name = (String) customer_classify_item.get("name");
                logger.info("id = " + id + " name = " + name);
                // request
                int page_value = 0;
                final int NUM_VALUE = 8;
                while (true) {
                    page_value++;
//                HashMap<String, String> requestDataMap = new HashMap<>();
//                requestDataMap.put("pid", id);
//                requestDataMap.put("page_index", String.valueOf(page_value++));
//                requestDataMap.put("page_size", String.valueOf(NUM_VALUE));
                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/user/brandhouse/window/getProducts?windowId=" + id + "&areaId=101&storeId=66880000048883&pageIndex=" + page_value + "&pageSize=" + NUM_VALUE + "&excludeAct=N&userKey=", "", String.class);
                    String responseEntityContent = responseEntity.getBody();
//                    logger.info("responseEntityContent = " + responseEntityContent);
                    JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
                    if (!"success".equals(parse.get("rspCode"))) {
                        throw CustomException.build();
                    }
                    JSONObject data = ((JSONObject) parse.get("data"));
                    JSONArray records = (JSONArray) data.get("records");
                    for (int i = 0; i < records.size(); i++) {
                        JSONObject records_item = (JSONObject) records.get(i);
                        JSONObject resResultItem = new JSONObject();
                        resResultItem.put("classify_id", id);     // 分类id
                        resResultItem.put("classify_name", name); //分类名称
                        resResultItem.put("product_id", records_item.get("prId")); //商品ID
                        resResultItem.put("product_name", records_item.get("prName")); //商品名称
                        resResultItem.put("product_price", records_item.get("saleAmt")); //商品单价
                        resResultItem.put("product_surplus_stock", records_item.get("limitQty")); //商品剩余库存
                        logger.info("resResultItem = " + resResultItem);
                        resResult.add(resResultItem);
                    }
                    int total = (int) data.get("total");
                    int current = (int) data.get("current");
                    if (current * NUM_VALUE > total) {
                        break;
                    }
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            logger.error("",e);
        }
        logger.info("爬取商品数据................结束");
        return resResult;
    }

    private void store_customer_classify(List<JSONObject> customer_classify) {
        for (int i = 0; i < customer_classify.size(); i++) {
            JSONObject customer_classify_item = customer_classify.get(i);
            String sql = "INSERT INTO `middleend_data_spider_customer_classify` (`competition_id`,`competition_name`,`classify_id`, `classify_name`) VALUES (?,?,?, ?);";
            jdbcTemplate.update(sql, COMPETITION_ID, COMPETITION_NAME, customer_classify_item.get("id"), customer_classify_item.get("name"));
        }
    }

    private void store_customer_product(List<JSONObject> customer_product) {
        for (int i = 0; i < customer_product.size(); i++) {
            JSONObject customer_product_item = customer_product.get(i);

            String sql = "INSERT INTO `middleend_data_spider_customer_product` (`competition_id`,`competition_name`,`classify_id`, `classify_name`,`product_id`, `product_name`, `product_price`, `product_surplus_stock`) VALUES (?,?,?,?,?,?,?,?);";
            jdbcTemplate.update(sql, COMPETITION_ID, COMPETITION_NAME,
                    customer_product_item.get("classify_id"), customer_product_item.get("classify_name"),
                    customer_product_item.get("product_id"), customer_product_item.get("product_name"),
                    customer_product_item.get("product_price"), customer_product_item.get("product_surplus_stock")
            );
        }
    }
}
