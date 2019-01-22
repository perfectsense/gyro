package beam.lang.ast;

import java.util.List;
import java.util.Optional;

import beam.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang.StringUtils;

public abstract class Node {

    public static Node create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(BeamParser.BeamFileContext.class)) {
            return new RootNode((BeamParser.BeamFileContext) context);

        } else if (cc.equals(BeamParser.BooleanValueContext.class)) {
            return new BooleanNode((BeamParser.BooleanValueContext) context);

        } else if (cc.equals(BeamParser.ForStmtContext.class)) {
            return new ForNode((BeamParser.ForStmtContext) context);

        } else if (cc.equals(BeamParser.KeyValueContext.class)) {
            return new KeyValueNode((BeamParser.KeyValueContext) context);

        } else if (cc.equals(BeamParser.ImportStmtContext.class)) {
            return new ImportNode((BeamParser.ImportStmtContext) context);

        } else if (cc.equals(BeamParser.ListValueContext.class)) {
            return new ListNode((BeamParser.ListValueContext) context);

        } else if (cc.equals(BeamParser.NumberValueContext.class)) {
            return new NumberNode((BeamParser.NumberValueContext) context);

        } else if (cc.equals(BeamParser.ReferenceBodyContext.class)) {
            BeamParser.ReferenceBodyContext rbc = (BeamParser.ReferenceBodyContext) context;
            String type = rbc.referenceType().getText();

            if (type.contains("::")) {
                Node name = Optional.ofNullable(rbc.referenceName())
                        .map(c -> Node.create(c.getChild(0)))
                        .orElse(null);

                String attribute = Optional.ofNullable(rbc.referenceAttribute())
                        .map(BeamParser.ReferenceAttributeContext::getText)
                        .orElse(null);

                return new ResourceReferenceNode(type, name, attribute);

            } else {
                return new ValueReferenceNode(type);
            }

        } else if (cc.equals(BeamParser.ReferenceValueContext.class)) {
            return create(((BeamParser.ReferenceValueContext) context).referenceBody());

        } else if (cc.equals(BeamParser.ResourceContext.class)) {
            return new ResourceNode((BeamParser.ResourceContext) context);

        } else if (cc.equals(BeamParser.StringExpressionContext.class)) {
            return new StringExpressionNode((BeamParser.StringExpressionContext) context);

        } else if (cc.equals(BeamParser.StringValueContext.class)) {
            BeamParser.StringValueContext svc = (BeamParser.StringValueContext) context;
            BeamParser.StringExpressionContext sec = svc.stringExpression();

            return sec != null
                    ? new StringExpressionNode(sec)
                    : new StringNode(StringUtils.strip(svc.STRING_LITERAL().getText(), "'"));

        } else if (cc.equals(BeamParser.SubresourceContext.class)) {
            BeamParser.SubresourceContext sc = (BeamParser.SubresourceContext) context;
            String type = sc.resourceType().getText();

            if (type.contains("::")) {
                return new UnknownNode(context);

            } else {
                return new KeyListValueNode(type, sc.subresourceBody());
            }

        } else if (TerminalNode.class.isAssignableFrom(cc)) {
            return new StringNode(context.getText());

        } else {
            return new UnknownNode(context);
        }
    }

    public abstract Object evaluate(Scope scope);

    public abstract void buildString(StringBuilder builder, int indentDepth);

    protected void buildNewline(StringBuilder builder, int indentDepth) {
        builder.append('\n');

        for (int i = 0; i < indentDepth; i ++) {
            builder.append("    ");
        }
    }

    protected void buildBody(StringBuilder builder, int indentDepth, List<Node> body) {
        for (Node n : body) {
            buildNewline(builder, indentDepth);
            n.buildString(builder, indentDepth);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildString(builder, 0);
        return builder.toString();
    }
}
