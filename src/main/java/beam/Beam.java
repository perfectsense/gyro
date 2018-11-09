package beam;

import beam.lang.BCL;
import beam.lang.BeamConfig;

public class Beam {

    public static void main(String[] arguments) throws Exception {
        try {
            BCL.init();
            BeamConfig root = BCL.parse("example.bcl");
            BCL.resolve(root);

            System.out.println(root);
        } finally {
            BCL.shutdown();
        }
    }
}
