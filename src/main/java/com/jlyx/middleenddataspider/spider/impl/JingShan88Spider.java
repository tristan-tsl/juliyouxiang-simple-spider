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

@EnableSpider
@Component
public class JingShan88Spider implements Spider {
    private Logger logger = LoggerFactory.getLogger(JingShan88Spider.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String COMPETITION_ID = "jingshan88";
    private static final String COMPETITION_NAME = "鲸山生物科技(广东)有限公司";
    public static final String REFERER = "https://servicewechat.com/wx656537ad11144b29/13/page-frame.html";
    @Value("${spider.jingshan88.base_url}")
    private String base_url;

    public RestTemplate getRestTemplate() {
        List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new ArrayList<>();
        clientHttpRequestInterceptors.add((httpRequest, bytes, clientHttpRequestExecution) -> {
            HttpHeaders headers = httpRequest.getHeaders();
            headers.add("referer", REFERER);
            headers.add("uniacid", "1");
            headers.add("openid", "o5muf4rapxW-d3gWM_X9MevVhCso");
            headers.add("version", "V1.9.9");
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
            HashMap<String, String> requestData = new HashMap<>();
            requestData.put("page_type", "1");
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/frontend/web/index.php/product/classification", requestData, String.class);
            String responseEntityContent = responseEntity.getBody();
            logger.info("responseEntityContent = " + responseEntityContent);
            JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
            if (!"success".equals(parse.get("message"))) {
                throw CustomException.build();
            }
            JSONObject rows = ((JSONObject) parse.get("extra"));
            return ((JSONArray) rows.get("data")).toJavaList(JSONObject.class);
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
//                int page_value = 1;
//                int NUM_VALUE = 20;
                HashMap<String, String> requestDataMap = new HashMap<>();
                requestDataMap.put("pid", id);
//                requestDataMap.put("page_index", String.valueOf(page_value++));
//                requestDataMap.put("page_size", String.valueOf(NUM_VALUE));
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/frontend/web/index.php/product/class_product", requestDataMap, String.class);
                String responseEntityContent = responseEntity.getBody();
//                    logger.info("responseEntityContent = " + responseEntityContent);
                JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
                if (((int) parse.get("code")) != 0) {
                    throw CustomException.build();
                }
                JSONArray extra = (JSONArray) parse.get("extra");
                for (int i = 0; i < extra.size(); i++) {
                    JSONObject extra_item = (JSONObject) extra.get(i);
                    JSONArray productArray = (JSONArray) extra_item.get("sub");
                    for (int j = 0; j < productArray.size(); j++) {
                        JSONObject productObject = (JSONObject) productArray.get(j);
                        JSONObject resResultItem = new JSONObject();
                        resResultItem.put("classify_id", id);     // 分类id
                        resResultItem.put("classify_name", name); //分类名称
                        resResultItem.put("product_id", productObject.get("id")); //商品ID
                        resResultItem.put("product_name", extra_item.get("title")+"-"+productObject.get("title")); //商品名称
                        float price = Float.parseFloat(productObject.get("price").toString());
                        resResultItem.put("product_price",price); //商品单价
                        resResultItem.put("product_surplus_stock", productObject.get("stock_number")); //商品剩余库存
                        // 销售量
                        int order_num_sum_sum = ((int) productObject.get("order_num"));
                        // 销售额
                        resResultItem.put("product_sales_volume", order_num_sum_sum);// 销售量
                        resResultItem.put("product_sales", order_num_sum_sum * price);// 销售额

                        logger.info("resResultItem = " + resResultItem);
                        resResult.add(resResultItem);
                    }
                }
                Thread.sleep(500);
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
