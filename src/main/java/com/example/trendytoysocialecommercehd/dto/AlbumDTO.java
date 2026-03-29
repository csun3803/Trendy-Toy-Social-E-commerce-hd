package com.example.trendytoysocialecommercehd.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlbumDTO {
    private String id;
    private String name;
    private String img;
    private String shortDescription;
    private String copyrightOwner;
    private String creator;
    private String countryOrigin;
    private Boolean isHotIp;
    private LocalDateTime creationTime;
}


