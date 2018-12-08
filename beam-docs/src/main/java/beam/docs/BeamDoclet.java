package beam.docs;

import com.google.common.base.CaseFormat;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public class BeamDoclet extends jdk.javadoc.doclet.StandardDoclet {

    @Override
    public boolean run(DocletEnvironment environment) {
        TypeMirror beamResource = null;
        for (TypeElement classElement : environment.getElementUtils().getAllTypeElements("beam.core.BeamResource")) {
            if ("beam.core.BeamResource".equals(classElement.getQualifiedName().toString())) {
                beamResource = classElement.asType();
            }
        }

        for (PackageElement packageElement : ElementFilter.packagesIn(environment.getIncludedElements())) {
            for (TypeElement classElement : ElementFilter.typesIn(packageElement.getEnclosedElements())) {
                if (environment.getTypeUtils().isAssignable(classElement.asType(), beamResource)) {
                    generateReferenceDoc(environment, classElement);
                }
            }
        }

        return true;
    }

    public void generateReferenceDoc(DocletEnvironment environment, TypeElement resourceElement) {
        DocTrees docTrees = environment.getDocTrees();

        DocCommentTree comment = docTrees.getDocCommentTree(resourceElement);
        if (comment != null) {
            System.out.println(resourceElement.getSimpleName());
            System.out.println("");
            System.out.println(docTrees.getDocCommentTree(resourceElement));
            System.out.println("");

            for (Element fieldElement : resourceElement.getEnclosedElements()) {
                if (fieldElement.getKind() == ElementKind.FIELD) {
                    DocCommentTree fieldComment = docTrees.getDocCommentTree(fieldElement);
                    if (fieldComment != null) {
                        String fieldName = fieldElement.getSimpleName().toString();
                        fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, fieldName);
                        System.out.println(fieldName + " - " + fieldComment);
                    }
                }
            }
        }
    }

}
