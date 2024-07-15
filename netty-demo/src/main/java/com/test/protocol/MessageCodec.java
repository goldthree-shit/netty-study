package com.test.protocol;

import com.test.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

@Slf4j
public class MessageCodec extends ByteToMessageCodec<Message> {
    // 出栈的时候编码, 会把数据写入到out
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        // 1. 4字节魔数，用于判定是否是无效数据包
        out.writeBytes(new byte[]{1, 2, 3, 4});
        // 2. 1字节版本
        out.writeByte(1);
        // 3. 1字节的序列化方式 jdk 0, json 1
        out.writeByte(0);
        // 4. 1字节的指令类型
        out.writeByte(msg.getMessageType());
        // 5. 1字节，使头部对齐
        out.writeByte(0);
        // 6. 4字节的请求序号(目前用不上)
        out.writeInt(msg.getSequenceId());

        // 7. java对象变成字节数组
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(msg);
            bytes = bos.toByteArray();
        }

        // 8. 4个字节 长度
        out.writeInt(bytes.length);

        // 9. 写入内容
        out.writeBytes(bytes);
    }

    // 入栈的时候解码
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // read会让读指针往后走，get获取索引，读指针不会变
        int magicNum = in.readInt();
        byte version = in.readByte();
        byte serializerType = in.readByte();
        byte messageType = in.readByte();
        in.readByte();
        int sequenceId = in.readInt();


        int length = in.readInt();
        log.info("MagicNum: {}, Version: {}, SerializerType: {}, MessageType: {}, SequenceId: {}",
                magicNum, version, serializerType, messageType, sequenceId);
        byte[] bytes = new byte[length];

        in.readBytes(bytes, 0, length);
        if (serializerType == 0) { // 反序列化
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                Message message = (Message) ois.readObject();
                log.info("Decoded message: {}", message);
                out.add(message); // 将解码后的对象添加到out列表中
            }
        }
    }
}
