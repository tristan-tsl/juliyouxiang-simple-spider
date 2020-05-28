ALTER TABLE `jlyx_backstage_admin`.`middleend_data_spider_customer_product`
ADD COLUMN `product_sales_volume` int(20) NULL COMMENT '商品销售量' AFTER `create_time`,
    ADD COLUMN `product_sales` int(20) NULL COMMENT '商品销售额' AFTER `product_sales_volume`;