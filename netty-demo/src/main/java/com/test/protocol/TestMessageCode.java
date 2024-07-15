package com.test.protocol;

import com.test.message.LoginRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;

public class TestMessageCode {
    public static void main(String[] args) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new LoggingHandler(), new MessageCodec());
        LoginRequestMessage zhangsan = new LoginRequestMessage("zhangsan", "123");
        // 测试出栈
        channel.writeOutbound(zhangsan);
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer();
        new MessageCodec().encode(null, zhangsan, byteBuf);
        // 测试入栈
        channel.writeInbound(byteBuf);
    }
}
