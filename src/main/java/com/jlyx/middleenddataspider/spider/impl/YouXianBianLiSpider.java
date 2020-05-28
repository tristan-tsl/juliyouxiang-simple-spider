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

@EnableSpider
@Component
public class YouXianBianLiSpider implements Spider {
    private Logger logger = LoggerFactory.getLogger(YouXianBianLiSpider.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String COMPETITION_ID = "you_xian_bian_li";
    private static final String COMPETITION_NAME = "广州千鲜汇企业管理有限公司";
    public static final String REFERER = "https://servicewechat.com";
    @Value("${spider.you_xian_bian_li.base_url}")
    private String base_url;
    @Value("${spider.you_xian_bian_li.tctoken}")
    private String tctoken;
    @Value("${spider.you_xian_bian_li.xparams}")
    private String xparams;


    public RestTemplate getRestTemplate() {
        List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new ArrayList<>();
        clientHttpRequestInterceptors.add((httpRequest, bytes, clientHttpRequestExecution) -> {
            HttpHeaders headers = httpRequest.getHeaders();
            headers.add("referer", REFERER);
            headers.add("tctoken", tctoken);
            headers.add("xparams", xparams);
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
            Map<String, String> requestDataMap = new HashMap<>();
            requestDataMap.put("topic_id", "1275888");
            requestDataMap.put("pickup_id", "16382");
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(base_url + "/topic/base_info_v2?topic_id={topic_id}&pickup_id={pickup_id}", String.class, requestDataMap);
            String responseEntityContent = responseEntity.getBody();
            logger.info("responseEntityContent = " + responseEntityContent);
            JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
            if (!"ok".equals(parse.get("result"))) {
                throw CustomException.build();
            }
            JSONObject rows = ((JSONObject) parse.get("rows"));
            return ((JSONArray) rows.get("categoryInfo")).toJavaList(JSONObject.class);
        } catch (Exception e) {
            logger.error("", e);
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
                int page_value = 1;
                int NUM_VALUE = 20;
                while (true) {
                    HashMap<String, String> requestDataMap = new HashMap<>();
                    requestDataMap.put("id", "1275888");
                    requestDataMap.put("type", id);
                    requestDataMap.put("page_index", String.valueOf(page_value++));
                    requestDataMap.put("page_size", String.valueOf(NUM_VALUE));
                    RestTemplate restTemplate = getRestTemplate();
                    ResponseEntity<String> responseEntity = restTemplate.getForEntity(base_url + "/topic/page_goods?id={id}&type={type}&page_index={page_index}&page_size={page_size}", String.class, requestDataMap);
                    String responseEntityContent = responseEntity.getBody();
//                    logger.info("responseEntityContent = " + responseEntityContent);
                    JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
                    if (!"ok".equals(parse.get("result"))) {
                        throw CustomException.build();
                    }
                    JSONArray productArray = (JSONArray) parse.get("rows");
                    for (int i = 0; i < productArray.size(); i++) {
                        JSONObject productObject = (JSONObject) productArray.get(i);
                        JSONObject resResultItem = new JSONObject();
                        resResultItem.put("classify_id", id);     // 分类id
                        resResultItem.put("classify_name", name); //分类名称
                        resResultItem.put("product_id", productObject.get("goods_id")); //商品ID
                        resResultItem.put("product_name", productObject.get("goods_name")); //商品名称
                        float price = Float.parseFloat(productObject.get("price").toString());
                        resResultItem.put("product_price", price); //商品单价
                        resResultItem.put("product_surplus_stock", productObject.get("store_count")); //商品剩余库存

                        int goods_sales_sum = ((int) productObject.get("goods_sales_sum"));
                        resResultItem.put("product_sales_volume", goods_sales_sum);// 销售量
                        resResultItem.put("product_sales", goods_sales_sum * price);// 销售额


                        logger.info("resResultItem = " + resResultItem);
                        resResult.add(resResultItem);
                    }
                    if (!((boolean) parse.get("more_page"))) {
                        break;
                    }
                    logger.info("还有更多数据,当前分类:" + id + "每页:" + NUM_VALUE + " 第" + page_value + "页");
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            logger.error("", e);
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

            String sql = "INSERT INTO `middleend_data_spider_customer_product` (" +
                    "`competition_id`,`competition_name`,`classify_id`," +
                    " `classify_name`,`product_id`, `product_name`," +
                    " `product_price`, `product_surplus_stock`" +
                    " , `product_sales_volume`, `product_sales`" +
                    ") VALUES (?,?,?,?,?,?,?,?,?,?);";
            jdbcTemplate.update(sql, COMPETITION_ID, COMPETITION_NAME,
                    customer_product_item.get("classify_id"), customer_product_item.get("classify_name")
                    , customer_product_item.get("product_id"), customer_product_item.get("product_name")
                    , customer_product_item.get("product_price"), customer_product_item.get("product_surplus_stock")
                    , customer_product_item.get("product_sales_volume"), customer_product_item.get("product_sales")
            );
        }
    }
}
