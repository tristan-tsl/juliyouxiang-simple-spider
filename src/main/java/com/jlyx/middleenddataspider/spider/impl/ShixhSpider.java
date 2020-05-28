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

import java.util.*;
import java.util.stream.Collectors;

@EnableSpider
@Component
public class ShixhSpider implements Spider {
    private Logger logger = LoggerFactory.getLogger(ShixhSpider.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String COMPETITION_ID = "shixh";
    private static final String COMPETITION_NAME = "食享会";
    public static final String REFERER = "https://servicewechat.com/wxafd63987bad96c1f/188/page-frame.html";
    @Value("${spider.shixh.base_url}")
    private String base_url;
    @Value("${spider.shixh.admin_shop_id}")
    private String admin_shop_id;
    @Value("${spider.shixh.biz_type}")
    private String biz_type;
    @Value("${spider.shixh.user_receive_id}")
    private String user_receive_id;

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
            requestData.put("adminShopId", admin_shop_id);
            requestData.put("bizType", biz_type);
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/product/getCategoryList", requestData, String.class);
            String responseEntityContent = responseEntity.getBody();
            logger.info("responseEntityContent = " + responseEntityContent);
            JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
            if (parse.containsKey("message")) {
                logger.error("", parse.get("message"));
                throw CustomException.build();
            }
            return (((JSONArray) parse.get("data"))).toJavaList(JSONObject.class).stream().map(t -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", t.get("categoryId"));
                jsonObject.put("categoryType", t.get("categoryType"));
                jsonObject.put("name", t.get("categoryName"));
                return jsonObject;
            }).collect(Collectors.toList());

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
                String categoryType = customer_classify_item.get("categoryType").toString();

                String name = (String) customer_classify_item.get("name");
                logger.info("id = " + id + " name = " + name);
                // request
                int page_value = 0;
                final int NUM_VALUE = 10;
                while (true) {
                    page_value++;
                    Map<String, String> requestDataMap = new HashMap<>();
                    requestDataMap.put("categoryId", id);
                    requestDataMap.put("categoryType", categoryType);
                    requestDataMap.put("adminShopId", admin_shop_id);
                    requestDataMap.put("bizType", biz_type);
                    requestDataMap.put("userReceiveId", user_receive_id);
                    requestDataMap.put("pageIndex", String.valueOf(page_value));
                    requestDataMap.put("pageSize", String.valueOf(NUM_VALUE));
                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/product/getProductList", requestDataMap, String.class);
                    String responseEntityContent = responseEntity.getBody();
//                    logger.info("responseEntityContent = " + responseEntityContent);
                    JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
                    if (parse.containsKey("message")) {
                        logger.error("", parse.get("message"));
                        throw CustomException.build();
                    }
                    JSONObject data = ((JSONObject) parse.get("data"));
                    JSONArray rows = (JSONArray) data.get("rows");
                    for (int i = 0; i < rows.size(); i++) {
                        JSONObject records_item = (JSONObject) rows.get(i);
                        logger.info("records_item = " + records_item);
                        Float product_price = Float.valueOf(records_item.get("salePrice").toString());
                        int total_product_sales_volume = 0; // 父商品销售量
                        int total_product_sales = 0;        // 父商品销售额
                        int total_product_stock = 0;        // 父商品库存量


                        if (records_item.containsKey("childProductDtos")) {
                            // 查看子商品
                            JSONArray childProductDtos = (JSONArray) records_item.get("childProductDtos");
                            for (JSONObject childProductDto : childProductDtos.toJavaList(JSONObject.class)) {
                                System.out.println("childProductDto = " + childProductDto);
                                JSONObject childResResultItem = new JSONObject();
                                childResResultItem.put("classify_id", id);     // 分类id
                                childResResultItem.put("classify_name", name); //分类名称
                                childResResultItem.put("product_id", childProductDto.get("prodId")); //商品ID
                                childResResultItem.put("product_name", childProductDto.get("prodName")); //商品名称
                                float child_price = Float.valueOf(childProductDto.get("salePrice").toString()); // 单价
                                childResResultItem.put("product_price", child_price); //商品单价

                                String sguId = childProductDto.get("sguId").toString();
                                String supplyNumber = childProductDto.get("supplyNumber").toString();
                                String skuCode = childProductDto.get("skuCode").toString();

                                Map<String, String> productStockAndSales = getProductStockAndSales(sguId, supplyNumber, skuCode);
                                int child_totalSales = Float.valueOf(productStockAndSales.get("totalSales")).intValue(); // 销售量
                                int child_sellableStock = Float.valueOf(productStockAndSales.get("sellableStock")).intValue(); // 库存量
                                total_product_sales_volume += child_totalSales;
                                total_product_sales += (int) (child_totalSales * child_price);
                                total_product_stock += child_sellableStock;


                                childResResultItem.put("product_surplus_stock", child_sellableStock); //商品剩余库存
                                childResResultItem.put("product_sales_volume", child_totalSales);// 销售量
                                childResResultItem.put("product_sales", child_totalSales * child_price);// 销售额
                                //销售量
                                //销售额
                                logger.info("resResultItem = " + childResResultItem);
                                resResult.add(childResResultItem);
                            }
                            // 结束查看子商品
                        } else {
                            String sguId = records_item.get("sguId").toString();
                            String supplyNumber = records_item.get("supplyNumber").toString();
                            String skuCode = records_item.get("skuCode").toString();

                            Map<String, String> productStockAndSales = getProductStockAndSales(sguId, supplyNumber, skuCode);
                            int totalSales = Float.valueOf(productStockAndSales.get("totalSales")).intValue(); // 销售量
                            int sellableStock = Float.valueOf(productStockAndSales.get("sellableStock")).intValue(); // 库存量
                            total_product_sales_volume = totalSales;
                            total_product_sales = (int) (totalSales * product_price);
                            total_product_stock = sellableStock;
                        }
                        JSONObject resResultItem = new JSONObject();
                        resResultItem.put("classify_id", id);     // 分类id
                        resResultItem.put("classify_name", name); //分类名称
                        resResultItem.put("product_id", records_item.get("prodId")); //商品ID
                        resResultItem.put("product_name", records_item.get("sguName")); //商品名称
                        resResultItem.put("product_price", product_price); //商品单价


                        resResultItem.put("product_surplus_stock", total_product_stock); //商品剩余库存
                        resResultItem.put("product_sales_volume", total_product_sales_volume);// 销售量
                        resResultItem.put("product_sales", total_product_sales);// 销售额
                        logger.info("resResultItem = " + resResultItem);
                        resResult.add(resResultItem);
                    }
                    int total = (int) data.get("total");

                    if (page_value * NUM_VALUE > total) {
                        break;
                    }
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        logger.info("爬取商品数据................结束");
        return resResult;
    }

    private Map<String, String> getProductStockAndSales(String sguId, String supplyNumber, String skuCode) throws Exception {
        HashMap<String, String> resResult = new HashMap<>();
        // 请求参数
        Map requestDataMap = new HashMap();
        // 核心请求参数

        JSONObject sguDtoList_item = new JSONObject();
        sguDtoList_item.put("sguId", sguId);
        JSONObject sguDetailDtoList_item = new JSONObject();
        sguDetailDtoList_item.put("skuCode", skuCode);
        sguDetailDtoList_item.put("supplierNumber", supplyNumber);
        sguDtoList_item.put("sguDetailDtoList", Arrays.asList(sguDetailDtoList_item));
        // 核心请求结束
        requestDataMap.put("sguDtoList", Arrays.asList(sguDtoList_item));
        requestDataMap.put("adminShopId", admin_shop_id);
        requestDataMap.put("bizType", biz_type);
        System.out.println("JSONObject.toJSONString(requestDataMap) = " + JSONObject.toJSONString(requestDataMap));
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(base_url + "/product/getLabelInfo", requestDataMap, String.class);
        String responseEntityContent = responseEntity.getBody();
        JSONObject parse = ((JSONObject) JSONObject.parse(responseEntityContent));
        if (parse.containsKey("message")) {
            logger.error("异常: ", parse.get("message"));
            throw CustomException.build();
        }
        JSONObject data = ((JSONObject) parse.get("data"));
        JSONArray laberInfoDtoList = (JSONArray) data.get("laberInfoDtoList");
        JSONObject laberInfoDtoListJO = (JSONObject) laberInfoDtoList.get(0);
        resResult.put("totalSales", laberInfoDtoListJO.get("totalSales").toString());
        resResult.put("sellableStock", laberInfoDtoListJO.get("sellableStock").toString());
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
