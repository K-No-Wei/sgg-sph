package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {



        Map<String, Object> result = new HashMap<>();

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        if (!bloomFilter.contains(skuId)) {
//            return result;
//        }

        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(new Supplier<SkuInfo>() {
            @Override
            public SkuInfo get() {
                //  获取到的数据是skuInfo + skuImageList
                SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
                result.put("skuInfo", skuInfo);
                return skuInfo;
            }
        }, executor);

        CompletableFuture<Void> priceC = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                //  获取价格
                BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
                //  map 中 key 对应的谁? Thymeleaf 获取数据的时候 ${skuInfo.skuName}
                result.put("price", skuPrice);
            }
        }, executor);
        //  判断skuInfo 不为空

        CompletableFuture<Void> categoryViewC = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                //  获取分类数据
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                result.put("categoryView", categoryView);
            }
        }, executor);

        CompletableFuture<Void> spuSaleAttrListC = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                //  获取销售属性+销售属性值
                List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
                result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
            }
        }, executor);

        CompletableFuture<Void> valuesSkuJsonC = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                //  查询销售属性值Id 与skuId 组合的map
                Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
                //  将这个map 转换为页面需要的Json 对象
                String valueJson = JSON.toJSONString(skuValueIdsMap);
                result.put("valuesSkuJson", valueJson);
            }
        }, executor);

        CompletableFuture<Void> spuPosterListC = skuInfoCompletableFuture.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                //  返回map 集合 Thymeleaf 渲染：能用map 存储数据！
                //  spu海报数据
                List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
                result.put("spuPosterList", spuPosterList);
            }
        }, executor);

        CompletableFuture<Void> attrValueListCompletableFuture = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
                //  使用拉姆达表示
                List<Map<String, String>> skuAttrList = attrList.stream().map((baseAttrInfo) -> {
                    Map<String, String> attrMap = new HashMap<>();
                    attrMap.put("attrName", baseAttrInfo.getAttrName());
                    attrMap.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                    return attrMap;
                }).collect(Collectors.toList());
                result.put("skuAttrList", skuAttrList);
            }
        }, executor);

        CompletableFuture.allOf(skuInfoCompletableFuture, priceC, categoryViewC, spuSaleAttrListC, valuesSkuJsonC, spuPosterListC, attrValueListCompletableFuture)
                .join();


        return result;
    }
}