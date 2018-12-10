package beam.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class BeamReference extends BeamLiteral {

    private List<BeamContextKey> scopeChain;

    private List<String> referenceChain;

    private Object value;

    public BeamReference() {
    }

    public BeamReference(List<BeamContextKey> scopeChain, String chain) {
        this.scopeChain = scopeChain;
        this.referenceChain = parseReferenceChain(chain);
    }

    public List<BeamContextKey> getScopeChain() {
        if (scopeChain == null) {
            scopeChain = new ArrayList<>();
        }

        return scopeChain;
    }

    public void setScopeChain(List<BeamContextKey> scopeChain) {
        this.scopeChain = scopeChain;
    }

    public List<String> getReferenceChain() {
        if (referenceChain == null) {
            referenceChain = new ArrayList<>();
        }

        return referenceChain;
    }

    public void setReferenceChain(List<String> referenceChain) {
        this.referenceChain = referenceChain;
    }

    public List<String> parseReferenceChain(String chain) {
        if (chain == null) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(chain.split("\\."));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('$');
        sb.append('(');
        for (BeamContextKey scope : getScopeChain()) {
            sb.append(scope);
            sb.append(" | ");
        }
        if (!getReferenceChain().isEmpty()) {
            sb.append(String.join(".", getReferenceChain()));
        } else {
            sb.setLength(sb.length() - 3);
        }

        sb.append(')');
        return sb.toString();
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean resolve(BeamContext context) {
        BeamReferable referable = null;
        for (BeamContextKey scope : getScopeChain()) {
            if (context.hasKey(scope)) {
                referable = context.getReferable(scope);
                if (referable instanceof BeamContext) {
                    context = (BeamContext) referable;
                } else {
                    throw new BeamLangException(String.format("Unable to resolve %s, expecting %s to be an BeamContext found %s", this, scope, referable.getClass()));
                }
            }
        }

        for (String key : getReferenceChain()) {
            if (referable == null) {
                BeamContextKey contextKey = new BeamContextKey(key);
                referable = context.getReferable(contextKey);
            } else {
                if (referable instanceof BeamCollection) {
                    referable = ((BeamCollection) referable).get(key);
                } else {
                    throw new BeamLangException(String.format("Illegal reference %s, %s is not a collection", this, referable));
                }
            }

            if (referable == null || referable.getValue() == null) {
                return false;
            }
        }

        if (referable == null) {
            return false;
        }

        if (value != null && value.getClass() == referable.getValue().getClass()) {
            return false;
        } else {
            value = referable.getValue();
            return true;
        }
    }

    @Override
    public Set<BeamReference> getDependencies(BeamConfig config) {
        Set<BeamReference> dependencies = new HashSet<>();
        if (getValue() != null) {
            return dependencies;
        }

        BeamResolvable resolvable = config;
        for (BeamContextKey scope : getScopeChain()) {
            if (config.hasKey(scope)) {
                resolvable = config.getReferable(scope);
                if (resolvable instanceof BeamConfig) {
                    config = (BeamConfig) resolvable;
                } else {
                    throw new BeamLangException(String.format("Unable to resolve %s, expecting %s as BeamConfig found %s", this, scope, resolvable.getClass()));
                }
            }
        }

        if (resolvable == null) {
            throw new BeamLangException("Unable to find BeamConfig");
        }

        BeamReference reference = new BeamReference();
        reference.setScopeChain(getScopeChain());
        reference.value = resolvable;

        BeamResolvable nextResolvable = resolvable;
        for (String key : getReferenceChain()) {
            if (nextResolvable instanceof BeamCollection) {
                nextResolvable = ((BeamCollection) nextResolvable).get(key);
            } else {
                throw new BeamLangException(String.format("Illegal reference %s, %s is not a collection", this, nextResolvable));
            }

            if (nextResolvable == null || nextResolvable.getValue() == null) {
                dependencies.add(reference);
            }
        }

        return dependencies;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        BeamReference otherReference = (BeamReference) other;
        if (getScopeChain().size() != otherReference.getScopeChain().size()) {
            return false;
        }

        for (int i = 0; i < getScopeChain().size(); i ++) {
            if (!getScopeChain().get(i).equals(otherReference.getScopeChain().get(i))) {
                return false;
            }
        }

        if (getReferenceChain().size() != otherReference.getReferenceChain().size()) {
            return false;
        }

        for (int i = 0; i < getReferenceChain().size(); i ++) {
            if (!getReferenceChain().get(i).equals(otherReference.getReferenceChain().get(i))) {
                return false;
            }
        }

        return true;
    }
}
