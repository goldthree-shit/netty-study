package com.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LoggingHandler;

public class TestHttp {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler()); // 打印日志
                    ch.pipeline().addLast(new HttpServerCodec()); // 点进去查看，发现已经实现了编码器与解码器. 即使入栈处理器，又是出栈处理器
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                        @Override
                         public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof HttpRequest) { // 请求头，请求行
                                HttpRequest msg1 = (HttpRequest) msg;
                                System.out.println(msg1.uri());

                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(msg1.protocolVersion(), HttpResponseStatus.OK);
                                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, "<a>hello<a>".getBytes().length);
                                // 设置长度否则浏览器会不停地转圈
                                response.content().writeBytes("<a>hello<a>".getBytes());

                                ctx.writeAndFlush(response); // 会经过出栈处理器编码之后返回

                            } else if (msg instanceof HttpContent) { // 请求体
                            }
                        }
                    });
                }
            });
            ChannelFuture channelFuture = serverBootstrap.bind(8080).sync();// 绑定服务器并阻塞直到绑定完成
            // 返回的 ChannelFuture 对象可以用来添加监听器（addListener()），以便在绑定操作完成后执行一些逻辑。
            channelFuture.channel().closeFuture().sync(); // 等待服务器关闭并阻塞直到关闭完成
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
