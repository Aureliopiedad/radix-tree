package com.gin;

import com.gin.util.TreeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <a href="https://github.com/gin-gonic/gin/blob/master/tree.go#L417">参考文件</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private String path = "";
    private String indices = "";
    private boolean wildChild = false;
    private NodeType nType = NodeType.STATIC;
    private List<Node> children = new ArrayList<>();
    private HandlersChain handlers;
    private String fullPath = "";
    private String assetId;

    /**
     * addChild will add a child node, keeping wildcardChild at the end
     *
     * @param child
     */
    public void addChild(Node child) {
        if (this.isWildChild() && !this.getChildren().isEmpty()) {
            this.getChildren().add(this.getChildren().size() - 1, child);
            return;
        }

        this.getChildren().add(child);
    }

    @SneakyThrows
    public void insertChild(String path, String fullPath, String assetId) {
        Node n = this;
        while (true) {
            // Find prefix until first wildcard
            TreeUtil.FindWildcard wildcard = TreeUtil.findWildcard(path);
            if (wildcard.getI() < 0) {
                break;
            }

            if (!wildcard.isValid()) {
                throw new Exception(String.format("only wildcard path ends with '}/' are allowed, has: '%s' in path '%s'", wildcard.getWildcard(), fullPath));
            }

            if (wildcard.getWildcard().length() < 3) {
                throw new Exception(String.format("wildcards must be named with a non-empty name in path '%s'", fullPath));
            }

            // 根节点不能是路径参数匹配，所以只有i > 0 时，才会填充path
            if (wildcard.getI() > 0) {
                n.setPath(path.substring(0, wildcard.getI()));
                path = path.substring(wildcard.getI());
            }

            Node child = new Node();
            child.setNType(NodeType.PARAM);
            child.setPath(wildcard.getWildcard());
            child.setFullPath(fullPath);

            n.addChild(child);
            n.setWildChild(true);
            n = child;

            // if the path doesn't end with the wildcard, then there will be another subpath starting with '/'
            // wildcard.getWildcard() 指的是url中第一个匹配到的路径参数，path现在是该路径参数到url末尾的剩余字符串，例如{id}/ 和 {id}/test，且路径参数后第一个字符一定是/
            // 如果遇到这种情况，那么意味着需要再走一次循环，将下一个subPath添加到children
            if (wildcard.getWildcard().length() < path.length()) {
                path = path.substring(wildcard.getWildcard().length());
                Node child1 = new Node();
                child1.setFullPath(fullPath);

                if (!path.startsWith("{id}/")) {
                    n.setIndices(n.getIndices() + path.charAt(0));
                }
                n.addChild(child1);
                n = child1;
                continue;
            }

            n.setAssetId(assetId);
            // catchAll 在本项目中被排除
            return;
        }

        n.setPath(path);
        n.setAssetId(assetId);
        n.setFullPath(fullPath);
    }

    @SneakyThrows
    public void addRoute(String path) {
        addRoute(path, "api." + path);
    }

    /**
     * addRoute adds a node with the given handle to the path.
     * Not concurrency-safe!
     *
     * @param path
     */
    @SneakyThrows
    public void addRoute(String path, String assetId) {
        Node n = this;

        String fullPath = path;

        // 当前树并没有任何节点
        if (n.getPath().isEmpty() && n.getChildren().isEmpty()) {
            n.insertChild(path, fullPath, assetId);
            n.setNType(NodeType.ROOT);
            return;
        }

        int parentFullPathIndex = 0;

        walk:
        while (true) {
            // Find the longest common prefix.
            // This also implies that the common prefix contains no ':' or '*' since the existing key can't contain those chars.
            // 判断新加入的url和当前节点的path有多长的公共前缀
            // 由于一些原因，如果该节点是param节点的话，path和n.getPath()一定是前缀相同的
            int i = TreeUtil.longestCommonPrefix(path, n.getPath());

            if (i < n.getPath().length()) {
                // 如果当前节点前缀只有一部分匹配，需要使用当前节点所有属性生成子节点
                // 当前节点的path只取公共部分
                Node child = new Node(
                        n.getPath().substring(i),
                        n.getIndices(),
                        n.isWildChild(),
                        NodeType.STATIC,
                        n.getChildren(),
                        n.getHandlers(),
                        n.getFullPath(),
                        n.getAssetId()
                );

                // 新的节点直接变成当前节点的子节点
                n.setChildren(new ArrayList<>());
                n.getChildren().add(child);

                // 重置当前节点属性
                n.setIndices(String.valueOf(n.getPath().charAt(i)));
                n.setPath(path.substring(0, i));
                n.setHandlers(null);
                n.setWildChild(false);
                n.setFullPath(fullPath.substring(0, parentFullPathIndex + i));
                n.setAssetId(null);
            }

            // Make new node a child of this node
            if (i < path.length()) {
                // 公共部分之外的部分，继续匹配当前节点的子节点

                // 截取公共之外的部分
                path = path.substring(i);
                char c = path.charAt(0);

                // '/' after param
//                if (n.getNType() == NodeType.PARAM && n.getChildren().size() == 1) {
//                    parentFullPathIndex += n.getPath().length();
//                    n = n.getChildren().get(0);
//                    continue walk;
//                }

                if (!path.startsWith("{id}/")) {
                    // 证明下一个节点不是路径参数，但是节点的节点有可能是
                    // Check if a child with the next path byte exists
                    // 如果当前节点还有STATIC类型的子节点，就依次匹配
                    for (int j = 0; j < n.getIndices().length(); j++) {
                        if (c == n.getIndices().charAt(j)) {
                            parentFullPathIndex += n.getPath().length();
                            n = n.getChildren().get(j);
                            continue walk;
                        }
                    }

                    n.setIndices(n.getIndices() + c);
                    Node child = new Node();
                    child.setFullPath(fullPath);
                    n.addChild(child);
                    n = child;
                } else if (n.isWildChild() && path.startsWith("{id}/")) {
                    // 这里的if需要新增一些条件，不是所有{开头的字符串都是路径参数，万一没有}呢
                    // 由于一些原因，把所有的路径参数都认为是{id}
                    n = n.getChildren().get(n.children.size() - 1);

                    // 这里gin要求传入的路径参数必须字符串一致，我看不明白但是我大受震撼
                    if (path.length() >= n.getPath().length()
                            && Objects.equals(n.getPath(), path.substring(0, n.getPath().length()))) {
                        continue walk;
                    }

                    String pathSeg = pathSeg = path.split("/", 2)[0];
                    String prefix = fullPath.substring(0, fullPath.indexOf(pathSeg)) + n.path;
                    throw new Exception(String.format("'%s' in new path '%s' conflicts with existing wildcard '%s' in existing prefix '%s'", pathSeg, fullPath, n.path, prefix));
                }

                n.insertChild(path, fullPath, assetId);
                return;
            }

            n.setFullPath(fullPath);
            return;
        }
    }

    @SneakyThrows
    public NodeValue getValue(String path, List<SkippedNode> skippedNodes) {
        NodeValue value = new NodeValue();

        Node n = this;

        walk:
        while (true) {
            String prefix = n.getPath();

            // 一个url和某个节点的匹配，只能有三种结果
            // 1. url能匹配到该节点，且url还需要继续向下匹配(url长度超过当前节点前缀)
            // 2. url能匹配到该节点，且url无需继续向下匹配(url长度等同当前节点前缀)
            // 3. url不能匹配到该节点

            if (path.length() > prefix.length() && path.startsWith(prefix)) {
                // url能匹配到该节点，且url还需要继续向下匹配(url长度超过当前节点前缀)
                path = path.substring(prefix.length());

                // 先检查一下子节点有没有静态url能匹配的
                char idxc = path.charAt(0);
                for (int i = 0; i < n.getIndices().length(); i++) {
                    char c = n.getIndices().charAt(i);
                    if (c == idxc) {
                        // 以防万一 需要把路径参数的子节点也带上，方便之后回溯
                        if (n.isWildChild()) {
                            skippedNodes.add(new SkippedNode(
                                    prefix + path,
                                    new Node(n.getPath(), "", n.isWildChild(), n.getNType(), n.getChildren(), n.getHandlers(), n.getFullPath(), n.getAssetId()),
                                    0)
                            );
                        }

                        n = n.getChildren().get(i);
                        continue walk;
                    }
                }

                // 发现该节点下没有静态节点能继续匹配了，现在有两种选择
                // 1. 要么当前节点的子节点有路径参数节点，可以继续匹配路径参数
                // 2. 如果当前节点的子节点没有路径参数节点了，就直接回溯到上一个路径参数节点

                if (!n.isWildChild()) {
                    // 如果当前节点的子节点没有路径参数节点了，就直接回溯到上一个路径参数节点
                    for (int length = skippedNodes.size(); length > 0; length--) {
                        SkippedNode skippedNode = skippedNodes.get(length - 1);
                        skippedNodes.remove(length - 1);

                        if (skippedNode.getPath().endsWith(path)) {
                            path = skippedNode.getPath();
                            n = skippedNode.getNode();
                            continue walk;
                        }
                    }

                    // 如果没有上一个节点可供回溯了，那就证明这个url没有匹配到任何东西
                    return value;
                }

                // 要么当前节点的子节点有路径参数节点，可以继续匹配路径参数
                n = n.getChildren().get(n.children.size() - 1);
                // 不考虑不存在/的情况
                int end = path.indexOf("/");
                if (end + 1 < path.length()) {
                    // 在路径参数之后还有path需要匹配
                    String prefix2 = path.substring(0, end + 1);
                    path = path.substring(end + 1);
                    idxc = path.charAt(0);
                    for (int i = 0; i < n.getIndices().length(); i++) {
                        char c = n.getIndices().charAt(i);
                        if (c == idxc) {
                            // 以防万一 需要把路径参数的子节点也带上，方便之后回溯
                            if (n.isWildChild()) {
                                skippedNodes.add(new SkippedNode(
                                        prefix2 + path,
                                        new Node(n.getPath(), "", n.isWildChild(), n.getNType(), n.getChildren(), n.getHandlers(), n.getFullPath(), n.getAssetId()),
                                        0)
                                );
                            }

                            n = n.getChildren().get(i);
                            continue walk;
                        }
                    }

                    return value;
                }

                value.setFullPath(n.getFullPath());
                value.setAssetId(n.getAssetId());
                return value;
            }

            if (path.equals(prefix)) {
                // url能匹配到该节点，且url无需继续向下匹配(url长度等同当前节点前缀)
                if (n.getAssetId() == null) {
                    // 发现这个节点并不是资产，就需要回溯到上一个路径参数
                    for (int length = skippedNodes.size(); length > 0; length--) {
                        SkippedNode skippedNode = skippedNodes.get(skippedNodes.size() - 1);
                        skippedNodes.remove(length - 1);
                        if (skippedNode.getPath().endsWith(path)) {
                            path = skippedNode.getPath();
                            n = skippedNode.getNode();
                            continue walk;
                        }
                    }
                }


            }
        }
    }

//    @SneakyThrows
//    public NodeValue getValue(String path, List<SkippedNode> skippedNodes, boolean unescape) {
//        NodeValue value = new NodeValue();
//
//        Node n = this;
//
//        walk:
//        while (true) {
//            String prefix = n.getPath();
//            // 一个url进来，一般就几种情况：
//            // 1. 该url符合该节点的path，继续匹配子节点
//            // 2. 没有匹配到结果
//
//            if (path.length() > prefix.length() && path.startsWith(prefix)) {
//                path = path.substring(prefix.length());
//
//                // Try all the non-wildcard children first by matching the indices
//                // 先尝试匹配所有static的路径
//                char idxc = path.charAt(0);
//                for (int i = 0; i < n.getIndices().length(); i++) {
//                    char c = n.getIndices().charAt(i);
//                    if (c == idxc) {
//                        //  strings.HasPrefix(n.children[len(n.children)-1].path, ":") == n.wildChild
//                        if (n.isWildChild()) {
//                            // 如果该节点存在通配子节点，需要把这个节点带上，万一static节点没有匹配上
//                            skippedNodes.add(new SkippedNode(prefix + path, new Node(n.getPath(), "", n.isWildChild(), n.getNType(), n.getChildren(), n.getHandlers(), n.getFullPath()), globalParamsCount));
//                        }
//
//                        n = n.getChildren().get(i);
//                        continue walk;
//                    }
//                }
//
//                // 证明当前节点下已经不存在可能能匹配的static节点了
//                if (!n.isWildChild()) {
//                    // If the path at the end of the loop is not equal to '/' and the current node has no child nodes
//                    // the current node needs to roll back to last valid skippedNode
//
//                    // 由于输入输出的所有url都是携带末尾/的，所以能到这里的path肯定还剩下路径，需要回溯到上层的通配节点
//                    for (int length = skippedNodes.size(); length > 0; length--) {
//                        SkippedNode skippedNode = skippedNodes.get(length - 1);
//                        skippedNodes.remove(length - 1);
//
//                        if (skippedNode.getPath().endsWith(path)) {
//                            path = skippedNode.getPath();
//                            n = skippedNode.getNode();
//                            continue walk;
//                        }
//                    }
//
//                    // Nothing found.
//                    // We can recommend to redirect to the same URL without a
//                    // trailing slash if a leaf exists for that path.
//
//                    // 未找到匹配的节点，返回失败
//                    value.setTsr(false);
//                    return value;
//                }
//
//                n = n.getChildren().get(n.getChildren().size() - 1);
//
//                if (n.getNType() == NodeType.PARAM) {
//                    // fix truncate the parameter
//                    // Find param end (either '/' or path end)
//
//                    int end = 0;
//                    while (end < path.length() && path.charAt(end) != '/') {
//                        end++;
//                    }
//
//                    // we need to go deeper!
//                    if (end < path.length()) {
//                        if (n.getChildren().size() > 0) {
//                            path = path.substring(end);
//                            n = n.getChildren().get(0);
//                            continue walk;
//                        }
//
//                        // ... but we can't
//                        value.setTsr(path.length() == end + 1);
//                        return value;
//                    }
//
//                    if (n.getChildren().size() == 1) {
//                        // No handle found. Check if a handle for this path + a
//                        // trailing slash exists for TSR recommendation
//                        n = n.getChildren().get(0);
//                        value.setTsr((n.getPath().equals("/") && n.getHandlers() != null) || (n.getPath().equals("") && n.getIndices().equals("/")));
//                    }
//                    return value;
//                }
//
//                throw new Exception("invalid node type");
//            }
//
//            if (path.equals(prefix)) {
//                // If the current path does not equal '/' and the node does not have a registered handle and the most recently matched node has a child node
//                // the current node needs to roll back to last valid skippedNode
//                if (n.getHandlers() == null && !path.equals("/")) {
//                    for (int length = skippedNodes.size(); length > 0; length--) {
//                        SkippedNode skippedNode = skippedNodes.get(length - 1);
//                        skippedNodes.remove(length - 1);
//                        if (skippedNode.getPath().endsWith(path)) {
//                            path = skippedNode.getPath();
//                            n = skippedNode.getNode();
//                            if (value.getParams() != null) {
//                                value.getParams().setParams(value.getParams().getParams().subList(0, skippedNode.getParamsCount()));
//                            }
//                            globalParamsCount = skippedNode.getParamsCount();
//                            continue walk;
//                        }
//                    }
//                    //	n = latestNode.children[len(latestNode.children)-1]
//                }
//                // We should have reached the node containing the handle.
//                // Check if this node has a handle registered.
//                value.setHandlers(n.getHandlers());
//                if (value.getHandlers() != null) {
//                    value.setFullPath(n.getFullPath());
//                    return value;
//                }
//                // If there is no handle for this route, but this route has a
//                // wildcard child, there must be a handle for this path with an
//                // additional trailing slash
//                if (path.equals("/") && n.isWildChild() && n.getNType() != NodeType.ROOT) {
//                    value.setTsr(true);
//                    return value;
//                }
//
//                if (path.equals("/") && n.getNType() == NodeType.STATIC) {
//                    value.setTsr(true);
//                    return value;
//                }
//
//                // No handle found. Check if a handle for this path + a
//                // trailing slash exists for trailing slash recommendation
//                for (int i = 0; i < n.getIndices().length(); i++) {
//                    char c = n.getIndices().charAt(i);
//                    if (c == '/') {
//                        n = n.getChildren().get(i);
//                        value.setTsr((n.getPath().length() == 1 && n.getHandlers() != null) || (n.getNType() == NodeType.CATCH_ALL && n.getChildren().get(0).getHandlers() != null));
//                        return value;
//                    }
//                }
//
//                return value;
//            }
//
//            // Nothing found. We can recommend to redirect to the same URL with an
//            // extra trailing slash if a leaf exists for that path
//            value.setTsr(!path.equals("/") || (prefix.length() == path.length() + 1 && prefix.charAt(path.length()) == '/' && path.equals(prefix.substring(0, prefix.length() - 1)) && n.getHandlers() != null));
//
//            // roll back to last valid skippedNode
//            if (!value.isTsr() && !path.equals("/")) {
//                for (int length = skippedNodes.size(); length > 0; length--) {
//                    SkippedNode skippedNode = skippedNodes.get(length - 1);
//                    skippedNodes.remove(length - 1);
//                    if (skippedNode.getPath().endsWith(path)) {
//                        path = skippedNode.getPath();
//                        n = skippedNode.getNode();
//                        if (value.getParams() != null) {
//                            value.getParams().setParams(value.getParams().getParams().subList(0, skippedNode.getParamsCount()));
//                        }
//                        globalParamsCount = skippedNode.getParamsCount();
//                        continue walk;
//                    }
//                }
//            }
//
//            return value;
//        }
//    }
}
