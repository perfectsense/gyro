package beam.docs;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class BeamResourceDoc {

    private String resourceName;

    private DocCommentTree commentTree;

    private List<BeamResourceAttributeDoc> attributes = new ArrayList<>();

    public BeamResourceDoc(DocletEnvironment environment, TypeElement resourceElement) {
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

    public String generate() {
        if (commentTree == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(resourceName);
        sb.append("\n\n");
        sb.append(commentTree);
        sb.append("\n\n");

        for (BeamResourceAttributeDoc attributeDoc : attributes) {
            String attribute = attributeDoc.generate();

            if (attribute.length() > 1) {
                sb.append(attributeDoc.generate());
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
