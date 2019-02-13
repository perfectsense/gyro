package beam.docs;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

        // group -> "resource -> rst"
        Map<String, Map<String, String>> docs = new HashMap<>();

        for (ClassDoc doc : root.classes()) {
            ResourceDocGenerator generator = new ResourceDocGenerator(root, doc);

            Map<String, String> groupDocs = docs.computeIfAbsent(generator.getGroupName(), m -> new HashMap());
            groupDocs.put(generator.getName(), generator.generate());

        }

        for (String group : docs.keySet()) {
            if (group != null) {
                String groupDir = group.toLowerCase().replaceAll(" ", "-");

                new File(outputDirectory + File.separator + groupDir).mkdirs();

                Map<String, String> resources = docs.get(group);
                for (String resource : resources.keySet()) {
                    String rst = resources.get(resource);

                    try (FileWriter writer = new FileWriter(outputDirectory + File.separator + groupDir + File.separator + resource + ".rst")) {
                        writer.write(rst);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
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
