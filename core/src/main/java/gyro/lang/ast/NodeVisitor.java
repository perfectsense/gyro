package gyro.lang.ast;

import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.PluginNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.block.VirtualResourceNode;
import gyro.lang.ast.condition.AndConditionNode;
import gyro.lang.ast.condition.ComparisonConditionNode;
import gyro.lang.ast.condition.OrConditionNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.control.IfNode;
import gyro.lang.ast.value.BooleanNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.LiteralStringNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.NumberNode;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.lang.ast.value.ValueReferenceNode;

public interface NodeVisitor<C> {

    default Object visit(Node node, C context) {
        return node.accept(this, context);
    }

    Object visitDirective(DirectiveNode node, C context);

    Object visitPair(PairNode node, C context);

    Object visitFile(FileNode node, C context);

    Object visitKeyBlock(KeyBlockNode node, C context);

    Object visitPlugin(PluginNode node, C context);

    Object visitResource(ResourceNode node, C context);

    Object visitVirtualResource(VirtualResourceNode node, C context);

    Object visitAndCondition(AndConditionNode node, C context);

    Object visitComparisonCondition(ComparisonConditionNode node, C context);

    Object visitOrCondition(OrConditionNode node, C context);

    Object visitValueCondition(ValueConditionNode node, C context);

    Object visitFor(ForNode node, C context);

    Object visitIf(IfNode node, C context);

    Object visitBoolean(BooleanNode node, C context);

    Object visitInterpolatedString(InterpolatedStringNode node, C context);

    Object visitList(ListNode node, C context);

    Object visitLiteralString(LiteralStringNode node, C context);

    Object visitMap(MapNode node, C context);

    Object visitNumber(NumberNode node, C context);

    Object visitResourceReference(ResourceReferenceNode node, C context);

    Object visitValueRefence(ValueReferenceNode node, C context);

}
