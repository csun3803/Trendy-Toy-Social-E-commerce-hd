package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.AlbumDTO;
import com.example.trendytoysocialecommercehd.dto.AlbumDetailDTO;
import com.example.trendytoysocialecommercehd.dto.SeriesDTO;
import com.example.trendytoysocialecommercehd.dto.SeriesDetailDTO;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.entity.Series;
import com.example.trendytoysocialecommercehd.entity.TechnicalIpAlbum;
import com.example.trendytoysocialecommercehd.service.AlbumService;
import com.example.trendytoysocialecommercehd.service.SeriesService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import com.example.trendytoysocialecommercehd.mapper.SeriesMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/album")
@Tag(name = "管理端-图鉴管理", description = "管理端图鉴相关接口")
public class AlbumAdminController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private SeriesService seriesService;

    @Autowired
    private SeriesMapper seriesMapper;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/list")
    @Operation(summary = "获取所有IP列表", description = "管理员获取所有IP列表（包含未审核）")
    public Result<List<AlbumDTO>> getAllAlbums() {
        List<TechnicalIpAlbum> albums = albumService.list();
        List<AlbumDTO> dtoList = albums.stream().map(album -> {
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
        }).toList();
        return Result.success(dtoList);
    }

    @GetMapping("/detail/{albumId}")
    @Operation(summary = "获取IP详情", description = "获取IP详情及系列统计")
    public Result<Map<String, Object>> getAlbumDetail(@PathVariable String albumId) {
        TechnicalIpAlbum album = albumService.getById(albumId);
        if (album == null) {
            return Result.error("未找到该IP");
        }

        QueryWrapper<Series> wrapper = new QueryWrapper<>();
        wrapper.eq("ip_album_id", albumId);
        List<Series> seriesList = seriesService.list(wrapper);

        int seriesCount = seriesList.size();
        int totalVariants = seriesList.stream()
                .mapToInt(s -> s.getTotalVariants() != null ? s.getTotalVariants() : 0)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("albumId", album.getAlbumId());
        result.put("ipName", album.getIpName());
        result.put("shortDescription", album.getShortDescription());
        result.put("copyrightOwner", album.getCopyrightOwner());
        result.put("logo", album.getLogo());
        result.put("creationTime", album.getCreationTime());
        result.put("creator", album.getCreator());
        result.put("countryOrigin", album.getCountryOrigin());
        result.put("isHotIp", album.getIsHotIp());
        result.put("auditStatus", album.getAuditStatus());
        result.put("seriesCount", seriesCount);
        result.put("totalVariants", totalVariants);

        return Result.success(result);
    }

    @GetMapping("/{albumId}/series")
    @Operation(summary = "获取IP下的系列列表", description = "获取指定IP下的所有系列")
    public Result<List<Map<String, Object>>> getAlbumSeries(@PathVariable String albumId) {
        QueryWrapper<Series> wrapper = new QueryWrapper<>();
        wrapper.eq("ip_album_id", albumId);
        List<Series> seriesList = seriesService.list(wrapper);

        List<Map<String, Object>> result = seriesList.stream().map(series -> {
            Map<String, Object> map = new HashMap<>();
            map.put("seriesId", series.getSeriesId());
            map.put("seriesName", series.getSeriesName());
            map.put("totalVariants", series.getTotalVariants());
            map.put("hiddenVariants", series.getHiddenVariants());
            map.put("regularVariants", series.getRegularVariants());
            map.put("coverImage", series.getCoverImage());
            map.put("status", series.getStatus());
            return map;
        }).toList();

        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "创建IP", description = "创建新的IP")
    public Result<TechnicalIpAlbum> createAlbum(@RequestBody TechnicalIpAlbum album) {
        if (album.getAlbumId() == null || album.getAlbumId().isEmpty()) {
            album.setAlbumId("album_" + UUID.randomUUID().toString().substring(0, 8));
        }
        boolean success = albumService.save(album);
        if (success) {
            return Result.success(album);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/{albumId}")
    @Operation(summary = "更新IP", description = "更新IP信息")
    public Result<TechnicalIpAlbum> updateAlbum(@PathVariable String albumId, @RequestBody TechnicalIpAlbum album) {
        album.setAlbumId(albumId);
        boolean success = albumService.updateById(album);
        if (success) {
            return Result.success(album);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{albumId}")
    @Operation(summary = "删除IP", description = "删除IP及其下属系列")
    public Result<Void> deleteAlbum(@PathVariable String albumId) {
        QueryWrapper<Series> wrapper = new QueryWrapper<>();
        wrapper.eq("ip_album_id", albumId);
        long seriesCount = seriesService.count(wrapper);
        if (seriesCount > 0) {
            return Result.error("该IP下存在系列，请先删除系列");
        }
        boolean success = albumService.removeById(albumId);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }

    @PostMapping("/{albumId}/series")
    @Operation(summary = "创建系列", description = "在指定IP下创建系列")
    public Result<Series> createSeries(@PathVariable String albumId, @RequestBody Series series) {
        if (series.getSeriesId() == null || series.getSeriesId().isEmpty()) {
            series.setSeriesId("series_" + UUID.randomUUID().toString().substring(0, 8));
        }
        series.setIpAlbumId(albumId);
        boolean success = seriesService.save(series);
        if (success) {
            return Result.success(series);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/series/{seriesId}")
    @Operation(summary = "更新系列", description = "更新系列信息")
    public Result<Series> updateSeries(@PathVariable String seriesId, @RequestBody Series series) {
        series.setSeriesId(seriesId);
        boolean success = seriesService.updateById(series);
        if (success) {
            return Result.success(series);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/series/{seriesId}")
    @Operation(summary = "删除系列", description = "删除系列及其下属款式")
    public Result<Void> deleteSeries(@PathVariable String seriesId) {
        long productCount = productMapper.selectCount(
                new QueryWrapper<Product>().eq("series_id", seriesId)
        );
        if (productCount > 0) {
            return Result.error("该系列下存在款式，请先删除款式");
        }
        boolean success = seriesService.removeById(seriesId);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }

    @GetMapping("/series/{seriesId}/products")
    @Operation(summary = "获取系列下的款式列表", description = "获取指定系列下的所有款式")
    public Result<List<Map<String, Object>>> getSeriesProducts(@PathVariable String seriesId) {
        List<Map<String, Object>> products = productMapper.selectProductsBySeriesId(seriesId).stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", p.getProductId());
            map.put("productName", p.getProductName());
            map.put("price", p.getPrice());
            map.put("stock", p.getStock());
            map.put("variantType", p.getVariantType());
            map.put("rarity", p.getRarity());
            map.put("productImages", p.getProductImages());
            map.put("status", p.getStatus());
            map.put("sortOrder", p.getSortOrder());
            return map;
        }).toList();
        return Result.success(products);
    }

    @GetMapping("/series/detail/{seriesId}")
    @Operation(summary = "获取系列详情", description = "获取系列详细信息")
    public Result<Map<String, Object>> getSeriesDetail(@PathVariable String seriesId) {
        Series series = seriesService.getById(seriesId);
        if (series == null) {
            return Result.error("系列不存在");
        }

        TechnicalIpAlbum album = albumService.getById(series.getIpAlbumId());

        Map<String, Object> result = new HashMap<>();
        result.put("seriesId", series.getSeriesId());
        result.put("seriesName", series.getSeriesName());
        result.put("ipAlbumId", series.getIpAlbumId());
        result.put("ipName", album != null ? album.getIpName() : null);
        result.put("theme", series.getTheme());
        result.put("description", series.getDescription());
        result.put("coverImage", series.getCoverImage());
        result.put("regularVariants", series.getRegularVariants());
        result.put("hiddenVariants", series.getHiddenVariants());
        result.put("totalVariants", series.getTotalVariants());
        result.put("isLimited", series.getIsLimited());
        result.put("status", series.getStatus());
        result.put("minPrice", series.getMinPrice());
        result.put("fullsetPrice", series.getFullsetPrice());

        return Result.success(result);
    }

    @PostMapping("/series/{seriesId}/product")
    @Operation(summary = "创建款式", description = "在指定系列下创建款式")
    public Result<Product> createProduct(@PathVariable String seriesId, @RequestBody Product product) {
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            product.setProductId("prod_" + UUID.randomUUID().toString().substring(0, 8));
        }
        product.setSeriesId(seriesId);
        int insert = productMapper.insert(product);
        if (insert > 0) {
            return Result.success(product);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/product/{productId}")
    @Operation(summary = "更新款式", description = "更新款式信息")
    public Result<Product> updateProduct(@PathVariable String productId, @RequestBody Product product) {
        product.setProductId(productId);
        int update = productMapper.updateById(product);
        if (update > 0) {
            return Result.success(product);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "删除款式", description = "删除款式")
    public Result<Void> deleteProduct(@PathVariable String productId) {
        int delete = productMapper.deleteById(productId);
        if (delete > 0) {
            return Result.success();
        }
        return Result.error("删除失败");
    }
}