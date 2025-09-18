package com.gin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkippedNode {
    private String path;
    private Node node;
    private int paramsCount;
}
