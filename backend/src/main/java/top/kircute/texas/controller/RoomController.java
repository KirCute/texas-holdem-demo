package top.kircute.texas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.kircute.texas.pojo.dto.GameRuleDTO;
import top.kircute.texas.pojo.dto.ResultDTO;
import top.kircute.texas.pojo.dto.RoomStateDTO;
import top.kircute.texas.service.RoomBO;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class RoomController {
    @Value("${api.websocket-endpoint}")
    private String wsEndpoint;

    @Value("${gamerule.default-initial-chip}")
    private Integer defaultInitialChip;

    @Value("${gamerule.default-small-blind-bet}")
    private Integer defaultSmallBlindBet;

    @Value("${gamerule.default-reflection-time}")
    private Long defaultReflectionTime;

    @Value("${gamerule.default-suit-range}")
    private Integer defaultSuitRange;

    @Value("${gamerule.default-rank-range}")
    private Integer defaultRankRange;

    @Resource
    private ConcurrentHashMap<String, RoomBO> rooms;

    @GetMapping("/room_state")
    public ResultDTO<RoomStateDTO> getRoomState(@RequestParam String room, @RequestParam String player) {
        RoomBO r = rooms.get(room);
        if (r == null) return new ResultDTO<>(404, null);
        return new ResultDTO<>(r.getState(player));
    }

    @GetMapping("/default_rule")
    public GameRuleDTO getDefaultGameRule() {
        return new GameRuleDTO(defaultInitialChip, defaultSmallBlindBet, defaultReflectionTime, defaultSuitRange,
                defaultRankRange);
    }

    @GetMapping("/ws_endpoint")
    public String getWsEndpoint() {
        return wsEndpoint;
    }
}
