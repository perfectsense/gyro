package beam.docs;

import jdk.javadoc.doclet.DocletEnvironment;

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
                    BeamResourceDoc doc = new BeamResourceDoc(environment, classElement);
                    System.out.println(doc.generate());
                }
            }
        }

        return true;
    }

}
