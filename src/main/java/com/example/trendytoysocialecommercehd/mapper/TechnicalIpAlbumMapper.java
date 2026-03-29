package com.example.trendytoysocialecommercehd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trendytoysocialecommercehd.dto.AlbumDTO;
import com.example.trendytoysocialecommercehd.entity.TechnicalIpAlbum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TechnicalIpAlbumMapper extends BaseMapper<TechnicalIpAlbum> {

    @Select("SELECT album_id as id, ip_name as name, logo as img, " +
            "short_description, copyright_owner, creator, " +
            "country_origin, is_hot_ip, creation_time " +
            "FROM TECHNICAL_IP_ALBUM " +
            "WHERE audit_status = '已通过' " +
            "ORDER BY is_hot_ip DESC, creation_time DESC")
    List<AlbumDTO> selectAlbumList();

    @Select("SELECT * FROM TECHNICAL_IP_ALBUM WHERE album_id = #{albumId}")
    TechnicalIpAlbum selectByAlbumId(String albumId);
}
