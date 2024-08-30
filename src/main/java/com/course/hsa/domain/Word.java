package com.course.hsa.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Word {

    private String content;
    private Integer size;
}
