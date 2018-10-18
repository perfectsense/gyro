package beam.core;

public interface BeamReferable {

    boolean resolve(BeamContext context);

    Object getValue();
}
