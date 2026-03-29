package com.example.trendytoysocialecommercehd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@MapperScan("com.example.trendytoysocialecommercehd.mapper")
public class TrendyToySocialECommerceHdApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrendyToySocialECommerceHdApplication.class, args);
    }

}
