package com.gin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Params {
    private List<Param> params;

    /**
     * Get returns the value of the first Param which key matches the given name and a boolean true.
     * If no matching Param is found, an empty string is returned and a boolean false .
     *
     * <pre>{@code
     * func (ps Params) Get(name string) (string, bool) {
     * 	for _, entry := range ps {
     * 		if entry.Key == name {
     * 			return entry.Value, true
     * 		}
     * 	}
     * 	return "", false
     * }
     * }</pre>
     *
     * @param name
     * @return
     */
    public String get(String name) {
        return params.stream().filter(param -> param.getKey().equals(name)).findFirst().map(Param::getValue).orElse("");
    }

    /**
     * ByName returns the value of the first Param which key matches the given name.
     * If no matching Param is found, an empty string is returned.
     *
     * <pre>{@code
     * func (ps Params) ByName(name string) (va string) {
     * 	va, _ = ps.Get(name)
     * 	return
     * }
     * }</pre>
     *
     * @param name
     * @return
     */
    public String byName(String name) {
        return get(name);
    }
}
