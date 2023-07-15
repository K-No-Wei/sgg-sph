package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper, BaseCategoryTrademark> implements BaseCategoryTrademarkService {

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    /**
     * 根据三级分类获取品牌
     *
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id", category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            List<Long> tradeMarkList = baseCategoryTrademarkList.stream()
                    .map(baseCategoryTrademark -> baseCategoryTrademark.getTrademarkId())
                    .collect(Collectors.toList());
            return baseTrademarkMapper.selectBatchIds(tradeMarkList);
        }
        return null;
    }

    /**
     * 保存分类与品牌关联
     *
     * @param categoryTrademarkVo
     */
    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (!CollectionUtils.isEmpty(trademarkIdList)) {
            List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream()
                    .map(trademarkId -> {
                        BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                        baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                        baseCategoryTrademark.setTrademarkId(trademarkId);
                        return baseCategoryTrademark;
                    })
                    .collect(Collectors.toList());
            saveBatch(baseCategoryTrademarkList);
        }
    }

    /**
     * 获取当前未被三级分类关联的所有品牌
     *
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id", category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)) {
            //获取所有有关的id
            List<Long> tradeMarkList = baseCategoryTrademarkList.stream()
                    .map(baseCategoryTrademark -> baseCategoryTrademark.getTrademarkId())
                    .collect(Collectors.toList());

            List<BaseTrademark> trademarkList = baseTrademarkMapper.selectList(null)
                    .stream()
                    .filter(baseTrademark -> !tradeMarkList.contains(baseTrademark.getId()))
                    .collect(Collectors.toList());
            return trademarkList;

        }
        return baseTrademarkMapper.selectList(null);
    }

    /**
     * 删除关联
     *
     * @param category3Id
     * @param trademarkId
     */
    @Override
    public void remove(Long category3Id, Long trademarkId) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id", category3Id);
        queryWrapper.eq("trademark_id", trademarkId);

        baseCategoryTrademarkMapper.delete(queryWrapper);
    }
}
