package cn.itcast.server.handler;

import cn.itcast.message.LoginRequestMessage;
import cn.itcast.message.LoginResponseMessage;
import cn.itcast.server.service.UserServiceFactory;
import cn.itcast.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class LoginRequestMessageHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        String username = msg.getUsername();
        String password = msg.getPassword();
        boolean logined = UserServiceFactory.getUserService().login(username, password);
        LoginResponseMessage loginResponseMessage;
        if (logined) {
            SessionFactory.getSession().bind(ctx.channel(), username);
            loginResponseMessage = new LoginResponseMessage(true, "登录成功");
        } else {
            loginResponseMessage = new LoginResponseMessage(false, "用户名或密码错误");
        }
        ctx.writeAndFlush(loginResponseMessage);
    }
}