package top.kircute.texas.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class WaitingPlayerDTO {
    @NotNull private PlayerDTO player;
    @NotNull private Boolean ready;
}
