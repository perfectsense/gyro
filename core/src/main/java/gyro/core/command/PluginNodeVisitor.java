package gyro.core.command;

import gyro.core.scope.Defer;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.BinaryNode;
import gyro.lang.ast.value.IndexedNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;

import java.util.List;

public class PluginNodeVisitor implements NodeVisitor<List<DirectiveNode>, Object, RuntimeException> {

    public void visitBody(List<Node> body, List<DirectiveNode> context) {
        Defer.execute(body, i -> visit(i, context));
    }

    @Override
    public Object visitDirective(DirectiveNode node, List<DirectiveNode> context) {
        if ("plugin".equals(node.getName())) {
            context.add(node);
        }

        return null;
    }

    @Override
    public Object visitPair(PairNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitFile(FileNode node, List<DirectiveNode> context) {
        visitBody(node.getBody(), context);
        return null;
    }

    @Override
    public Object visitKeyBlock(KeyBlockNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitResource(ResourceNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitBinary(BinaryNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitIndexed(IndexedNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitInterpolatedString(InterpolatedStringNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitList(ListNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitMap(MapNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitReference(ReferenceNode node, List<DirectiveNode> context) {
        return null;
    }

    @Override
    public Object visitValue(ValueNode node, List<DirectiveNode> context) {
        return null;
    }
}
