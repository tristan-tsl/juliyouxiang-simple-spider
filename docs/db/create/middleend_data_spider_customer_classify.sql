/*
 Navicat Premium Data Transfer

 Source Server         : dev-devlop
 Source Server Type    : MySQL
 Source Server Version : 50725
 Source Host           : rm-wz93sp9bir6ttq660co.mysql.rds.aliyuncs.com:3306
 Source Schema         : jlyx_backstage_admin

 Target Server Type    : MySQL
 Target Server Version : 50725
 File Encoding         : 65001

 Date: 11/12/2019 18:32:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for middleend_data_spider_customer_classify
-- ----------------------------
DROP TABLE IF EXISTS `middleend_data_spider_customer_classify`;
CREATE TABLE `middleend_data_spider_customer_classify`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `competition_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商家ID',
  `competition_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商家名称',
  `classify_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品分类ID',
  `classify_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品分类名称',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 165 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
