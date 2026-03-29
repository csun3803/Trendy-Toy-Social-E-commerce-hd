package com.example.trendytoysocialecommercehd.service;
// AlbumService.java - 接口

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.dto.AlbumDTO;
import com.example.trendytoysocialecommercehd.dto.AlbumDetailDTO;
import com.example.trendytoysocialecommercehd.entity.TechnicalIpAlbum;

import java.util.List;

public interface AlbumService extends IService<TechnicalIpAlbum> {
    List<AlbumDTO> getAlbumList();
    AlbumDetailDTO getAlbumDetail(String albumId);
    List<AlbumDTO> searchAlbums(String keyword);
    List<AlbumDTO> getHotAlbums();
}
