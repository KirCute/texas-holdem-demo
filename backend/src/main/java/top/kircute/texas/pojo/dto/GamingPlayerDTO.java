package top.kircute.texas.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class GamingPlayerDTO {
    public static final int GAMING_STATUS_NORMAL = 0;
    public static final int GAMING_STATUS_FOLD = 1;
    public static final int GAMING_STATUS_ALLIN = 2;

    @NotNull private PlayerDTO player;
    @NotNull private String lastOperation;
    @JsonIgnore private int status;
    @JsonIgnore private int card1;
    @JsonIgnore private int card2;
    @JsonIgnore private int bet;

    public GamingPlayerDTO(PlayerDTO player, int card1, int card2) {
        this.player = player;
        lastOperation = "";
        status = GAMING_STATUS_NORMAL;
        this.card1 = card1;
        this.card2 = card2;
        bet = 0;
    }
}
