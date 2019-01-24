package beam.docs;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class BeamResourceDoc {

    private String resourceName;

    private DocCommentTree commentTree;

    private String outputDirectory;

    private List<BeamResourceAttributeDoc> attributes = new ArrayList<>();

    public BeamResourceDoc(DocletEnvironment environment, TypeElement resourceElement, String outputDirectory) {
        this.outputDirectory = outputDirectory;

        DocTrees docTrees = environment.getDocTrees();

        DocCommentTree comment = docTrees.getDocCommentTree(resourceElement);
        if (comment != null) {
            resourceName = resourceElement.getSimpleName().toString();
            for (AnnotationMirror am : resourceElement.getAnnotationMirrors()) {
                if (am.getAnnotationType().toString().equals("beam.core.diff.ResourceName")
                        && am.getElementValues().size() > 0) {
                    resourceName = am.getElementValues().values().iterator().next().toString().replace("\"", "");
                }
            }

            commentTree = docTrees.getDocCommentTree(resourceElement);

            for (Element element : resourceElement.getEnclosedElements()) {
                if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) element;
                    BeamResourceAttributeDoc attributeDoc = new BeamResourceAttributeDoc(environment, method);
                    attributes.add(attributeDoc);
                }
            }
        }
    }

    public static String trim(String s) {
        StringBuilder sb = new StringBuilder();

        for (String line : s.split("\n")) {
            if (line.startsWith(" ")) {
                sb.append(line.substring(1));
            } else {
                sb.append(line);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public void generate() {
        if (commentTree == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(resourceName);
        sb.append("\n");
        sb.append(repeat("=", resourceName.length()));
        sb.append("\n\n");
        sb.append(trim(commentTree.toString()));
        sb.append("\n\n");

        sb.append("Attributes\n");
        sb.append(repeat("-", 10));
        sb.append("\n\n");

        for (BeamResourceAttributeDoc attributeDoc : attributes) {
            String attribute = attributeDoc.generate();

            if (attribute.length() > 1) {
                sb.append(attributeDoc.generate());
                sb.append("\n");
            }
        }

        try (FileWriter writer = new FileWriter(outputDirectory + File.separator + resourceName + ".rst")) {
            writer.write(sb.toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String repeat(String c, int r) {
        return new String(new char[r]).replace("\0", c);
    }

}
