package com.gin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandlersChain {
    private List<HandlersFunc> handlersFuncs = new ArrayList<>();
}
