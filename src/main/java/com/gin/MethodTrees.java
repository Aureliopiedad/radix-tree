package com.gin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodTrees {
    private List<MethodTree> methodTrees;

    /**
     * <pre>{@code
     * func (trees methodTrees) get(method string) *node {
     * 	for _, tree := range trees {
     * 		if tree.method == method {
     * 			return tree.root
     * 		        }    * 	}
     * 	return nil
     * }
     * }</pre>
     *
     * @param method
     * @return
     */
    public Node get(String method) {
        return methodTrees.stream().filter(methodTree -> methodTree.getMethod().equals(method)).findFirst().map(MethodTree::getRoot).orElse(null);
    }
}
