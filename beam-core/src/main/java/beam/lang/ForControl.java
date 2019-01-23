package beam.lang;

import beam.core.BeamCore;
import beam.lang.types.ReferenceValue;
import beam.lang.types.Value;
import beam.parser.antlr4.BeamParser.ForStmtContext;

import java.util.ArrayList;
import java.util.List;

public class ForControl extends Control {

    private List<String> variables;
    private List<Value> listValues;
    private List<Frame> frames;
    private ReferenceValue listReference;

    private ForStmtContext context;
    private BeamVisitor visitor;
    private BeamCore core;
    private boolean evaluated;

    public ForControl(BeamCore core, BeamVisitor visitor, ForStmtContext context) {
        this.core = core;
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

    public ReferenceValue listReference() {
        return listReference;
    }

    public void listReference(ReferenceValue listReference) {
        listReference.parent(this);
        this.listReference = listReference;
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
    }

}

