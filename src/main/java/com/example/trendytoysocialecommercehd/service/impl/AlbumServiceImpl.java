package com.example.trendytoysocialecommercehd.service.impl;

// AlbumServiceImpl.java - 实现类

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.dto.AlbumDTO;
import com.example.trendytoysocialecommercehd.dto.AlbumDetailDTO;
import com.example.trendytoysocialecommercehd.entity.TechnicalIpAlbum;
import com.example.trendytoysocialecommercehd.mapper.TechnicalIpAlbumMapper;
import com.example.trendytoysocialecommercehd.service.AlbumService;
import com.example.trendytoysocialecommercehd.dto.AlbumDTO;
import com.example.trendytoysocialecommercehd.dto.AlbumDetailDTO;
import com.example.trendytoysocialecommercehd.entity.TechnicalIpAlbum;
import com.example.trendytoysocialecommercehd.service.AlbumService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlbumServiceImpl extends ServiceImpl<TechnicalIpAlbumMapper, TechnicalIpAlbum>
        implements AlbumService {

    private final TechnicalIpAlbumMapper albumMapper;

    public AlbumServiceImpl(TechnicalIpAlbumMapper albumMapper) {
        this.albumMapper = albumMapper;
    }

    @Override
    public List<AlbumDTO> getAlbumList() {
        return albumMapper.selectAlbumList();
    }

    @Override
    public AlbumDetailDTO getAlbumDetail(String albumId) {
        TechnicalIpAlbum album = albumMapper.selectByAlbumId(albumId);
        if (album == null) {
            return null;
        }

        AlbumDetailDTO detailDTO = new AlbumDetailDTO();
        BeanUtils.copyProperties(album, detailDTO);
        return detailDTO;
    }

    @Override
    public List<AlbumDTO> searchAlbums(String keyword) {
        return albumMapper.selectList(null).stream()
                .filter(album -> album.getIpName().contains(keyword) ||
                        album.getShortDescription().contains(keyword))
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<AlbumDTO> getHotAlbums() {
        return albumMapper.selectAlbumList().stream()
                .filter(AlbumDTO::getIsHotIp)
                .toList();
    }

    private AlbumDTO convertToDTO(TechnicalIpAlbum album) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getAlbumId());
        dto.setName(album.getIpName());
        dto.setImg(album.getLogo());
        dto.setShortDescription(album.getShortDescription());
        dto.setCopyrightOwner(album.getCopyrightOwner());
        dto.setCreator(album.getCreator());
        dto.setCountryOrigin(album.getCountryOrigin());
        dto.setIsHotIp(album.getIsHotIp());
        dto.setCreationTime(album.getCreationTime());
        return dto;
    }
}
