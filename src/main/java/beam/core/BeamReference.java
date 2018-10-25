package beam.core;

import beam.core.diff.DiffUtil;
import com.google.common.base.CaseFormat;

import java.util.*;

/**
 * BeamReference to an AWS resource.
 */
public class BeamReference extends BeamLiteral {

    private BeamContextKey key;

    private List<String> referenceChain;

    private boolean resolved;

    private Object value;

    public BeamReference(BeamContextKey key, List<String> referenceChain) {
        this.key = key;
        this.referenceChain = referenceChain;
    }

    public BeamReference(BeamContextKey key, String chain) {
        this.key = key;
        this.referenceChain = parseReferenceChain(chain);
    }

    private List<String> parseReferenceChain(String chain) {
        if (chain == null) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(chain.split("\\."));
        }
    }

    public BeamContextKey getKey() {
        return key;
    }

    public void setKey(BeamContextKey key) {
        this.key = key;
    }

    public List<String> getReferenceChain() {
        if (referenceChain == null) {
            referenceChain = new ArrayList<>();
        }

        return referenceChain;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        if (!referenceChain.isEmpty()) {
            sb.append(" | ");
            sb.append(String.join(".", referenceChain));
        }

        return sb.toString();
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean resolve(BeamContext context) {
        if (resolved) {
            return false;
        }

        BeamReferable referable = null;
        for (BeamContextKey contextKey : context.getContext().keySet()) {
            if (getKey().equals(contextKey)) {
                referable = context.getContext().get(contextKey);
            }
        }

        if (referable == null) {
            throw new BeamException(String.format("Unable to resolve %s", getKey()));
        }

        boolean progress = false;
        if (referable.getValue() != null) {
            Object resolvedValue = referable.getValue();
            try {
                for (String getter : getReferenceChain()) {
                    if (resolvedValue != null) {
                        getter = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, getter);
                        resolvedValue = DiffUtil.getPropertyValue(resolvedValue, null, getter);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (resolvedValue != null) {
                value = resolvedValue;
                resolved = true;
                progress = true;
            }
        }

        return progress;
    }
}
