package top.kircute.texas.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import top.kircute.texas.service.AsyncExecutor;
import top.kircute.texas.service.RoomBO;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketAuthenticator extends HttpSessionHandshakeInterceptor {
    @Resource
    private ConcurrentHashMap<String, RoomBO> rooms;

    @Value("${game.initial-chip}")
    private Integer initialChip;

    @Value("${game.small-blind-bet}")
    private Integer smallBlindBet;

    @Resource
    private AsyncExecutor asyncExecutor;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            ServletServerHttpRequest req = (ServletServerHttpRequest) request;
            String roomName = null;
            String playerName = null;
            if (req.getURI().getQuery() != null) {
                String[] kvs = req.getURI().getQuery().split("&");
                for (String kv : kvs) {
                    String[] s = kv.split("=");
                    if (s.length != 2) continue;
                    if (s[0].equals("room")) roomName = s[1];
                    else if (s[0].equals("player")) playerName = s[1];
                }
            }
            if (roomName == null || playerName == null) {
                log.error("WebSocket handshake failed for not providing room name or player name.");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            RoomBO room = rooms.computeIfAbsent(roomName, r -> {
                log.info("Created new room: {}", r);
                return new RoomBO(asyncExecutor, initialChip, smallBlindBet);
            });
            if (!room.join(playerName)) {
                log.error("WebSocket handshake from another {}({}) in {} failed: player name conflict.", playerName, req.getRemoteAddress(), roomName);
                response.setStatusCode(HttpStatus.CONFLICT);
                return false;
            }

            attributes.put("room", roomName);
            attributes.put("player", playerName);
            super.setCreateSession(true);
            return super.beforeHandshake(req, response, wsHandler, attributes);
        } catch (Exception e) {
            log.error("WebSocket handshake from {} failed: {}", request.getRemoteAddress(), e.getMessage());
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.info("WebSocket handshake from {} succeed.", request.getRemoteAddress());
        super.afterHandshake(request, response, wsHandler, exception);
    }
}
