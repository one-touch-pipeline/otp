/*
 * Copyright 2011-2026 The OTP authors
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

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.Phases
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

class AutowiredCollectionServiceFieldsRule extends AbstractAstVisitorRule {
    int priority = 1
    String name = 'AutowiredCollectionServiceFields'
    String description = 'Collection fields that reference services (List, Set, Collection of services) must be explicitly annotated with @Autowired'
    Class astVisitorClass = AutowiredCollectionServiceFieldsVisitor
    int compilerPhase = Phases.SEMANTIC_ANALYSIS
}

class AutowiredCollectionServiceFieldsVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    private static final String AUTOWIRED_ANNOTATION = 'Autowired'
    private static final String COMPONENT_ANNOTATION = 'Component'
    private static final String SERVICE_ANNOTATION = 'Service'

    @Override
    protected void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node)) {
            return
        }

        // Check if this is a service class (ends with "Service") or annotated with @Component or @Service
        boolean isServiceClass = node.name.endsWith('Service')
        boolean isComponentClass = hasAnnotation(node, COMPONENT_ANNOTATION)
        boolean isServiceAnnotatedClass = hasAnnotation(node, SERVICE_ANNOTATION)

        if (isServiceClass || isComponentClass || isServiceAnnotatedClass) {
            node.fields.each { fieldNode ->
                checkCollectionServiceField(fieldNode)
            }
        }
    }

    private void checkCollectionServiceField(FieldNode fieldNode) {
        // Skip static, final, or private fields that might be constants
        if (fieldNode.static || fieldNode.final || fieldNode.private) {
            return
        }

        // Check if field is a collection type (implements Collection interface)
        if (isCollectionType(fieldNode.type)) {
            // Check if the generic type is a service
            if (isServiceCollectionField(fieldNode)) {
                // Check if @Autowired annotation is present
                if (!hasAnnotation(fieldNode, AUTOWIRED_ANNOTATION)) {
                    String message = "Field '${fieldNode.name}' is a collection of services and must be explicitly annotated with @Autowired"
                    addViolation(fieldNode, message)
                }
            }
        }
    }

    private boolean isServiceCollectionField(FieldNode fieldNode) {
        // Get the generic types of the collection
        GenericsType[] genericsTypes = fieldNode.type.genericsTypes
        if (!genericsTypes || genericsTypes.length == 0) {
            return false
        }

        // Check if any generic type appears to be a service
        return genericsTypes.any { genericType ->
            String genericTypeName = genericType.type.nameWithoutPackage
            ClassNode genericClassNode = genericType.type

            // Check if it's a service by naming convention OR annotated with @Component or @Service
            return genericTypeName.endsWith('Service') ||
                   hasAnnotation(genericClassNode, COMPONENT_ANNOTATION) ||
                   hasAnnotation(genericClassNode, SERVICE_ANNOTATION)
        }
    }

    private boolean isCollectionType(ClassNode type) {
        // Check if the type implements java.util.Collection interface
        // This covers all collection types: List, Set, Queue, Deque, Bag, etc.

        // Direct name checking for Collection interface
        if (checkCollectionClassName(type)) {
            return true
        }

        // Check if type implements Collection interface
        if (type.allInterfaces?.any { interfaceNode ->
            checkCollectionClassName(interfaceNode)
        }) {
            return true
        }

        // Check if type extends a class that implements Collection
        ClassNode superClass = type.superClass
        while (superClass && superClass.name != 'java.lang.Object') {
            if (checkCollectionClassName(superClass) ||
                superClass.allInterfaces?.any { checkCollectionClassName(it) }) {
                return true
            }
            superClass = superClass.superClass
        }

        // Fallback: check common collection type names for cases where AST might not have full type info
        return type.nameWithoutPackage in ['Collection', 'List', 'Set', 'Queue', 'Deque', 'ArrayList', 'LinkedList',
                                          'HashSet', 'TreeSet', 'LinkedHashSet', 'Vector', 'Stack', 'PriorityQueue',
                                          'ArrayDeque', 'ConcurrentLinkedQueue', 'Bag', 'MultiSet']
    }

    private boolean checkCollectionClassName(ClassNode node) {
        // Check if the node is java.util.Collection interface
        return node.name == 'java.util.Collection' ||
                node.nameWithoutPackage == 'Collection'
    }

    private boolean hasAnnotation(AnnotatedNode node, String annotationName) {
        return node.annotations?.any { annotation ->
            annotation.classNode.nameWithoutPackage == annotationName
        }
    }

}
