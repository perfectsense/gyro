package beam.docs;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ObjectUtils;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

public class ResourceDocGenerator {

    private RootDoc root;
    private ClassDoc doc;
    private String namespace;
    private String name;
    private String groupName;
    private String providerPackage;

    public ResourceDocGenerator(RootDoc root, ClassDoc doc) {
        this.root = root;
        this.doc = doc;

        PackageDoc packageDoc = doc.containingPackage();
        for (AnnotationDesc annotationDesc : packageDoc.annotations()) {
            if (annotationDesc.annotationType().name().equals("DocGroup")) {
                groupName = (String) annotationDesc.elementValues()[0].value().value();
            }
        }

        for (AnnotationDesc annotationDesc : doc.annotations()) {
            if (annotationDesc.annotationType().name().equals("ResourceName")) {
                name = (String) annotationDesc.elementValues()[0].value().value();
            }
        }

        if (name == null) {
            name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, doc.name().replace("Resource", ""));
        }

        providerPackage = packageDoc.name().substring(0, packageDoc.name().lastIndexOf('.'));
        PackageDoc rootPackageDoc = root.packageNamed(providerPackage);
        for (AnnotationDesc annotationDesc : rootPackageDoc.annotations()) {
            if (annotationDesc.annotationType().name().equals("DocNamespace")) {
                namespace = (String) annotationDesc.elementValues()[0].value().value();
            }
        }
    }

    public String generate() {
        StringBuilder sb = new StringBuilder();

        generateHeader(sb);
        generateAttributes(doc, sb);

        return sb.toString();
    }

    public String getProviderPackage() {
        return providerPackage;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return groupName;
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

    public static String repeat(String c, int r) {
        return new String(new char[r]).replace("\0", c);
    }

    private String resourceName() {
        return String.format("%s::%s", namespace, name);
    }

    private void generateHeader(StringBuilder sb) {

        sb.append(resourceName());
        sb.append("\n");
        sb.append(repeat("=", resourceName().length()));
        sb.append("\n\n");
        sb.append(trim(doc.commentText()));
        sb.append("\n\n");
        sb.append("Attributes\n");
        sb.append(repeat("-", 10));
        sb.append("\n\n");
    }

    private void generateAttributes(ClassDoc classDoc, StringBuilder sb) {
        // `auto-scaling-group-name <#auto-scaling-group-name>`_ - The name of the auto scaling group, also served as its identifier and thus unique. (Required)

        if (classDoc.superclass() != null && !classDoc.superclass().name().equals("Resource")) {
            generateAttributes(classDoc.superclass(), sb);
        }

        for (MethodDoc methodDoc : classDoc.methods()) {
            if (!ObjectUtils.isBlank(methodDoc.commentText())) {
                String attributeName = methodDoc.name();
                attributeName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, attributeName).replaceFirst("get-", "");

                sb.append(String.format("`%s <#%s>`_", attributeName, attributeName));
                sb.append(" - ");
                sb.append(methodDoc.commentText());
                sb.append("\n\n");
            }
        }
    }

}
