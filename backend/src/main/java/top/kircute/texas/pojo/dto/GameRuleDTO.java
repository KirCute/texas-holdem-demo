package top.kircute.texas.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class GameRuleDTO {
    @NotNull private Integer defaultInitialChip;
    @NotNull private Integer defaultSmallBlindBet;
}
