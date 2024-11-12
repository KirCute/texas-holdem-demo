package top.kircute.texas.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class PlayerDTO {
    @NotNull private String name;
    @NotNull private Integer chips;
    @NotNull private Integer bankruptcy;
    @NotNull private Boolean isButton;

    public void reduceChips(int bet) {
        chips -= bet;
    }

    public void increaseChips(int award) {
        chips += award;
    }

    public void increaseBankruptcy(int initialChip) {
        bankruptcy++;
        chips = initialChip;
    }
}
