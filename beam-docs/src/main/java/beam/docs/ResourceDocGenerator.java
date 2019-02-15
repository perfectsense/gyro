package beam.docs;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ObjectUtils;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

import java.util.ArrayList;
import java.util.List;

public class ResourceDocGenerator {

    private RootDoc root;
    private ClassDoc doc;
    private String namespace;
    private String name;
    private String groupName;
    private String providerPackage;
    private List<String> subresources = new ArrayList<>();
    private boolean isSubresource = false;

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
                for (AnnotationDesc.ElementValuePair pair : annotationDesc.elementValues()) {
                    if (pair.element().name().equals("value")) {
                        name = (String) pair.value().value();
                        break;
                    } else if (pair.element().name().equals("parent") && pair.value().value() != null) {
                        isSubresource = true;
                    }
                }
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

        if (doc.superclass() != null && doc.superclass().name().equals("Diffable")) {
            isSubresource = true;
        }
    }

    public String generate() {
        if (isSubresource) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        System.out.println("Generating documentation for: " + resourceName());

        generateHeader(sb);

        sb.append("Attributes\n");
        sb.append(repeat("-", 10));
        sb.append("\n\n");

        generateAttributes(doc, sb, 0);

        sb.append("Outputs\n");
        sb.append(repeat("-", 7));
        sb.append("\n\n");

        generateOutputs(doc, sb, 0);

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
    }

    private void generateAttributes(ClassDoc classDoc, StringBuilder sb, int indent) {

        // Output superclass attributes.
        if (classDoc.superclass() != null && !classDoc.superclass().name().equals("Resource")) {
            generateAttributes(classDoc.superclass(), sb, indent);
        }

        for (MethodDoc methodDoc : classDoc.methods()) {
            if (!ObjectUtils.isBlank(methodDoc.commentText())) {
                String attributeName = methodDoc.name();
                attributeName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, attributeName).replaceFirst("get-", "");

                boolean isSubresource = false;
                boolean isOutput = false;
                for (Tag tag : methodDoc.tags()) {
                    if (tag.name().equals("@subresource"))  {
                        sb.append(repeat(" ", indent));
                        sb.append(String.format("**%s** is a subresource with the following attributes:", attributeName));
                        sb.append("\n\n");
                        sb.append(repeat(" ", indent + 4));
                        sb.append("*");
                        sb.append(firstSentence(methodDoc.commentText()));
                        sb.append("*");
                        sb.append("\n\n");

                        ClassDoc subresourceDoc = root.classNamed(tag.text());
                        if (subresourceDoc != null) {
                            generateAttributes(subresourceDoc, sb, indent + 4);
                        }

                        isSubresource = true;
                    } else if (tag.name().equals("@output")) {
                        isOutput = true;
                    }
                }

                if (!isSubresource && !isOutput) {
                    writeAttribute(sb, methodDoc, indent);
                }
            }
        }
    }

    private void generateOutputs(ClassDoc classDoc, StringBuilder sb, int indent) {
        // Output superclass attributes.
        if (classDoc.superclass() != null && !classDoc.superclass().name().equals("Resource")) {
            generateAttributes(classDoc.superclass(), sb, indent);
        }

        for (MethodDoc methodDoc : classDoc.methods()) {
            if (!ObjectUtils.isBlank(methodDoc.commentText())) {
                boolean isOutput = false;
                for (Tag tag : methodDoc.tags()) {
                    if (tag.name().equals("@output")) {
                        isOutput = true;
                    }
                }

                if (isOutput) {
                    writeAttribute(sb, methodDoc, indent);
                }
            }
        }
    }

    private void writeAttribute(StringBuilder sb, MethodDoc methodDoc, int indent) {
        String attributeName = methodDoc.name();
        attributeName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, attributeName).replaceFirst("get-", "");

        sb.append(repeat(" ", indent));
        sb.append(String.format("`%s <#%s>`_", attributeName, attributeName));
        sb.append(" - ");
        sb.append(firstSentence(methodDoc.commentText()));

        String rest = comment(methodDoc.commentText(), indent);
        if (!ObjectUtils.isBlank(rest)) {
            sb.append(rest);
        }

        sb.append("\n\n");
    }

    private String firstSentence(String commentText) {
        return commentText.split("\n")[0];
    }

    private String comment(String commentText, int indent) {
        StringBuilder sb = new StringBuilder();

        String[] parts = commentText.split("\n");
        if (parts.length > 1) {
            sb.append("\n");
            for (int i = 1; i < parts.length; i++) {
                sb.append(repeat(" ", indent));
                sb.append(parts[i]);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

}
