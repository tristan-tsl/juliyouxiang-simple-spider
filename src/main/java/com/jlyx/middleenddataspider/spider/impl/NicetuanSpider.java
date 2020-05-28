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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EnableSpider
@Component
public class NicetuanSpider implements Spider {
    private Logger logger = LoggerFactory.getLogger(NicetuanSpider.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String COMPETITION_ID = "nicetuan";
    private static final String COMPETITION_NAME = "十荟团";
    public static final String REFERER = "https://servicewechat.com/wxbbdca62c011eeb38/180/page-frame.html";
    @Value("${spider.nicetuan.base_url}")
    private String base_url;
    @Value("${spider.nicetuan.ver}")
    private String ver;
    @Value("${spider.nicetuan.x_tingyun_id}")
    private String x_tingyun_id;
    @Value("${spider.nicetuan.token}")
    private String token;
    @Value("${spider.nicetuan.partner_id}")
    private String partner_id;
    @Value("${spider.nicetuan.groupon_id}")
    private String groupon_id;
    public RestTemplate getRestTemplate() {
        List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new ArrayList<>();

        clientHttpRequestInterceptors.add((httpRequest, bytes, clientHttpRequestExecution) -> {
            HttpHeaders headers = httpRequest.getHeaders();
            headers.add("referer", REFERER);
            headers.add("ver", ver);
            headers.add("x-tingyun-id", x_tingyun_id);
            headers.add("Authorization", "Bearer " + token);
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
            Map<String, String> requestData = new HashMap<>();
            requestData.put("partnerId", partner_id);
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/mc/diamond/list-diamond", requestData, String.class);
            String responseEntityContent = responseEntity.getBody();
            logger.info("responseEntityContent = " + responseEntityContent);
            JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
            if (((int) parse.get("code")) != 0) {
                throw CustomException.build();
            }

            return ((JSONArray) (((JSONObject) parse.get("data"))).get("diamondInfo")).toJavaList(JSONObject.class).stream().map(t -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", t.get("kingkongId"));
                jsonObject.put("name", t.get("title"));
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
                final int NUM_VALUE = 10;
                while (true) {
                    page_value++;
                    Map<String, String> requestDataMap = new HashMap<>();
                    requestDataMap.put("diamondId", id);
                    requestDataMap.put("grouponId", groupon_id);
                    requestDataMap.put("partnerId", partner_id);
                    requestDataMap.put("p", String.valueOf(page_value));
                    requestDataMap.put("size", String.valueOf(NUM_VALUE));
                    RestTemplate restTemplate = getRestTemplate();
                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/mc/diamond/list-merchandise", requestDataMap, String.class);
                    String responseEntityContent = responseEntity.getBody();
//                    logger.info("responseEntityContent = " + responseEntityContent);
                    JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
                    if (((int) parse.get("code")) != 0) {
                        throw CustomException.build();
                    }
                    JSONObject data = ((JSONObject) parse.get("data"));
                    JSONArray grouponMerchandiseList = (JSONArray) data.get("grouponMerchandiseList");
                    for (int i = 0; i < grouponMerchandiseList.size(); i++) {
                        JSONObject records_item = (JSONObject) grouponMerchandiseList.get(i);
                        JSONObject resResultItem = new JSONObject();
                        resResultItem.put("classify_id", id);     // 分类id
                        resResultItem.put("classify_name", name); //分类名称
                        resResultItem.put("product_id", records_item.get("merchandiseid")); //商品ID
                        resResultItem.put("product_name", records_item.get("title")+"-"+records_item.get("typecontent")); //商品名称
                        float price = Float.parseFloat(records_item.get("activityprice").toString());
                        resResultItem.put("product_price", records_item.get("activityprice")); //商品单价
                        resResultItem.put("product_surplus_stock", records_item.get("maxquantity")); //商品剩余库存


                        // 销售量
                        int order_num_sum_sum = ((int) records_item.get("waterQuantity"));
                        // 销售额
                        resResultItem.put("product_sales_volume", order_num_sum_sum);// 销售量
                        resResultItem.put("product_sales", order_num_sum_sum * price);// 销售额


                        logger.info("records_item = " + records_item);
                        logger.info("resResultItem = " + resResultItem);
                        resResult.add(resResultItem);
                    }
                    int totalPages = (int) data.get("totalPages");

                    if (page_value >= totalPages) {
                        break;
                    }
                    Thread.sleep(1000);
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
