package top.kircute.texas.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.kircute.texas.pojo.dto.SummaryDTO;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
public class HandTypeVO implements Comparable<HandTypeVO> {
    @NotNull private List<SummaryDTO.Card> cards;
    private int size;

    @Override
    public int compareTo(HandTypeVO o) {
        return Integer.compare(size, o.getSize());
    }
}
