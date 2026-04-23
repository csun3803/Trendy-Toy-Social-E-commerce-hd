package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.entity.MyDisplayCabinet;
import com.example.trendytoysocialecommercehd.entity.CabinetItem;
import java.util.List;

public interface CabinetService extends IService<MyDisplayCabinet> {
    List<MyDisplayCabinet> getCabinetsByUserId(String userId);
    MyDisplayCabinet getCabinetDetail(String cabinetId);
    List<CabinetItem> getCabinetItems(String cabinetId);
    MyDisplayCabinet createCabinet(MyDisplayCabinet cabinet);
    MyDisplayCabinet updateCabinet(String cabinetId, MyDisplayCabinet cabinet);
    boolean deleteCabinet(String cabinetId);
    CabinetItem addItemToCabinet(String cabinetId, CabinetItem item);
    CabinetItem updateCabinetItem(String cabinetId, String itemId, CabinetItem item);
    boolean removeItemFromCabinet(String cabinetId, String itemId);
}