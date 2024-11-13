package top.kircute.texas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.kircute.texas.pojo.dto.ResultDTO;
import top.kircute.texas.pojo.dto.RoomStateDTO;
import top.kircute.texas.service.RoomBO;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping
public class RoomController {
    @Value("${api.websocket-endpoint}")
    private String wsEndpoint;

    @Resource
    private ConcurrentHashMap<String, RoomBO> rooms;

    @GetMapping("/api/room_state")
    public ResultDTO<RoomStateDTO> getRoomState(@RequestParam String room, @RequestParam String player) {
        RoomBO r = rooms.get(room);
        if (r == null) return new ResultDTO<>(404, null);
        return new ResultDTO<>(r.getState(player));
    }

    @GetMapping("/api/ws_endpoint")
    public String getWsEndpoint() {
        return wsEndpoint;
    }
}
