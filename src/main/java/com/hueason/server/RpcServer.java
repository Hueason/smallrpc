package com.hueason.server;

import com.hueason.common.RpcDecoder;
import com.hueason.common.RpcEncoder;
import com.hueason.common.RpcRequest;
import com.hueason.common.RpcResponse;
import com.hueason.register.ServiceRegister;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/5/5.
 */
public class RpcServer implements ApplicationContextAware,InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private String serverAddress;
    private ServiceRegister serviceRegister;

    private Map<String,Object> handlerMap = new ConcurrentHashMap<String, Object>();//存放接口和服务对象之间的映射关系

    public RpcServer(String serverAddress){
        this.serverAddress = serverAddress;
    }

    public RpcServer(String serverAddress,ServiceRegister serviceRegister){
        this.serverAddress = serverAddress;
        this.serviceRegister = serviceRegister;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //扫描所有带有RpcSrvice注解的类 并初始化
        Map<String,Object> serviceBeanmap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if(MapUtils.isNotEmpty(serviceBeanmap)){
            for (Object serviceBean:serviceBeanmap.values() ) {
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName,serviceBean);
            }
        }
    }

    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup wokerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,wokerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcDecoder(RpcRequest.class))//将请求进行解码
                                    .addLast(new RpcEncoder(RpcResponse.class))//将响应进行编码
                                    .addLast(new RpcHandler(handlerMap));//处理请求
                        }
                    });

            //获取Rpc服务器的Ip地址和端口号
            String[] strings = StringUtils.split(serverAddress,":");
            String host = strings[0];
            int port = Integer.valueOf(strings[1]);


            ChannelFuture f = b.bind(host,port).sync();
            logger.debug("Server started on port {}",port);

            if(serviceRegister != null){
                for (String interfaceName:handlerMap.keySet()) {
                    serviceRegister.register(interfaceName,serverAddress);//注册服务地址
                }
            }

            f.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            wokerGroup.shutdownGracefully();
        }
    }

}
