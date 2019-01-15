package beam.lang;

import beam.lang.types.Value;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.ForStmtContext;

import java.util.ArrayList;
import java.util.List;

public class ForControl extends Control {

    private List<String> variables;
    private List<Value> listValues;
    private List<Frame> frames;

    private ForStmtContext context;
    private BeamVisitor visitor;
    private boolean evaluated;

    public ForControl(BeamVisitor visitor, ForStmtContext  context) {
        this.visitor = visitor;
        this.context = context;
    }

    public List<String> variables() {
        if (variables == null) {
            variables = new ArrayList<>();
        }

        return variables;
    }

    public void variables(List<String> variables) {
        this.variables = variables;
    }

    public List<Value> listValues() {
        if (listValues == null) {
            listValues = new ArrayList<>();
        }

        return listValues;
    }

    public void listValues(List<Value> listValues) {
        this.listValues = listValues;
    }

    public List<Frame> frames() {
        if (frames == null) {
            frames = new ArrayList<>();
        }

        return frames;
    }

    public void frames(List<Frame> frames) {
        this.frames = frames;
    }

    public boolean evaluated() {
        return evaluated;
    }

    public void evaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    @Override
    public boolean resolve() {
        for (Value value : listValues()) {
            if (!value.resolve()) {
                throw new BeamLanguageException("Unable to resolve configuration.", value);
            }
        }

        evaluate();

        return true;
    }

    public void evaluate() {
        if (evaluated()) {
            return;
        }

        // Validate there are enough values to evenly loop over the list.
        if (listValues().size() % variables().size() != 0) {
            throw new BeamLanguageException("Not enough values to loop", this);
        }

        int loops = listValues().size() / variables().size();

        Container parent = (Container) parent();
        for (int i = 0; i < loops; i++) {
            Frame frame = new Frame();
            frames().add(frame);

            frame.parent(parent);
            parent.frames().add(frame);

            for (int j = 0; j < variables().size(); j++) {
                int index = (i * variables().size()) + j;

                String variableName = variables().get(index % variables.size());
                Value value = listValues().get(index);

                frame.put(variableName, value);
            }

            for (BeamParser.ControlStmtsContext stmtContext : context.controlBody().controlStmts()) {
                if (stmtContext.keyValue() != null) {
                    String key = visitor.parseKey(stmtContext.keyValue().key());
                    Value value = visitor.parseValue(stmtContext.keyValue().value());

                    frame.put(key, value);
                } else if (stmtContext.forStmt() != null) {
                    ForControl forControl = visitor.visitForStmt(stmtContext.forStmt(), frame);
                    frame.putControl(forControl);
                } else if (stmtContext.ifStmt() != null) {
                    IfControl ifControl = visitor.visitIfStmt(stmtContext.ifStmt(), frame);
                    frame.putControl(ifControl);
                } else if (stmtContext.resource() != null) {
                    Resource resource = visitor.visitResource(stmtContext.resource(), frame);
                    frame.putResource(resource);
                } else if (stmtContext.subresource() != null) {
                    Resource resource = visitor.visitSubresource(stmtContext.subresource(), (Resource) parent);
                    frame.putSubresource(resource);
                }
            }
        }

        evaluated(true);
    }

}

