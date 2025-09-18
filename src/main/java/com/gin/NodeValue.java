package com.gin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeValue {
    private HandlersChain handlers;
    private Params params;
    private boolean tsr;
    private String fullPath;
}
