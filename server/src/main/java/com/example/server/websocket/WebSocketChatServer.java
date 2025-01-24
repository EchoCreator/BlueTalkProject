package com.example.server.websocket;

import com.example.common.constant.JwtClaimsConstant;
import com.example.common.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/chat/{sid}")
public class WebSocketChatServer {

    // 存放会话对象，记录当前在线连接数
    private static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
        System.out.println("进入用户/群聊：" + sid + "的聊天频道");
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到来自用户/群聊：" + sid + "的信息:" + message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("断开同用户/群聊:" + sid + "的聊天频道");
        sessionMap.remove(sid);
    }

    /*
     *  发送消息给客户端
     * */
    public void sentMessage(String message, String key) {
        Session toSession = sessionMap.get(key);
        if (toSession != null) {
            System.out.println("给用户/群聊：" + toSession.getId() + "发送消息：" + message);
            try {
                toSession.getBasicRemote().sendText(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
