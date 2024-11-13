package top.kircute.texas.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.kircute.texas.service.RoomBO;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class GameHandler extends TextWebSocketHandler {
    @Resource
    private ConcurrentHashMap<String, RoomBO> rooms;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String player = (String) session.getAttributes().get("player");
        String room = (String) session.getAttributes().get("room");
        if (room == null || player == null) return;
        rooms.compute(room, (k, v) -> {
            if (v == null) {
                log.error("Connection established from {} in a non-existent room {}, automatically close.", player, k);
                try {
                    session.close();
                } catch (Exception e) {
                    log.error("Failed close connection from {} in {}: {}", player, k, e);
                }
                return null;
            }
            try {
                v.connect(player, session);
                log.info("Connection established from {} in {}", player, k);
            } catch (Exception e) {
                log.error("Connection failed from {} in {}: {}", player, k, e.getMessage());
                try {
                    session.close();
                } catch (Exception ex) {
                    log.error("Failed close connection from {} in {}: {}", player, k, ex);
                }
            }
            return v;
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String player = (String) session.getAttributes().get("player");
        String room = (String) session.getAttributes().get("room");
        String msgStr = new String(message.asBytes());
        JSONObject parsed;
        try {
            parsed = JSON.parseObject(msgStr);
            String cmd = parsed.getString("cmd");
            if ("heartbeat".equals(cmd)) return;
            rooms.compute(room, (k, v) -> {
                if (v == null) {
                    log.error("Received command from {} in a non-existent room {}, automatically close the connection.", player, k);
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.error("Failed close connection from {} in {}: {}", player, k, e);
                    }
                    return null;
                }
                try {
                    switch (cmd) {
                        case "fold": v.fold(player); break;
                        case "raise": v.raise(player, (Integer) parsed.get("bet")); break;
                        case "call": v.call(player); break;
                        case "ready": v.ready(player, (Boolean) parsed.get("ready")); break;
                        case "newGame": v.newGame(player); break;
                        case "chat": v.chat(player, (String) parsed.get("content")); break;
                        default:
                            log.error("Received request from {} in {} but provided command is not exist: {}", player, k, cmd);
                            break;
                    }
                    log.info("Deal command from {} in {} succeeded.", player, k);
                } catch (Exception e) {
                    log.error("Deal command failed from {} in {}: {}, the message is {}", player, k, e.getMessage(), msgStr);
                }
                return v;
            });
        } catch (Exception e) {
            log.error("Received request from {} in {} but parsing failed: {}, the message is {}", player, room, e.getMessage(), msgStr);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String player = (String) session.getAttributes().get("player");
        String room = (String) session.getAttributes().get("room");
        log.info("Connection closed from {} in {}: {}", player, room, status.getReason());
        if (room == null || player == null) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed close connection from {} in {}", player, room);
            }
        } else roomDisconnect(room, player, session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String player = (String) session.getAttributes().get("player");
        String room = (String) session.getAttributes().get("room");
        log.error("Transport error from {} in {}: {}", player, room, exception.getMessage());
        if (room == null || player == null) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed close connection from {} in {}", player, room);
            }
        } else roomDisconnect(room, player, session);
    }

    private void roomDisconnect(String room, String player, WebSocketSession session) {
        rooms.compute(room, (k, v) -> {
            if (v == null) {
                log.error("Received connection closure request from {} in a non-existent room {}, automatically close.", player, k);
                try {
                    session.close();
                } catch (Exception e) {
                    log.error("Failed close connection from {} in {}: {}", player, k, e);
                }
                return null;
            }
            try {
                boolean reserve = v.disconnect(player);
                log.info("Connection closed from {} in {}.", player, k);
                if (!reserve) {
                    log.info("Room {} is empty, automatically remove.", k);
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed quit the room from {} in {}: {}", player, k, e.getMessage());
            }
            return v;
        });
    }
}
