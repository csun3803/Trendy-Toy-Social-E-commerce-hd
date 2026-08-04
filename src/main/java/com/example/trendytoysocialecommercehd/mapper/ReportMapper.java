package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    @Select("SELECT COUNT(*) FROM report WHERE target_type = #{targetType} AND target_id = #{targetId} AND status = 'PENDING'")
    int countPendingReports(@Param("targetType") String targetType, @Param("targetId") String targetId);
}
