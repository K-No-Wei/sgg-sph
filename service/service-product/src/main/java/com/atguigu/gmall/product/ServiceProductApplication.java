package com.atguigu.gmall.product;

import com.atguigu.gmall.common.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceProductApplication implements CommandLineRunner {

   @Autowired
   private RedissonClient redissonClient;


   public static void main(String[] args) {
      SpringApplication.run(ServiceProductApplication.class, args);
   }

   /**
    * 初始化布隆过滤器
    * @param args
    * @throws Exception
    */
   @Override
   public void run(String... args) throws Exception {
      RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);

      //初始化布隆过滤器
      bloomFilter.tryInit(10001, 0.001);
      bloomFilter.add(24);
      bloomFilter.add(25);
      bloomFilter.add(23);
   }
}