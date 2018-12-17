package beam.lang.types;

import org.apache.commons.lang.StringUtils;

public class BeamString extends BeamLiteral {

    public BeamString(String literal) {
        super(StringUtils.strip("'", literal));
    }

}
