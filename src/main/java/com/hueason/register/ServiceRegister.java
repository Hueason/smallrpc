package com.hueason.register;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2017/5/5.
 */
public class ServiceRegister {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegister.class);

    private final ZkClient zkClient;

    public ServiceRegister(String zkAddress) {
        //创建 zk客户端
        zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
        logger.debug("connect zookeeper");
    }

    public void register(String serviceName, String serviceAddress) {
        //创建 registry 节点(持久)
        String registryPath = Constant.ZK_REGISTRY_PATH;
        if (!zkClient.exists(registryPath)) {
            zkClient.createPersistent(registryPath);
            logger.debug("create registry node : {}", registryPath);
        }
        //创建 service节点(持久)
        String servicePath = registryPath + "/" + serviceName;
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
            logger.debug("create service node : {}", servicePath);
        }
        //创建 address节点(临时)
        String addressPath = servicePath + "/address-";
        String addressNode = zkClient.createEphemeralSequential(addressPath, serviceAddress);
        logger.debug("create address node : {}", addressNode);
    }


    public void register(String serverAddress) {
    }
}
