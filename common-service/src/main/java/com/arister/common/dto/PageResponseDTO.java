package com.arister.common.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<E> {
    private List<E> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private Boolean first;
    private Boolean last;
    private PageableDTO pageable;
}
