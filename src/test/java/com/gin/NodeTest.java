package com.gin;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

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
        Assert.assertEquals("/api/", child1.getParent().getPath());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("test/", child2.getPath());
        Assert.assertEquals(NodeType.STATIC, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
        Assert.assertEquals("api./api/{id}/test/", child2.getAssetId());
        Assert.assertEquals("", child2.getIndices());
        Assert.assertEquals("{id}/", child2.getParent().getPath());
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
        Assert.assertEquals("/api/", child1.getParent().getPath());
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
        Assert.assertEquals("/", child1.getParent().getPath());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("test/", child2.getPath());
        Assert.assertEquals(NodeType.STATIC, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
        Assert.assertEquals("", child2.getIndices());
        Assert.assertEquals("api./{id}/test/", child2.getAssetId());
        Assert.assertEquals("{id}/", child2.getParent().getPath());
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
        Assert.assertEquals("/api/test/", child1.getParent().getPath());
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
        Assert.assertEquals("/", child1.getParent().getPath());

        Node child2 = child1.getChildren().get(0);
        Assert.assertEquals("{id}/", child2.getPath());
        Assert.assertEquals(NodeType.PARAM, child2.getNType());
        Assert.assertEquals(0, child2.getChildren().size());
        Assert.assertEquals("api./api/test/{id}/", child2.getAssetId());
        Assert.assertEquals("api/test/", child2.getParent().getPath());

        Node child3 = root.getChildren().get(1);
        Assert.assertEquals("{id}/", child3.getPath());
        Assert.assertEquals("ta", child3.getIndices());
        Assert.assertEquals(NodeType.PARAM, child3.getNType());
        Assert.assertEquals(3, child3.getChildren().size());
        Assert.assertEquals("/", child3.getParent().getPath());

        Node child4 = child3.getChildren().get(0);
        Assert.assertEquals("test/", child4.getPath());
        Assert.assertEquals(NodeType.STATIC, child4.getNType());
        Assert.assertEquals(0, child4.getChildren().size());
        Assert.assertEquals("api./{id}/test/", child4.getAssetId());
        Assert.assertEquals("{id}/", child4.getParent().getPath());

        Node child5 = child3.getChildren().get(1);
        Assert.assertEquals("aaa/", child5.getPath());
        Assert.assertEquals(NodeType.STATIC, child5.getNType());
        Assert.assertEquals(0, child5.getChildren().size());
        Assert.assertEquals("api./{id}/aaa/", child5.getAssetId());
        Assert.assertEquals("{id}/", child5.getParent().getPath());

        Node child6 = child3.getChildren().get(2);
        Assert.assertEquals("{id}/", child6.getPath());
        Assert.assertEquals(NodeType.PARAM, child6.getNType());
        Assert.assertEquals(0, child6.getChildren().size());
        Assert.assertEquals("api./{id}/{id}/", child6.getAssetId());
        Assert.assertEquals("{id}/", child6.getParent().getPath());

        root.addRoute("/api/te/{id}/");
        root.addRoute("/{id/test/");
        root.addRoute("/apiii/test/");

        Assert.assertEquals("a{", root.getIndices());
        Assert.assertEquals("{id/test/", root.getChildren().get(1).getPath());

        Assert.assertEquals("/i", root.getChildren().get(0).getIndices());
    }

    @Test
    public void getValueTest() {
        Node root = new Node();
        root.addRoute("/{id}/test/");
        root.addRoute("/api/test/{id}/");
        root.addRoute("/{id}/aaa/");
        root.addRoute("/{id}/{id}/");
        root.addRoute("/api/te/{id}/");
        root.addRoute("/{id/test/");
        root.addRoute("/apiii/test/");

        Assert.assertEquals(root.getValue("/123/test/", new ArrayList<>()).getAssetId(), "api./{id}/test/");
        Assert.assertEquals(root.getValue("/api/test/123/", new ArrayList<>()).getAssetId(), "api./api/test/{id}/");
        Assert.assertEquals(root.getValue("/123/aaa/", new ArrayList<>()).getAssetId(), "api./{id}/aaa/");
        Assert.assertEquals(root.getValue("/123/123/", new ArrayList<>()).getAssetId(), "api./{id}/{id}/");
        Assert.assertEquals(root.getValue("/{id/test/", new ArrayList<>()).getAssetId(), "api./{id/test/");
        Assert.assertEquals(root.getValue("/apiii/test/", new ArrayList<>()).getAssetId(), "api./apiii/test/");

        root.addRoute("/test/test/test/");
        root.addRoute("/test/test/123/");
        Assert.assertEquals(root.getValue("/test/test/", new ArrayList<>()).getAssetId(), "api./{id}/test/");

        root.addRoute("/test/test/{id}/111/");
        root.addRoute("/test/test/123/222/");

        root.addRoute("/test/test/124/{id}/");
        root.addRoute("/test/test/124/222/");
        Assert.assertEquals(root.getValue("/test/test/123/111/", new ArrayList<>()).getAssetId(), "api./test/test/{id}/111/");
        Assert.assertEquals(root.getValue("/test/test/124/22/", new ArrayList<>()).getAssetId(), "api./test/test/124/{id}/");
    }

    @Test
    public void removeTest() {
        Node root = new Node();
        root.addRoute("/{id}/test/");
        root.addRoute("/{id}/{id}/");

        Assert.assertEquals(root.getValue("/test/test/", new ArrayList<>()).getAssetId(), "api./{id}/test/");
        root.remove("/{id}/test/");
        Assert.assertEquals(root.getValue("/test/test/", new ArrayList<>()).getAssetId(), "api./{id}/{id}/");
        root.remove("/{id}/{id}/");
        Assert.assertNull(root.getValue("/test/test/", new ArrayList<>()).getAssetId());

        root.addRoute("/test/{id}/test/");
        root.addRoute("/test/{id}/");
        Assert.assertEquals(root.getValue("/test/111/test/", new ArrayList<>()).getAssetId(), "api./test/{id}/test/");
        Assert.assertEquals(root.getValue("/test/111/", new ArrayList<>()).getAssetId(), "api./test/{id}/");
        root.remove("/test/{id}/");
        root.remove("/test/{id}/test/");

        root.addRoute("/test/test/111/");
        root.addRoute("/test/test/123/");
        root.addRoute("/test/test/111/1/");
        root.addRoute("/test/test/111/2/");
        Assert.assertEquals("/test/test/1", root.getPath());
        Assert.assertEquals(2, root.getChildren().size());
        Assert.assertEquals("11/", root.getChildren().get(0).getPath());
        Assert.assertEquals("23/", root.getChildren().get(1).getPath());
        Assert.assertEquals("1/", root.getChildren().get(0).getChildren().get(0).getPath());
        Assert.assertEquals("11/", root.getChildren().get(0).getChildren().get(0).getParent().getPath());
        Assert.assertEquals("2/", root.getChildren().get(0).getChildren().get(1).getPath());
        Assert.assertEquals("11/", root.getChildren().get(0).getChildren().get(0).getParent().getPath());
        Assert.assertEquals(root.getValue("/test/test/111/", new ArrayList<>()).getAssetId(), "api./test/test/111/");
        root.remove("/test/test/111/");
        Assert.assertEquals("/test/test/1", root.getPath());
    }
}
