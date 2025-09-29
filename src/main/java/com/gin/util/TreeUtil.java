package com.gin.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

public class TreeUtil {
    private TreeUtil() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindWildcard {
        private String wildcard;
        private int i;
        private boolean valid;
    }

    public static int longestCommonPrefix(String a, String b) {
        int i = 0;
        int max = Math.min(a.length(), b.length());

        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }

        return i;
    }

    /**
     * Search for a wildcard segment and check the name for invalid characters.
     * 找到每个url片段中可能是路径参数的部分
     *
     * @param path 默认认为传入的uri都是符合openapi 3.0规范的；且由于一定原因，uri一定以 / 结尾
     * @return 例如/api/{id}/test/，返回的就是 {id}/,5,true
     */
    @SneakyThrows
    public static FindWildcard findWildcard(String path) {
        // Find start
        for (int start = 0; start < path.length(); start++) {
            char c = path.charAt(start);

            // A wildcard starts with '{' (param)
            if (c == '{') {
                if (path.substring(start).startsWith("{id}/")) {
                    return new FindWildcard("{id}/", start, true);
                }
            }
        }

        return new FindWildcard("", -1, false);
    }
}
