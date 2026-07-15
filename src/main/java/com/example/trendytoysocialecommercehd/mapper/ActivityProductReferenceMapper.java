package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.entity.ActivityProductReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityProductReferenceMapper extends BaseMapper<ActivityProductReference> {

    @Select("SELECT r.reference_id, r.activity_id, r.series_id, " +
            "s.series_id AS s_series_id, s.series_name, s.ip_album_id, s.description, " +
            "s.release_year, s.theme, s.total_variants, s.regular_variants, s.hidden_variants, " +
            "s.fullset_price, s.cover_image, s.status, s.start_date, s.end_date, " +
            "s.is_limited, s.limited_quantity, s.series_hotness, s.min_price, " +
            "s.create_time, s.update_time " +
            "FROM activity_product_reference r " +
            "LEFT JOIN series s ON r.series_id = s.series_id " +
            "WHERE r.activity_id = #{activityId}")
    List<ActivityProductReference> selectSeriesByActivity(@Param("activityId") String activityId);

    @Select("SELECT r.reference_id, r.activity_id, r.series_id, " +
            "a.activity_id AS a_activity_id, a.user_id, a.activity_type, a.title, a.content, " +
            "a.cover_image, a.image_list, a.location, a.publish_status, a.audit_status, " +
            "a.audit_notes, a.auditor_id, a.audited_at, a.view_count, a.like_count, " +
            "a.comment_count, a.favorite_count, a.share_count, a.published_at, a.updated_at " +
            "FROM activity_product_reference r " +
            "LEFT JOIN social_activity a ON r.activity_id = a.activity_id " +
            "WHERE r.series_id = #{seriesId} " +
            "ORDER BY a.published_at DESC")
    List<ActivityProductReference> selectActivitiesBySeries(@Param("seriesId") String seriesId);
}