package com.hueason.register;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Administrator on 2017/5/15.
 */
public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private String zkAddress;

    public ServiceDiscovery(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public String discover(String name) {
        //创建Zookeeper客户端

        ZkClient zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
        logger.debug("connect zookeeper");
        try {
            //获取 service 节点
            String servicePath = Constant.ZK_REGISTRY_PATH + "/" + name;
            if (!zkClient.exists(servicePath)) {
                throw new RuntimeException(String.format("can not find any service node on path : %s", servicePath));
            }
            List<String> addressList = zkClient.getChildren(servicePath);
            if (CollectionUtils.isEmpty(addressList)) {
                throw new RuntimeException(String.format("can not find any service node on path : %s", servicePath));
            }
            //获取 address节点
            String address;
            int size = addressList.size();
            if (size == 1) {
                //若只有一个地址，直接获取
                address = addressList.get(0);
                logger.debug("get only address node: {}", address);
            } else {
                //如存在多个地址，则随机获取
                address = addressList.get(ThreadLocalRandom.current().nextInt(size));
                logger.debug("get random address node: {}", address);
            }
            //获取 address 节点的值
            String addressPath = servicePath + "/" + address;
            return zkClient.readData(addressPath);
        } finally {
            zkClient.close();
        }
    }
}
