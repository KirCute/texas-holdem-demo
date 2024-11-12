package top.kircute.texas.pojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryDTO {
    @Data
    @AllArgsConstructor
    public static class Card {
        @NotNull Integer card;
        @NotNull Boolean hole;
    }

    @NotNull String playerName;
    @Nullable List<Integer> holeCards;
    @Nullable List<Card> bestHand;
    @Nullable String handType;
    @NotNull Integer award;
}
