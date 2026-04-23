package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.MyDisplayCabinet;
import com.example.trendytoysocialecommercehd.entity.CabinetItem;
import com.example.trendytoysocialecommercehd.mapper.MyDisplayCabinetMapper;
import com.example.trendytoysocialecommercehd.mapper.CabinetItemMapper;
import com.example.trendytoysocialecommercehd.service.CabinetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CabinetServiceImpl extends ServiceImpl<MyDisplayCabinetMapper, MyDisplayCabinet> implements CabinetService {

    @Autowired
    private MyDisplayCabinetMapper cabinetMapper;

    @Autowired
    private CabinetItemMapper cabinetItemMapper;

    @Override
    public List<MyDisplayCabinet> getCabinetsByUserId(String userId) {
        List<MyDisplayCabinet> cabinets = cabinetMapper.selectByUserId(userId);
        for (MyDisplayCabinet cabinet : cabinets) {
            cabinet.setTotalItems(cabinetMapper.countItemsByCabinetId(cabinet.getCabinetId()));
        }
        return cabinets;
    }

    @Override
    public MyDisplayCabinet getCabinetDetail(String cabinetId) {
        MyDisplayCabinet cabinet = cabinetMapper.selectById(cabinetId);
        if (cabinet != null) {
            cabinet.setTotalItems(cabinetMapper.countItemsByCabinetId(cabinetId));
        }
        return cabinet;
    }

    @Override
    public List<CabinetItem> getCabinetItems(String cabinetId) {
        return cabinetItemMapper.selectByCabinetId(cabinetId);
    }

    @Override
    @Transactional
    public MyDisplayCabinet createCabinet(MyDisplayCabinet cabinet) {
        cabinet.setCabinetId("cab_" + UUID.randomUUID().toString().substring(0, 8));
        cabinet.setTotalItems(0);
        cabinet.setTotalValuation(BigDecimal.ZERO);
        cabinet.setViewCount(0);
        cabinet.setLikeCount(0);
        cabinet.setCommentCount(0);
        cabinet.setShareCount(0);
        cabinet.setCreatedAt(LocalDateTime.now());
        cabinetMapper.insert(cabinet);
        return cabinet;
    }

    @Override
    @Transactional
    public MyDisplayCabinet updateCabinet(String cabinetId, MyDisplayCabinet cabinet) {
        cabinet.setCabinetId(cabinetId);
        cabinetMapper.updateById(cabinet);
        return getCabinetDetail(cabinetId);
    }

    @Override
    @Transactional
    public boolean deleteCabinet(String cabinetId) {
        cabinetItemMapper.deleteByMap(Map.of("cabinet_id", cabinetId));
        return cabinetMapper.deleteById(cabinetId) > 0;
    }

    @Override
    @Transactional
    public CabinetItem addItemToCabinet(String cabinetId, CabinetItem item) {
        item.setItemId("item_cab_" + UUID.randomUUID().toString().substring(0, 8));
        item.setCabinetId(cabinetId);
        item.setAddedAt(LocalDateTime.now());
        cabinetItemMapper.insert(item);

        updateCabinetStats(cabinetId);
        return item;
    }

    @Override
    @Transactional
    public CabinetItem updateCabinetItem(String cabinetId, String itemId, CabinetItem item) {
        item.setItemId(itemId);
        item.setCabinetId(cabinetId);
        cabinetItemMapper.updateById(item);
        updateCabinetStats(cabinetId);
        return item;
    }

    @Override
    @Transactional
    public boolean removeItemFromCabinet(String cabinetId, String itemId) {
        boolean result = cabinetItemMapper.deleteById(itemId) > 0;
        if (result) {
            updateCabinetStats(cabinetId);
        }
        return result;
    }

    private void updateCabinetStats(String cabinetId) {
        List<CabinetItem> items = cabinetItemMapper.selectByCabinetId(cabinetId);
        int totalItems = items.size();
        BigDecimal totalValuation = items.stream()
                .filter(item -> item.getAcquisitionPrice() != null)
                .map(CabinetItem::getAcquisitionPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MyDisplayCabinet cabinet = new MyDisplayCabinet();
        cabinet.setCabinetId(cabinetId);
        cabinet.setTotalItems(totalItems);
        cabinet.setTotalValuation(totalValuation);
        cabinetMapper.updateById(cabinet);
    }
}