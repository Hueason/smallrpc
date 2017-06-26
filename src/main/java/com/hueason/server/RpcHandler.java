package com.hueason.server;

import com.hueason.common.RpcRequest;
import com.hueason.common.RpcResponse;
import io.netty.channel.ChannelFuture;
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

    private final Map<String,Object> handlerMap;


    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) throws Exception {
        RpcServer.submit(new Runnable() { //这边是使用了一个异步线程来处理rpc请求
            public void run() {
                logger.info("Receive request "+ request.getRequestId());
                RpcResponse response = new RpcResponse();
                response.setRequestId(request.getRequestId());
                try {
                    Object result = handler(request);
                    response.setResult(result);
                }catch (Throwable t){
                    response.setError(t.toString());
                    logger.error("RPC Server handler request error {}",t);
                }
                //响应 并接受回执
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        logger.info("Send response for request "+request.getRequestId());
                    }
                });
            }
        });
    }

    private Object handler(RpcRequest request) throws Exception {
        String className = request.getClassName();
        Object serviceBean = handlerMap.get(className);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] paramterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        
        logger.info(serviceClass.getName());
        logger.info(methodName);
        for (int i = 0; i < paramterTypes.length; i++) {
            logger.info(paramterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; i++) {
            logger.info(parameters[i].toString());
        }

        //JDK reflect
//        Method method = serviceClass.getMethod(methodName,paramterType);
//        method.setAccessible(true);
//        return method.invoke(serviceBean,parameters);

        //Cglib reflect
        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName,paramterTypes);
        return serviceFastMethod.invoke(serviceBean,parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("serivce caught exception",cause);
        ctx.close();
    }
}
