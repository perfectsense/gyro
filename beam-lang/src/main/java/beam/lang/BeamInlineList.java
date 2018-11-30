package beam.lang;

public class BeamInlineList extends BeamList {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BCL.ui().dump("["));
        for (BeamScalar beamScalar : getList()) {
            sb.append(beamScalar);
            sb.append(", ");
        }

        if (!getList().isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("]");
        return sb.toString();
    }
}
