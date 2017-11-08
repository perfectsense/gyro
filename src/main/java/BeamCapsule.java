import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class BeamCapsule extends Capsule {

    protected BeamCapsule(Path jarFile) {
        super(jarFile);
    }

    public BeamCapsule(Capsule pred) {
        super(pred);
    }

}
