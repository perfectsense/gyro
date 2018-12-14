package beam.core;

import beam.lang.BeamBlock;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamReference;
import beam.lang.types.BeamList;
import beam.lang.types.BeamScalar;

public class BeamObject extends BeamValidatedBlock {

    private boolean added;

    @Override
    protected boolean resolve(BeamContext parent, BeamContext root) {
        boolean progress = false;
        String id = getParameters().get(0).getValue().toString();
        BeamContextKey key = new BeamContextKey(id, getType());
        if (parent.containsKey(key)) {
            BeamBlock existingConfig = (BeamBlock) parent.get(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.add(key, this);
                progress = true;
            }
        } else {
            parent.add(key, this);
            progress = true;
        }

        if (getScope().isEmpty()) {
            getScope().addAll(parent.getScope());
            getScope().add(key);
        }

        if (!added) {
            BeamContextKey fieldKey = new BeamContextKey(getType());
            if (parent.containsKey(fieldKey) && parent.get(fieldKey) instanceof BeamList) {
                BeamList beamList = (BeamList) parent.get(fieldKey);
                BeamReference reference = new BeamReference();
                reference.getScopeChain().addAll(getScope());
                BeamScalar scalar = new BeamScalar();
                scalar.getElements().add(reference);

                boolean found = false;
                for (BeamScalar beamScalar : beamList.getList()) {
                    if (beamScalar.getElements().size() == 1) {
                        if (reference.equals(beamScalar.getElements().get(0))) {
                            found = true;
                        }
                    }
                }

                if (!found) {
                    beamList.getList().add(scalar);
                }

            } else {
                BeamReference reference = new BeamReference();
                reference.getScopeChain().addAll(getScope());
                parent.add(fieldKey, reference);
            }
            added = true;
        }

        return resolve(root) || progress;
    }

    @Override
    public boolean resolve(BeamContext context) {
        boolean progress = super.resolve(context);
        populate();

        return progress;
    }
}
