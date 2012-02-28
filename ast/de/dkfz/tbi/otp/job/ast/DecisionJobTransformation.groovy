package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.AnnotationNode
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
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST Transformation for DecisionJob.
 *
 * Same as JobTransformation, just for a different target.
 *
 * @see JobExecution
 * @see JobTransformation
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class DecisionJobTransformation extends AbstractJobTransformation implements ASTTransformation {

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
            boolean isDecisionJob = false
            boolean inheritsAbstractJob = false
            classNode.getAllInterfaces().each {
                if (it.name == "de.dkfz.tbi.otp.job.processing.DecisionJob") {
                    isDecisionJob = true
                }
            }
            if (classNode.superClass.name == "de.dkfz.tbi.otp.job.processing.AbstractDecisionJobImpl") {
                isDecisionJob = true
                inheritsAbstractJob = true
            }
            if (!isDecisionJob) {
                return
            }
            if (inheritsAbstractJob) {
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
            // add annotation nodes for scope
            addScopeAnnotation(classNode)
            addComponentAnnotation(classNode)

            // add the Annotation to the execute method and generates the version
            classNode.getMethods().each { method ->
                if (method.getName() == "execute") {
                    method.addAnnotation(new AnnotationNode(new ClassNode(this.class.classLoader.loadClass("de.dkfz.tbi.otp.job.scheduler.JobExecution"))))
                }
                createGetVersion(method)
            }
        }
    }

}
