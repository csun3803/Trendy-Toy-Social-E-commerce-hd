package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.dto.AlbumDTO;
import com.example.trendytoysocialecommercehd.dto.AlbumDetailDTO;
import com.example.trendytoysocialecommercehd.service.AlbumService;
import com.example.trendytoysocialecommercehd.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/album")
@Tag(name = "IP图鉴管理", description = "IP图鉴相关接口")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取IP图鉴列表", description = "获取所有已审核通过的IP列表")
    public Result<List<AlbumDTO>> getAlbumList() {
        List<AlbumDTO> albumList = albumService.getAlbumList();
        return Result.success(albumList);
    }

    @GetMapping("/detail/{albumId}")
    @Operation(summary = "获取IP详情", description = "根据ID获取IP详细信息")
    public Result<AlbumDetailDTO> getAlbumDetail(@PathVariable String albumId) {
        AlbumDetailDTO detail = albumService.getAlbumDetail(albumId);
        if (detail == null) {
            return Result.error("未找到该IP");
        }
        return Result.success(detail);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索IP", description = "根据关键词搜索IP")
    public Result<List<AlbumDTO>> searchAlbums(@RequestParam String keyword) {
        List<AlbumDTO> result = albumService.searchAlbums(keyword);
        return Result.success(result);
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门IP", description = "获取热门IP列表")
    public Result<List<AlbumDTO>> getHotAlbums() {
        List<AlbumDTO> hotAlbums = albumService.getHotAlbums();
        return Result.success(hotAlbums);
    }

    @GetMapping("/test")
    @Operation(summary = "测试接口", description = "用于前端开发测试")
    public Result<List<AlbumDTO>> getTestData() {
        // 模拟前端需要的测试数据格式
        List<AlbumDTO> testData = List.of(
                createTestDTO("1", "DIMOO", "/static/images/tj/1.jpg"),
                createTestDTO("2", "SKULLPANDA", "/static/images/tj/2.jpg"),
                createTestDTO("3", "Molly", "/static/images/tj/3.jpg"),
                createTestDTO("4", "Farmer Bob", "/static/images/tj/4.jpg"),
                createTestDTO("5", "LABUBU", "/static/images/tj/5.jpg"),
                createTestDTO("6", "RiCO", "/static/images/tj/6.jpg")
        );
        return Result.success(testData);
    }

    private AlbumDTO createTestDTO(String id, String name, String img) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setImg(img);
        return dto;
    }
}
