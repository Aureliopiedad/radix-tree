package com.gin;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class NodeTest {
    @Test
    public void addStaticRoute2EmptyTree() {
        Node root = new Node();
        root.addRoute("/api/test/");

        Assert.assertEquals("/api/test/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(0, root.getChildren().size());
    }

    @Test
    public void addParamRoute2EmptyTree() {
        Node root = new Node();
        root.addRoute("/api/{id}/test/");

        Assert.assertEquals("/api/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(1, child1.getChildren().size());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("test/", child2.getPath());
        Assert.assertEquals(NodeType.STATIC, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
    }

    @Test
    public void addParamRoute2EmptyTree2() {
        Node root = new Node();
        root.addRoute("/api/{id}/");

        Assert.assertEquals("/api/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(0, child1.getChildren().size());
    }

    @Test
    public void addParamRoute2EmptyTree3() {
        Node root = new Node();
        root.addRoute("/{id}/test/");

        Assert.assertEquals("/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(1, child1.getChildren().size());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("test/", child2.getPath());
        Assert.assertEquals(NodeType.STATIC, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
    }
}
