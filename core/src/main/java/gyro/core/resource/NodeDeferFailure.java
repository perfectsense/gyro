package gyro.core.resource;

import gyro.core.GyroCore;
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

public class NodeDeferFailure implements NodeVisitor<DeferError, String> {

    @Override
    public String visitDirective(DirectiveNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitPair(PairNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitFile(FileNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitKeyBlock(KeyBlockNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitResource(ResourceNode node, DeferError context) {
        return String.format(
            "Resource type '%s' does not exist. Verify the resource name is correct and that you have included the correct provider plugin in %s",
            node.getType(),
            GyroCore.INIT_FILE);
    }

    @Override
    public String visitBinary(BinaryNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitIndexed(IndexedNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitInterpolatedString(InterpolatedStringNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitList(ListNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitMap(MapNode node, DeferError context) {
        return node.toString();
    }

    @Override
    public String visitReference(ReferenceNode node, DeferError context) {
        return String.format(
            "Can't resolve reference! [%s]",
            node.getLocation());
    }

    @Override
    public String visitValue(ValueNode node, DeferError context) {
        return node.toString();
    }

}
