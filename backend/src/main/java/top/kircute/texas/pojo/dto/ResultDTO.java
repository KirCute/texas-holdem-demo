package top.kircute.texas.pojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class ResultDTO<T> {
    @NotNull private Integer errno;
    @JsonInclude(JsonInclude.Include.NON_NULL) private T data;

    public ResultDTO(T data) {
        this.errno = 200;
        this.data = data;
    }
}
