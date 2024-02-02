/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.transform.trait.Traits
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName

import java.lang.reflect.Modifier

class EnumForBeanNameRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*"
    int priority = 1
    String name = 'EnumForBeanName'
    String description = 'Ensures, that for a given Interface there exists a Enum that represents all the beanName names ' +
            'for all the Classes that inherit from this interface'
    Class astVisitorClass = EnumForBeanNameVisitor
}

class EnumForBeanNameVisitor extends AbstractAstVisitor {

    // [InterfaceName: AllAvailableBeanNames]
    Map<String, List<String>> properties = [
            'SampleIdentifierParser': SampleIdentifierParserBeanName.values()*.beanName as List,
            'DirectoryStructure'    : DirectoryStructureBeanName.values()*.beanName as List,
    ]

    @Override
    void visitClassComplete(ClassNode node) {
        if (Modifier.isAbstract(node.modifiers) || Modifier.isInterface(node.modifiers) || isEnum(node.modifiers) || Traits.isTrait(node)) {
            return
        }

        node.interfaces.each { ClassNode interfaceNode ->
            properties.each { key, value ->
                if (interfaceNode.name == key) {
                    String beanName = node.nameWithoutPackage.uncapitalize()
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

    private static boolean isEnum(int mod) {
        return (mod & 16384) != 0
    }
}
