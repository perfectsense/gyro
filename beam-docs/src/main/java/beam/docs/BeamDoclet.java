package beam.docs;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class BeamDoclet implements Doclet {

    private String outputDirectory;

    @Override
    public void init(Locale locale, Reporter reporter) {

    }

    @Override
    public String getName() {
        return "Beam Reference Documentation Doclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        Doclet.Option[] options = {
                new BeamDocletOption("-d", 1, "output directory", "path") {
                    @Override
                    public boolean process(String opt, List<String> args) {
                        outputDirectory = args.get(0);
                        return true;
                    }
                }
        };

        Set<BeamDoclet.Option> oset = new HashSet<>();
        oset.addAll(Arrays.asList(options));

        return oset;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

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
