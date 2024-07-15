package com.test;

import com.test.protocol.MessageCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ChatServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);
        MessageCodec messageCodec = new MessageCodec();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(loggingHandler); // 打印日志
                    ch.pipeline().addLast(messageCodec);

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
