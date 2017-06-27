package com.hueason.client;

import com.hueason.common.RpcDecoder;
import com.hueason.common.RpcEncoder;
import com.hueason.common.RpcRequest;
import com.hueason.common.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2017/6/27.
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private final String host;
    private final int port;

    private RpcResponse response;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        this.response = response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("api caught exception", cause);
        ctx.close();
    }

    public RpcResponse send(RpcRequest request) throws InterruptedException {
//配置客户端NIO线程组
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建 netty客户端
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcEncoder(RpcRequest.class))//编码
                                    .addLast(new RpcDecoder(RpcResponse.class))//解码
                                    .addLast(RpcClient.this);//处理
                        }
                    });

            //发起异步连接操作
            ChannelFuture f = b.connect(host, port).sync();

            Channel channel = f.channel();
            //写入请求数据
            channel.writeAndFlush(request).sync();

            //等待客户端链路关闭
            channel.closeFuture().sync();

            return response;
        } finally {
            //优雅退出，释放NIO线程组
            group.shutdownGracefully();
        }
    }
}
