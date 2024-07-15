package com.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

public class HelloServer {
    public static void main(String[] args) {
        // 启动器，负责组装netty组件，启动服务器
        new ServerBootstrap()
                // 2.BossEventLoop(负责accept事件), WorkerEventLoop(selector 监听io事件如read，thread)
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                // 封装了原生的 NioServerSocketChannel
                .channel(NioServerSocketChannel.class)
                // child指的是worker，负责处理读写
                .childHandler(
                        // 和客户端进行数据读写的通道
                        new ChannelInitializer<NioSocketChannel>(){
                    // 连接建立后才会调用
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // 添加具体handler
                        // handler和handler之间的调用可以在同一个线程，也可以不在同一个线程
                        ch.pipeline().addLast(new StringDecoder()); // 将传输过来的ByteBuf转换为字符串
                        // InboundHandler是入栈处理器
                        // OutboundHandler只有向channel中写入数据才会触发
                        // 并且入栈是从头往后，出栈是从后往前
                        // head -> h1 -> h2 -> ... -> hn -> tail
                        // 调用writeAndFlash方法会去找出栈处理器(outBound) 其中ctx对象调用从当前节点找，ch对象调用从tail节点往前找
                        // ctx.fireChannelRead(msg)会去找下一个入栈处理器
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() { // 自定义handler
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                System.out.println(msg);

                            }
                        });
                    }
                })
                .bind(8080);

        ByteBufAllocator.DEFAULT.directBuffer();
    }
}
