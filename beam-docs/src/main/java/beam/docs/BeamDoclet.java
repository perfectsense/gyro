package beam.docs;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BeamDoclet extends Doclet {

    public static boolean start(RootDoc root) {
        // Generate rst file for each resource.
        // Generate index for each group (i.e. java package) of resources.
        // Generate index for all groups.

        String outputDirectory = ".";
        for (int i = 0; i < root.options().length; i++) {
            String[] optionArray = root.options()[i];
            String option = optionArray[0];

            if (option.equals("-d")) {
                outputDirectory = optionArray[1];
            }

        }

        for (ClassDoc doc : root.classes()) {
            ResourceDocGenerator generator = new ResourceDocGenerator(root, doc);

            try (FileWriter writer = new FileWriter(outputDirectory + File.separator + generator.name() + ".rst")) {
                writer.write(generator.generate());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return true;
    }

    public static int optionLength(String option) {
        if (option.equals("-d")) {
            return 2;
        }

        return 0;
    }

}
