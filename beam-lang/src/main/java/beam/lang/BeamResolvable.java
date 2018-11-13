package beam.lang;

public interface BeamResolvable {

    boolean resolve(BeamConfig config);

    Object getValue();
}
