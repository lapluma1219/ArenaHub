package com.arenahub.websocket;

import com.arenahub.security.CurrentUser;
import com.arenahub.security.JwtService;
import com.arenahub.service.RoomService;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final JwtService jwtService;
    private final RoomService roomService;

    public RoomWebSocketHandler(JwtService jwtService, RoomService roomService) {
        this.jwtService = jwtService;
        this.roomService = roomService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = parseRoomId(session.getUri());
        String token = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("token");
        if (token == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing token"));
            return;
        }
        CurrentUser user = jwtService.parse(token);
        if (!roomService.canAccessRoom(roomId, user.playerId())) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("forbidden"));
            return;
        }
        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("playerId", user.playerId());
        roomSessions.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(session);
        broadcast(roomId, systemMessage("JOIN", user.playerId()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long playerId = (Long) session.getAttributes().get("playerId");
        String payload = "{\"type\":\"ACTION\",\"playerId\":" + playerId + ",\"payload\":" + quote(message.getPayload()) + "}";
        broadcast(roomId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long playerId = (Long) session.getAttributes().get("playerId");
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
            }
            if (playerId != null) {
                broadcast(roomId, systemMessage("LEAVE", playerId));
            }
        }
    }

    private void broadcast(Long roomId, String payload) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.getOrDefault(roomId, Set.of());
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        }
    }

    private Long parseRoomId(URI uri) {
        String[] parts = uri.getPath().split("/");
        return Long.valueOf(parts[parts.length - 1]);
    }

    private String systemMessage(String type, Long playerId) {
        return "{\"type\":\"" + type + "\",\"playerId\":" + playerId + "}";
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
