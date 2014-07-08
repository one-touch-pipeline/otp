package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * This transformation generates the version method by looking at the git
 * history of the source file for the Job implementation. The latest git commit becomes
 * the unique version number of the Job.
 *
 * If the Job Implementation class inherits the AbstractJobImpl class the transformation
 * also adds the required constructors.
 *
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class JobTransformation extends AbstractJobTransformation implements ASTTransformation {

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        List classNodes = sourceUnit.getAST()?.getClasses()
        
        classNodes.each { ClassNode classNode ->
            if (classNode.isInterface()) {
                // skip interfaces
                return
            }
            if ((classNode.modifiers & Opcodes.ACC_ABSTRACT) > 0) {
                // skip abstract classes
                return
            }
            // test whether the class implements the Job interface
            boolean isJob = false
            boolean inheritsAbstractJob = false
            classNode.getAllInterfaces().each {
                if (it.name == "de.dkfz.tbi.otp.job.processing.Job" || it.name == "de.dkfz.tbi.otp.job.processing.EndStateAwareJob") {
                    isJob = true
                }
            }
            if (classNode.superClass.name == "de.dkfz.tbi.otp.job.processing.AbstractJobImpl" ||
                    classNode.superClass.name == "de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl" ||
                    classNode.superClass.name == "de.dkfz.tbi.otp.job.processing.AbstractValidatingJobImpl") {
                isJob = true
                inheritsAbstractJob = true
            }
            if (!isJob) {
                return
            }
            if (inheritsAbstractJob) {
                println "Applying JobTransformations to ${classNode}"  // log.debug instead of println does not produce any output on stdout
                // add generic Constructor
                classNode.addConstructor(new ConstructorNode(Opcodes.ACC_PUBLIC, new BlockStatement()))
                // add constructor with two arguments calling the super class constructor
                Parameter[] params = [
                    new Parameter(classNode.superClass.getField("processingStep").type, "a"),
                    new Parameter(new ClassNode(Collection), "b") ] as Parameter[]
                BlockStatement stmt = new BlockStatement()
                stmt.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.SUPER, new ArgumentListExpression(new VariableExpression("a"), new VariableExpression("b")))))
                classNode.addConstructor(Opcodes.ACC_PUBLIC, params, ClassNode.EMPTY_ARRAY, stmt)
                // add getVersion method - actual code will be generated below
                classNode.addMethod("getVersion", Opcodes.ACC_PUBLIC, new ClassNode(String), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, new ReturnStatement(new ConstantExpression("")))
            }
            addLog(classNode)
            // add annotation nodes for scope
            addScopeAnnotation(classNode)
            addComponentAnnotation(classNode)

            // add the Annotation to the execute method and generates the version
            classNode.getMethods().each { method ->
                createGetVersion(method)
            }
        }
    }

}
