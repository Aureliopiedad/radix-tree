package com.gin;

import com.gin.util.TreeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <a href="https://github.com/gin-gonic/gin/blob/master/tree.go#L417">参考文件</a>
 * 检索和插入使用的是label\continue label的做法，原因是堆栈层数可能会过多
 * 由于是线程不安全的，每次addRoute之前需要加锁
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private String path = "";
    private NodeType nType = NodeType.STATIC;
    private Node parent = null;
    private List<Node> children = new ArrayList<>();
    private String assetId;

    @Override
    public String toString() {
        return "Node{" +
                "path='" + path + '\'' +
                ", nType=" + nType +
                ", parent.path=" + (parent == null ? "null" : parent.getPath())+
                ", children.size=" + children.size() +
                ", assetId='" + assetId + '\'' +
                '}';
    }

    public String getIndices() {
        return this.getChildren().stream().filter(node -> node.getNType() != NodeType.PARAM).map(Node::getPath).map(path -> path.substring(0, 1)).collect(Collectors.joining());
    }

    public boolean isWildChild() {
        if (children.isEmpty()) {
            return false;
        }

        return this.getChildren().get(this.getChildren().size() - 1).getNType() == NodeType.PARAM;
    }

    /**
     * addChild will add a child node, keeping wildcardChild at the end
     *
     * @param child
     */
    private void addChild(Node child) {
        if (this.isWildChild() && !this.getChildren().isEmpty()) {
            this.getChildren().add(this.getChildren().size() - 1, child);
            return;
        }

        this.getChildren().add(child);
    }

    @SneakyThrows
    private void insertChild(String path, String fullPath, String assetId) {
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
            child.setParent(n);

            n.addChild(child);
            n = child;

            // if the path doesn't end with the wildcard, then there will be another subpath starting with '/'
            // wildcard.getWildcard() 指的是url中第一个匹配到的路径参数，path现在是该路径参数到url末尾的剩余字符串，例如{id}/ 和 {id}/test，且路径参数后第一个字符一定是/
            // 如果遇到这种情况，那么意味着需要再走一次循环，将下一个subPath添加到children
            if (wildcard.getWildcard().length() < path.length()) {
                path = path.substring(wildcard.getWildcard().length());
                Node child1 = new Node();
                child1.setParent(n);

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
    }

    @SneakyThrows
    public void addRoute(String path) {
        addRoute(path, "api." + path);
    }

    /**
     * addRoute adds a node with the given handle to the path.
     * Not concurrency-safe! 需要加锁
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
                        NodeType.STATIC,
                        n,
                        n.getChildren(),
                        n.getAssetId()
                );

                n.getChildren().forEach(node -> node.setParent(child));

                // 新的节点直接变成当前节点的子节点
                n.setChildren(new ArrayList<>());
                n.getChildren().add(child);

                // 重置当前节点属性
                n.setPath(path.substring(0, i));
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
                    for (int j = 0; j < n.getChildren().size(); j++) {
                        if (n.getChildren().get(j).getNType() != NodeType.PARAM && n.getChildren().get(j).getPath().charAt(0) == c) {
                            n = n.getChildren().get(j);
                            continue walk;
                        }
                    }

                    Node child = new Node();
                    child.setParent(n);
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

            n.setAssetId(assetId);
            return;
        }
    }

    @SneakyThrows
    public Node getValue(String path, List<SkippedNode> skippedNodes) {
        Node n = this;

        walk:
        while (true) {
            String prefix = null;

            if (path.equals(n.getPath())) {
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

                return n;
            } else if (n.getNType() == NodeType.PARAM) {
                prefix = path.substring(0, path.indexOf("/") + 1);
                path = path.substring(path.indexOf("/") + 1);
            } else if (path.length() > n.getPath().length() && path.startsWith(n.getPath())) {
                // url能匹配到该节点，且url还需要继续向下匹配(url长度超过当前节点前缀)
                path = path.substring(n.getPath().length());
                prefix = n.getPath();
            } else {
                for (int length = skippedNodes.size(); length > 0; length--) {
                    SkippedNode skippedNode = skippedNodes.get(length - 1);
                    skippedNodes.remove(length - 1);

                    if (skippedNode.getPath().endsWith(path)) {
                        path = skippedNode.getPath();
                        n = skippedNode.getNode();
                        continue walk;
                    }
                }

                return n;
            }

            // 先检查一下子节点有没有静态url能匹配的
            char idxc = path.charAt(0);
            for (int i = 0; i < n.getChildren().size(); i++) {
                if (n.getChildren().get(i).getNType() != NodeType.PARAM && n.getChildren().get(i).getPath().charAt(0) == idxc) {
                    if (n.isWildChild()) {
                        skippedNodes.add(new SkippedNode(prefix + path, new Node(n.getPath(), n.getNType(), null, n.getChildren().subList(n.getChildren().size() - 1, n.getChildren().size()), n.getAssetId()), 0));
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
                return n;
            }

            // 要么当前节点的子节点有路径参数节点，可以继续匹配路径参数
            n = n.getChildren().get(n.children.size() - 1);
            // 不考虑不存在/的情况
            int end = path.indexOf("/");
            if (end + 1 < path.length()) {
                // 在路径参数之后还有path需要匹配
                continue walk;
            }

            return n;
        }
    }

    /**
     * 这个传入的是数据库中的uri，因此不用考虑路径参数带来的影响，直接用path匹配即可，也有可能返回null
     *
     * @param path
     * @return
     */
    private Node getValue(String path) {
        Node n = this;

        walk:
        while (true) {
            if (path.equals(n.getPath())) {
                return n;
            }

            if (path.length() > n.getPath().length() && path.startsWith(n.getPath())) {
                path = path.substring(n.getPath().length());
                for (Node child : n.getChildren()) {
                    if (path.startsWith(child.getPath())) {
                        n = child;
                        continue walk;
                    }
                }
            }

            return null;
        }
    }

    public void remove(String path) {
        Node node = getValue(path);
        if (node == null) {
            return;
        }
        node.remove();
    }

    /**
     * 需要加锁
     */
    private void remove() {
        // 先清除自身节点的资产属性
        this.setAssetId(null);

        if (this.getNType() == NodeType.ROOT) {
            return;
        }

        // 查询子节点状态

        // 如果没有子节点，这个节点直接移除即可
        if (this.getChildren().isEmpty()) {
            this.getParent().getChildren().removeIf(node -> node.getPath().equals(this.getPath()));
            // 需要考虑剪枝，避免出现父节点不是资产，且没有子节点或一个非路径参数子节点的情况
            this.getParent().cut();
            return;
        }

        // 如果只有一个子节点，需要考虑子节点是否是路径参数节点；
        // 如果不是，那么子节点上移；如果是，那么不变
        if (this.getChildren().size() == 1) {
            if (this.isWildChild()) {
                return;
            }

            if (this.getNType() == NodeType.PARAM) {
                return;
            }

            Node child = this.getChildren().get(0);
            this.setPath(this.path + child.getPath());
            child.getChildren().forEach(node -> node.setParent(this));
            this.setChildren(child.getChildren());
            this.setAssetId(child.getAssetId());
        }

        // 如果有多个子节点 保持不变
    }

    private void cut() {
        Node n = this;

        walk:
        while (true) {
            // 剪枝是因为当前节点删除了一个子节点，如果只剩下一个子节点的话需要将子节点合并上去

            // 如果当前节点没有子节点了
            if (n.getChildren().isEmpty()) {
                if (n.getAssetId() != null) {
                    // 如果当前节点是api资产，跳过
                    return;
                }

                if (n.getNType() == NodeType.ROOT) {
                    n.setPath("");
                    return;
                }

                String path = n.getPath();
                n.getParent().getChildren().removeIf(node -> node.getPath().equals(path));
                n = n.getParent();
                continue walk;
            }

            if (n.getNType() == NodeType.PARAM) {
                // 如果当前节点是路径参数节点，那么不合并
                return;
            }

            if (n.getChildren().size() > 1) {
                // 当前节点的子节点数量大于1，不合并
                return;
            }

            if (n.getChildren().size() == 1) {
                // 当前节点的子节点数量等于1，分情况
                if (n.getChildren().get(0).getNType() == NodeType.PARAM) {
                    // 如果子节点是路径参数，不合并
                    return;
                }

                if (n.getAssetId() != null) {
                    // 如果当前节点是api资产，那么不能将子节点合并
                    return;
                }

                Node child = n.getChildren().get(0);

                n.setPath(n.path + child.getPath());
                for (Node childChild : child.getChildren()) {
                    childChild.setParent(n);
                }
                n.setChildren(child.getChildren());
                n.setAssetId(child.getAssetId());
                return;
            }

            return;
        }
    }
}
