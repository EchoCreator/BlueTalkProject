package com.example.common.result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationResult<T> {
    private List<T> data;
    private Long minTime;
    private Integer offset;
}
