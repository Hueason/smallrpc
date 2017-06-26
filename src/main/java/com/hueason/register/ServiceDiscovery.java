package com.hueason.register;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2017/5/15.
 */
public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private String zkAddress;

    public ServiceDiscovery(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public String discover(String name){
        //创建Zookeeper客户端
        return null;
    }
}
