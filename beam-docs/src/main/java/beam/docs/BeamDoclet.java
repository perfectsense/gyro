package beam.docs;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        String providerPackage = "";

        for (ClassDoc doc : root.classes()) {
            if (doc.isAbstract()) {
                continue;
            }

            ResourceDocGenerator generator = new ResourceDocGenerator(root, doc);

            Map<String, String> groupDocs = docs.computeIfAbsent(generator.getGroupName(), m -> new HashMap());
            groupDocs.put(generator.getName(), generator.generate());

            providerPackage = generator.getProviderPackage();
        }

        /*
        AWS Provider
        ------------

        .. toctree::
            :hidden:

           autoscaling-groups/index
           ec2/index
         */

        List<String> groupDirs = new ArrayList<>();
        for (String group : docs.keySet()) {
            if (group != null) {
                String groupDir = group.toLowerCase().replaceAll(" ", "-");

                new File(outputDirectory + File.separator + groupDir).mkdirs();

                // Output individual resource files.
                Map<String, String> resources = docs.get(group);
                for (String resource : resources.keySet()) {
                    String rst = resources.get(resource);

                    try (FileWriter writer = new FileWriter(outputDirectory + File.separator + groupDir + File.separator + resource + ".rst")) {
                        writer.write(rst);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                // Output group index
                try (FileWriter writer = new FileWriter(outputDirectory + File.separator + groupDir + File.separator + "index.rst")) {
                    writer.write(generateGroupIndex(group, resources));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                groupDirs.add(groupDir);
            }
        }

        // Output provider index
        StringBuilder providerIndex = new StringBuilder();
        PackageDoc rootPackageDoc = root.packageNamed(providerPackage);
        providerIndex.append(rootPackageDoc.commentText());
        providerIndex.append("\n\n");
        providerIndex.append(".. toctree::\n");
        providerIndex.append("    :hidden:\n\n");

        Collections.sort(groupDirs);

        for (String groupDir : groupDirs) {
            providerIndex.append("    ");
            providerIndex.append(groupDir);
            providerIndex.append("/index\n");
        }

        try (FileWriter writer = new FileWriter(outputDirectory + File.separator + "index.rst")) {
            writer.write(providerIndex.toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return true;
    }

    public static int optionLength(String option) {
        if (option.equals("-d")) {
            return 2;
        }

        return 0;
    }

    private static String generateGroupIndex(String groupName, Map<String, String> resources) {
        StringBuilder sb = new StringBuilder();

        /*
        Autoscaling Groups
        ==================

        .. toctree::

            auto-scaling-group
            launch-configuration
        */

        sb.append(groupName).append("\n");
        sb.append(ResourceDocGenerator.repeat("=", groupName.length()));
        sb.append("\n\n");
        sb.append(".. toctree::");
        sb.append("\n");
        sb.append("    :hidden:");
        sb.append("\n\n");

        List<String> keys = new ArrayList<>(resources.keySet());
        Collections.sort(keys);

        for (String resource : keys) {
            if (resource != null) {
                sb.append("    ").append(resource);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

}
