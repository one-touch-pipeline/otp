import de.dkfz.tbi.otp.dataprocessing.*
import groovy.transform.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.transform.trait.*
import org.codenarc.rule.*

import java.lang.reflect.*

@CompileStatic
class EnumForBeanNameRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*"
    int priority = 1
    String name = 'EnumForBeanName'
    String description = 'Ensures, that for a given Interface there exists a Enum that represents all the beanName names for all the Classes that inherit from this interface '
    Class astVisitorClass = EnumForBeanNameVisitor
}

@CompileStatic
class EnumForBeanNameVisitor extends AbstractAstVisitor {

    // [InterfaceName: AllAvailableBeanNames]
    Map<String, List<String>> properties = [
            'SampleIdentifierParser': SampleIdentifierParserBeanName.values()*.beanName as List,
            'AlignmentDecider': AlignmentDeciderBeanName.values()*.beanName as List,
    ]

    @Override
    void visitClassComplete(ClassNode node) {
        if (Modifier.isAbstract(node.modifiers) || Modifier.isInterface(node.modifiers) || isEnum(node.modifiers) || Traits.isTrait(node)) {
            return
        }

        node.interfaces.each { ClassNode interfaceNode ->
            properties.each { key, value ->
                if (interfaceNode.name == key) {
                    String beanName = uncapitalize(node.nameWithoutPackage)
                    node.annotations.each { AnnotationNode annotationNode ->
                        if (annotationNode.classNode.text == 'Component') {
                            String annotationValue = annotationNode.members['value']?.text
                            if (annotationValue) {
                                beanName = annotationValue
                            }
                        }
                    }

                    if (beanName in value) {
                        return
                    }
                    addViolation(node, "BeanName '${beanName}' for class '${node.nameWithoutPackage} has to be added to '${key}BeanName'.")
                }
            }
        }
    }

    //This will be implemented in Groovy 2.4.8
    private static String uncapitalize(CharSequence self) {
        String s = self.toString()
        return s != null && s.length() != 0 ? "${Character.toLowerCase(s.charAt(0))}${s.substring(1)}" : s
    }

    private static boolean isEnum(int mod) {
        return (mod & 16384) != 0
    }
}
