package gyro.lang.ast;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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

public class NodePrinter implements NodeVisitor<PrinterContext, Void> {

    public static String toString(Node node) {
        StringBuilder builder = new StringBuilder();
        NodePrinter printer = new NodePrinter();
        PrinterContext context = new PrinterContext(builder, 0);

        printer.visit(node, context);

        return builder.toString();
    }

    public void visitBody(List<Node> body, PrinterContext context) {
        for (Node item : body) {
            context.appendNewline();
            visit(item, context);
        }
    }

    @Override
    public Void visitDirective(DirectiveNode node, PrinterContext context) {
        context.appendNewline();
        context.append("@import ");
        context.append(node.getDirectiveFile());

        Optional.ofNullable(node.getName())
            .ifPresent(name -> {
                context.append(" as ");
                context.append(name);
            });

        return null;
    }

    @Override
    public Void visitPair(PairNode node, PrinterContext context) {
        context.append(node.getKey());
        context.append(": ");
        visit(node.getValueNode(), context.indented());

        return null;
    }

    @Override
    public Void visitFile(FileNode node, PrinterContext context) {
        visitBody(node.getBody(), context);

        return null;
    }

    @Override
    public Void visitKeyBlock(KeyBlockNode node, PrinterContext context) {
        context.appendNewline();
        context.append(node.getKey());
        visitBody(node.getBody(), context.indented());
        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitPlugin(PluginNode node, PrinterContext context) {
        context.appendNewline();
        context.append("plugin");
        visitBody(node.getBody(), context.indented());
        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitResource(ResourceNode node, PrinterContext context) {
        context.appendNewline();
        context.append(node.getType());

        Optional.ofNullable(node.getNameNode())
            .ifPresent(nameNode -> {
                context.append(' ');
                visit(nameNode, context);
            });

        visitBody(node.getBody(), context.indented());
        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitVirtualResource(VirtualResourceNode node, PrinterContext context) {
        return null;
    }

    @Override
    public Void visitAndCondition(AndConditionNode node, PrinterContext context) {
        visit(node.getLeftNode(), context);
        context.append(" and ");
        visit(node.getRightNode(), context);

        return null;
    }

    @Override
    public Void visitComparisonCondition(ComparisonConditionNode node, PrinterContext context) {
        visit(node.getLeftNode(), context);
        context.append(" ");
        context.append(node.getOperator());
        context.append(" ");
        visit(node.getRightNode(), context);

        return null;
    }

    @Override
    public Void visitOrCondition(OrConditionNode node, PrinterContext context) {
        visit(node.getLeftNode(), context);
        context.append(" or ");
        visit(node.getRightNode(), context);

        return null;
    }

    @Override
    public Void visitValueCondition(ValueConditionNode node, PrinterContext context) {
        visit(node.getLeftNode(), context);

        return null;
    }

    @Override
    public Void visitFor(ForNode node, PrinterContext context) {
        context.appendNewline();
        context.append("for ");
        context.append(String.join(", ", node.getVariables()));
        context.append(" in [");

        for (Iterator<Node> i = node.getItems().iterator(); i.hasNext(); ) {
            Node item = i.next();

            visit(item, context);

            if (i.hasNext()) {
                context.append(", ");
            }
        }

        context.append("]");
        visitBody(node.getBody(), context.indented());
        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitIf(IfNode node, PrinterContext context) {
        List<Node> conditions = node.getConditions();
        List<List<Node>> bodies = node.getBodies();

        for (int i = 0; i < conditions.size(); i++) {
            context.append(i == 0 ? "if " : "else if ");
            visit(conditions.get(i), context);
            visitBody(bodies.get(i), context.indented());
            context.appendNewline();
        }

        if (bodies.size() > conditions.size()) {
            context.appendNewline();
            context.append("else");
            visitBody(bodies.get(bodies.size() - 1), context.indented());
        }

        context.appendNewline();
        context.append("end");

        return null;
    }

    @Override
    public Void visitBoolean(BooleanNode node, PrinterContext context) {
        context.append(String.valueOf(node.getValue()));

        return null;
    }

    @Override
    public Void visitInterpolatedString(InterpolatedStringNode node, PrinterContext context) {
        context.append('"');

        for (Object item : node.getItems()) {
            if (item instanceof Node) {
                visit((Node) item, context);

            } else {
                context.append(String.valueOf(item));
            }
        }

        context.append('"');

        return null;
    }

    @Override
    public Void visitList(ListNode node, PrinterContext context) {
        context.append('[');

        for (Iterator<Node> i = node.getItems().iterator(); i.hasNext(); ) {
            Node item = i.next();

            visit(item, context);

            if (i.hasNext()) {
                context.append(", ");
            }
        }

        context.append(']');

        return null;
    }

    @Override
    public Void visitLiteralString(LiteralStringNode node, PrinterContext context) {
        context.append('\"');
        context.append(node.getValue());
        context.append('\"');

        return null;
    }

    @Override
    public Void visitMap(MapNode node, PrinterContext context) {
        context.append('{');

        for (Iterator<PairNode> i = node.getEntries().iterator(); i.hasNext(); ) {
            PairNode entry = i.next();

            visit(entry, context);

            if (i.hasNext()) {
                context.append(", ");
            }
        }

        context.append('}');

        return null;
    }

    @Override
    public Void visitNumber(NumberNode node, PrinterContext context) {
        context.append(String.valueOf(node.getValue()));

        return null;
    }

    @Override
    public Void visitResourceReference(ResourceReferenceNode node, PrinterContext context) {
        context.append("$(");
        context.append(node.getType());

        Optional.ofNullable(node.getNameNode())
            .ifPresent(nameNode -> {
                context.append(' ');
                visit(nameNode, context);
            });

        Optional.ofNullable(node.getPath())
            .ifPresent(path -> {
                context.append(" | ");
                context.append(path);
            });

        context.append(")");

        return null;
    }

    @Override
    public Void visitValueReference(ValueReferenceNode node, PrinterContext context) {
        context.append("$(");
        context.append(node.getPath());
        context.append(")");

        return null;
    }

}
