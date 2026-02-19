package com.arister.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageableDTO {
    private int pageNumber;
    private int pageSize;
    private boolean paged;
    private boolean unpaged;
    private long offset;
    private Sort sort;
}
