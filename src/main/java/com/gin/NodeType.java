package com.gin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NodeType {
    STATIC(0),
    ROOT(1),
    PARAM(2),
    CATCH_ALL(3)
    ;

    private final int nodeType;
}
