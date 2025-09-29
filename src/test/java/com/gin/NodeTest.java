package com.gin;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class NodeTest {
    @Test
    public void addStaticRoute2EmptyTree() {
        Node root = new Node();
        root.addRoute("/api/test/", "api./api/test/");

        Assert.assertEquals("/api/test/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(0, root.getChildren().size());
        Assert.assertEquals("api./api/test/", root.getAssetId());
    }

    @Test
    public void addParamRoute2EmptyTree() {
        Node root = new Node();
        root.addRoute("/api/{id}/test/", "api./api/{id}/test/");

        Assert.assertEquals("/api/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertNull(root.getAssetId());
        Assert.assertEquals("", root.getIndices());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(1, child1.getChildren().size());
        Assert.assertNull(child1.getAssetId());
        Assert.assertEquals("t", child1.getIndices());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("test/", child2.getPath());
        Assert.assertEquals(NodeType.STATIC, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
        Assert.assertEquals("api./api/{id}/test/", child2.getAssetId());
        Assert.assertEquals("", child2.getIndices());
    }

    @Test
    public void addParamRoute2EmptyTree2() {
        Node root = new Node();
        root.addRoute("/api/{id}/");

        Assert.assertEquals("/api/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertEquals("", root.getIndices());
        Assert.assertNull(root.getAssetId());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(0, child1.getChildren().size());
        Assert.assertEquals("", child1.getIndices());
        Assert.assertEquals("api./api/{id}/", child1.getAssetId());
    }

    @Test
    public void addParamRoute2EmptyTree3() {
        Node root = new Node();
        root.addRoute("/{id}/test/");

        Assert.assertEquals("/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertEquals("", root.getIndices());
        Assert.assertNull(root.getAssetId());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(1, child1.getChildren().size());
        Assert.assertEquals("t", child1.getIndices());
        Assert.assertNull(child1.getAssetId());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("test/", child2.getPath());
        Assert.assertEquals(NodeType.STATIC, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
        Assert.assertEquals("", child2.getIndices());
        Assert.assertEquals("api./{id}/test/", child2.getAssetId());
    }

    @Test
    public void addRoute2Tree() {
        Node root = new Node();
        root.addRoute("/api/test/");
        root.addRoute("/api/test/{id}/");

        Assert.assertEquals("/api/test/", root.getPath());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertTrue(root.isWildChild());
        Assert.assertEquals("api./api/test/", root.getAssetId());
        Assert.assertEquals("", root.getIndices());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("{id}/", child1.getPath());
        Assert.assertEquals(NodeType.PARAM, child1.getNType());
        Assert.assertEquals(0, child1.getChildren().size());
        Assert.assertFalse(child1.isWildChild());
        Assert.assertEquals("api./api/test/{id}/", child1.getAssetId());
        Assert.assertEquals("", child1.getIndices());
    }

    @Test
    public void addRoute2Tree2() {
        Node root = new Node();
        root.addRoute("/{id}/test/");
        root.addRoute("/api/test/{id}/");
        root.addRoute("/{id}/aaa/");
        root.addRoute("/{id}/{id}/");

        Assert.assertEquals("/", root.getPath());
        Assert.assertEquals("a", root.getIndices());
        Assert.assertEquals(NodeType.ROOT, root.getNType());
        Assert.assertEquals(2, root.getChildren().size());
        Assert.assertTrue(root.isWildChild());
        Assert.assertNull(root.getAssetId());

        Node child1 = root.getChildren().get(0);
        Assert.assertEquals("api/test/", child1.getPath());
        Assert.assertEquals(NodeType.STATIC, child1.getNType());
        Assert.assertEquals(1, child1.getChildren().size());
        Assert.assertTrue(child1.isWildChild());
        Assert.assertNull(child1.getAssetId());
        Assert.assertEquals("", child1.getIndices());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("{id}/", child2.getPath());
        Assert.assertEquals(NodeType.PARAM, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
        Assert.assertEquals("api./api/test/{id}/", child2.getAssetId());

        Node child3 = root.getChildren().get(1);
        Assert.assertEquals("{id}/", child3.getPath());
        Assert.assertEquals("ta", child3.getIndices());
        Assert.assertEquals(NodeType.PARAM, child3.getNType());
        Assert.assertEquals(3, child3.getChildren().size());

        Node child4 = child3.getChildren().get(0);
        Assert.assertEquals("test/", child4.getPath());
        Assert.assertEquals(NodeType.STATIC, child4.getNType());
        Assert.assertEquals(0, child4.getChildren().size());
        Assert.assertEquals("api./{id}/test/", child4.getAssetId());

        Node child5 = child3.getChildren().get(1);
        Assert.assertEquals("aaa/", child5.getPath());
        Assert.assertEquals(NodeType.STATIC, child5.getNType());
        Assert.assertEquals(0, child5.getChildren().size());
        Assert.assertEquals("api./{id}/aaa/", child5.getAssetId());

        Node child6 = child3.getChildren().get(2);
        Assert.assertEquals("{id}/", child6.getPath());
        Assert.assertEquals(NodeType.PARAM, child6.getNType());
        Assert.assertEquals(0, child6.getChildren().size());
        Assert.assertEquals("api./{id}/{id}/", child6.getAssetId());

        root.addRoute("/api/te/{id}/");
        root.addRoute("/{id/test/");
        root.addRoute("/apiii/test/");

        Assert.assertEquals("a{", root.getIndices());
        Assert.assertEquals("{id/test/", root.getChildren().get(1).getPath());

        Assert.assertEquals("/i", root.getChildren().get(0).getIndices());
    }
}
