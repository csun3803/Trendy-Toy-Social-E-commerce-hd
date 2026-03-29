package com.example.trendytoysocialecommercehd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录
        String projectRootPath = System.getProperty("user.dir");
        System.out.println("项目根目录: " + projectRootPath);

        // 配置静态资源路径 - 指向src/main/resources/static
        String staticPath = projectRootPath + File.separator + "src" + File.separator + "main" +
                File.separator + "resources" + File.separator + "static" + File.separator;

        System.out.println("静态资源路径: " + staticPath);

        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + staticPath)
                .addResourceLocations("classpath:/static/");

        // 额外配置图片路径
        String imagesPath = staticPath + "images" + File.separator;
        System.out.println("图片资源路径: " + imagesPath);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagesPath);
    }
}