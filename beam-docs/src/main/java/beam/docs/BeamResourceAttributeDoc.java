package beam.docs;

import com.google.common.base.CaseFormat;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.ExecutableElement;

public class BeamResourceAttributeDoc {

    private String attributeName;

    private DocCommentTree commentTree;

    public BeamResourceAttributeDoc(DocletEnvironment environment, ExecutableElement method) {
        DocTrees docTrees = environment.getDocTrees();

        commentTree = docTrees.getDocCommentTree(method);
        if (commentTree != null) {
            attributeName = method.getSimpleName().toString();
            attributeName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, attributeName).replaceFirst("get-", "");
        }
    }

    public String generate() {
        if (commentTree == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(attributeName);
        sb.append("\n         ");
        sb.append(commentTree);
        sb.append("\n");

        return sb.toString();
    }

}
