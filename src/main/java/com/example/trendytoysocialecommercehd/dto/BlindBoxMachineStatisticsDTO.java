package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 抽盒机统计数据 DTO（用于商家端/管理员端数据页）
 */
@Data
public class BlindBoxMachineStatisticsDTO {

    /** 抽盒机ID */
    private String machineId;

    /** 抽盒机名称 */
    private String machineName;

    /** 总抽数 */
    private Integer totalDraws;

    /** 累计流水 */
    private BigDecimal totalRevenue;

    /** 参与用户数 */
    private Integer uniqueUsers;

    /** 保底触发次数 */
    private Integer guaranteedDraws;

    /** 各款式抽出统计 */
    private List<VariantDrawStat> variantStats;

    /**
     * 款式抽出统计
     */
    @Data
    public static class VariantDrawStat {
        /** 款式名称 */
        private String variantName;

        /** 款式图片 */
        private String variantImage;

        /** 是否为隐藏款 */
        private Boolean isHidden;

        /** 抽出次数 */
        private Integer drawCount;

        /** 抽出占比（0-1） */
        private BigDecimal drawRatio;
    }
}
