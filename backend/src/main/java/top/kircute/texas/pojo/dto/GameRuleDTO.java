package top.kircute.texas.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class GameRuleDTO {
    @NotNull private Integer initialChip;
    @NotNull private Integer smallBlindBet;
    @NotNull private Long reflectionTime;
    @NotNull private Integer suitRange;
    @NotNull private Integer rankRange;
}
