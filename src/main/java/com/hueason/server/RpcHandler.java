package com.hueason.server;

import com.hueason.common.RpcRequest;
import com.hueason.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Administrator on 2017/5/5.
 */
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static final Logger logger = LoggerFactory.getLogger(RpcHandler.class);

    private final Map<String, Object> handlerMap;


    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) throws Exception {
        logger.debug("Receive request " + request.getRequestId());
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            Object result = handler(request);
            response.setResult(result);
        } catch (Exception e) {
            response.setException(e);
            logger.error("RPC Server handler request error {}", e);
        }
        //响应 并接受回执
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private Object handler(RpcRequest request) throws Exception {
        String serviceName = request.getInterfaceName();
        Object serviceBean = handlerMap.get(serviceName);

        if (serviceBean == null) {
            throw new RuntimeException(String.format("can not find service bean by key: %s", serviceName));
        }

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] paramterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        logger.debug(serviceClass.getName());
        logger.debug(methodName);
        for (int i = 0; i < paramterTypes.length; i++) {
            logger.debug(paramterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; i++) {
            logger.debug(parameters[i].toString());
        }

        //JDK reflect
//        Method method = serviceClass.getMethod(methodName,paramterType);
//        method.setAccessible(true);
//        return method.invoke(serviceBean,parameters);

        //Cglib reflect
        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, paramterTypes);
        return serviceFastMethod.invoke(serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.debug("serivce caught exception", cause);
        ctx.close();
    }
}
