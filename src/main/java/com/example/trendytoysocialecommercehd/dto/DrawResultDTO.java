package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 抽盒结果DTO
 */
@Data
public class DrawResultDTO {

    /** 关联的订单ID */
    private String orderId;

    /** 订单编号 */
    private String orderNo;

    /** 抽盒总价格 */
    private BigDecimal totalPrice;

    /** 抽中的款式列表 */
    private List<DrawnItem> drawnItems;

    @Data
    public static class DrawnItem {
        /** 图鉴产品ID */
        private String variantId;

        /** 款式名称 */
        private String variantName;

        /** 款式图片 */
        private String variantImage;

        /** 是否隐藏款 */
        private Boolean isHidden;

        /** 是否保底 */
        private Boolean isGuaranteed;

        /** 单价 */
        private BigDecimal price;
    }
}
