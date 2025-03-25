package top.kircute.texas.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @NotNull @JsonIgnore private Boolean showCards;
    @NotNull @JsonIgnore private Boolean isHost;

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
