/*****************************************************************
 * Copyright (c) 2017 www.noryar.com Inc. All rights reserved.
 *****************************************************************/
package com.noryar.nrpc.client;

import com.noryar.rpc.common.RpcDecoder;
import com.noryar.rpc.common.RpcEncoder;
import com.noryar.rpc.common.RpcRequest;
import com.noryar.rpc.common.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类描述:RPC客户端（用于发送RPC请求）.
 *
 * @author leon.
 */
public class NrpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NrpcClient.class);
    private String host;
    private int port;
    private RpcResponse response;
    private final Object obj = new Object();

    public NrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 功能描述:链接服务端，发送消息.
     *
     * @param request request.
     * @return response.
     * @throws Exception e.
     */
    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel)
                                throws Exception {
                            // 向pipeline中添加编码、解码、业务处理的handler
                            channel.pipeline()
                                    .addLast(new RpcEncoder(RpcRequest.class))  //OUT - 1
                                    .addLast(new RpcDecoder(RpcResponse.class)) //IN - 1
                                    .addLast(NrpcClient.this);                   //IN - 2
                        }
                    }).option(ChannelOption.SO_KEEPALIVE, true);
            // 链接服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            //将request对象写入outbundle处理后发出（即RpcEncoder编码器）
            future.channel().writeAndFlush(request).sync();

            // 用线程等待的方式决定是否关闭连接
            // 其意义是：先在此阻塞，等待获取到服务端的返回后，被唤醒，从而关闭网络连接
            synchronized (obj) {
                obj.wait();
            }
            if (response != null) {
                future.channel().closeFuture().sync();
            }
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 功能描述:读取服务端的返回结果.
     *
     * @param ctx      context.
     * @param response response.
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, RpcResponse response)
            throws Exception {
        this.response = response;

        synchronized (obj) {
            obj.notifyAll();
        }
    }

    /**
     * 功能描述:异常处理.
     *
     * @param ctx   context.
     * @param cause e.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }
}
