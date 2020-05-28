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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableSpider
@Component
public class TudgoSpider implements Spider {
    private Logger logger = LoggerFactory.getLogger(TudgoSpider.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String COMPETITION_ID = "tudgo";
    private static final String COMPETITION_NAME = "深圳市土地公网络科技有限公司";

    @Value("${spider.tudgo.base_url}")
    private String base_url;


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
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/customer/classify/get_list", "", String.class);
            String responseEntityContent = responseEntity.getBody();
            logger.info("responseEntityContent = " + responseEntityContent);
            JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
            if (!"success".equals(parse.get("message"))) {
                throw CustomException.build();
            }
            return ((JSONArray) parse.get("result")).toJavaList(JSONObject.class);
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
                int NUM_VALUE = 100;
                while (true) {
                    HashMap<String, String> requestData = new HashMap<>();
                    requestData.put("classify_id", id);
                    requestData.put("page", String.valueOf(page_value++));
                    requestData.put("num", String.valueOf(NUM_VALUE));

                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/customer/product/product_list", requestData, String.class);
                    String responseEntityContent = responseEntity.getBody();
                    logger.info("responseEntityContent = " + responseEntityContent);
                    JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
                    if (!"success".equals(parse.get("message"))) {
                        throw CustomException.build();
                    }
                    if ("".equals(parse.get("result").toString())) {
                        break;
                    }
                    JSONArray productArray = (JSONArray) parse.get("result");
                    for (int i = 0; i < productArray.size(); i++) {
                        JSONObject productObject = (JSONObject) productArray.get(i);
                        JSONObject resResultItem = new JSONObject();
                        resResultItem.put("classify_id", id);     // 分类id
                        resResultItem.put("classify_name", name); //分类名称
                        String product_id = productObject.get("product_id").toString();
                        resResultItem.put("product_id", productObject.get("product_id")); //商品ID
                        resResultItem.put("product_name", productObject.get("name")); //商品名称
                        float price = Float.parseFloat(productObject.get("price").toString());
                        resResultItem.put("product_price", price); //商品单价
                        int surplus_stock = (int) productObject.get("surplus_stock");
                        resResultItem.put("product_surplus_stock", surplus_stock); //商品剩余库存

                        int goods_sales_sum = 0; //销售量
                        int product_sales = 0; //销售额
                        Map<String, String> last_product_surplus_stock_map = get_product_surplus_stock(product_id);
                        if (last_product_surplus_stock_map != null) {
                            int last_product_surplus_stock = Integer.valueOf(last_product_surplus_stock_map.get("product_surplus_stock"));
                            int last_product_sales_volume = Integer.valueOf(last_product_surplus_stock_map.get("product_sales_volume"));
                            // 得到本次库存差
                            int surplus_stock_diff = last_product_surplus_stock - surplus_stock;
                            System.out.println("surplus_stock = " + surplus_stock);
                            System.out.println("last_product_surplus_stock = " + last_product_surplus_stock);
                            if (surplus_stock_diff < 0) {
                                surplus_stock_diff = 0;
                            }
                            System.out.println(">>>>>>>>>>>>>>>surplus_stock_diff = " + surplus_stock_diff);
                            goods_sales_sum = surplus_stock_diff + last_product_sales_volume;
                        }
                        product_sales = (int) (goods_sales_sum * price);
                        resResultItem.put("product_sales_volume", goods_sales_sum);// 销售量
                        resResultItem.put("product_sales", product_sales);// 销售额

                        logger.info("resResultItem = " + resResultItem);
                        resResult.add(resResultItem);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        logger.info("爬取商品数据................结束");
        return resResult;
    }

    private Map<String, String> get_product_surplus_stock(String product_id) {
        try {
            String sql = "SELECT product_surplus_stock,product_sales_volume FROM middleend_data_spider_customer_product  WHERE competition_id = ? AND product_id = ? ORDER BY create_time DESC LIMIT 0,1;";
            Map<String, String> stringStringMap = jdbcTemplate.queryForObject(sql, new Object[]{COMPETITION_ID, product_id}, (resultSet, i) -> {
                Map<String, String> stringStringHashMap = new HashMap<>();
                int product_surplus_stock1 = resultSet.getInt("product_surplus_stock");
                int product_sales_volume = resultSet.getInt("product_sales_volume");
                stringStringHashMap.put("product_surplus_stock", String.valueOf(product_surplus_stock1));
                stringStringHashMap.put("product_sales_volume", String.valueOf(product_sales_volume));
                return stringStringHashMap;
            });
            return stringStringMap;
        } catch (Exception e) {
            logger.info("没有历史库存");
        }
        return null;
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
