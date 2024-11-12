package top.kircute.texas.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class RoomStateDTO {
    @NotNull private Boolean conflict;
    @NotNull private Boolean playing;
    @NotNull private Integer playerCount;
    @NotNull private String host;
}
