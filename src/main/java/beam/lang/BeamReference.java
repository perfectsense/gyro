package beam.lang;

import beam.core.BeamException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeamReference extends BeamLiteral {

    private List<BeamConfigKey> scopeChain;

    private List<String> referenceChain;

    private Object value;

    public BeamReference() {
    }

    public BeamReference(List<BeamConfigKey> scopeChain, String chain) {
        this.scopeChain = scopeChain;
        this.referenceChain = parseReferenceChain(chain);
    }

    public List<BeamConfigKey> getScopeChain() {
        if (scopeChain == null) {
            scopeChain = new ArrayList<>();
        }

        return scopeChain;
    }

    public void setScopeChain(List<BeamConfigKey> scopeChain) {
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
        if (value != null) {
            return value.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append('$');
        sb.append('(');
        for (BeamConfigKey scope : getScopeChain()) {
            sb.append(scope);
            sb.append(" | ");
        }

        sb.append(String.join(".", getReferenceChain()));
        sb.append(')');
        return sb.toString();
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean resolve(BeamConfig config) {
        if (value != null) {
            return false;
        }

        BeamResolvable resolvable = config;
        for (BeamConfigKey scope : getScopeChain()) {
            if (config.getContext().containsKey(scope)) {
                resolvable = config.getContext().get(scope);
                if (resolvable instanceof BeamConfig) {
                    config = (BeamConfig) resolvable;
                } else {
                    throw new BeamException(String.format("Unable to resolve %s, expecting %s as BeamConfig found %s", this, scope, resolvable.getClass()));
                }
            }
        }

        if (resolvable == null) {
            return false;
        }

        BeamResolvable nextResolvable = resolvable;
        for (String key : getReferenceChain()) {
            if (nextResolvable instanceof BeamCollection) {
                nextResolvable = ((BeamCollection) nextResolvable).get(key);
            } else {
                throw new BeamException(String.format("Illegal reference %s, %s is not a collection", this, nextResolvable));
            }

            if (nextResolvable.getValue() == null) {
                return false;
            }
        }

        value = nextResolvable.getValue();
        return true;
    }
}
