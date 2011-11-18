package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST Transformation for the StartJob classes.
 *
 * Similar to {@link JobTransformation}: generates the getVersion method.
 * @see JobTransformation
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class StartJobTransformation extends AbstractJobTransformation implements
        ASTTransformation {

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        List classNodes = sourceUnit.getAST()?.getClasses()
        
        classNodes.each { ClassNode classNode ->
            // test whether the class implements the StartJob interface
            boolean isStartJob = false
            boolean inheritsAbstractJob = false
            classNode.getAllInterfaces().each {
                if (it.name == "de.dkfz.tbi.otp.job.processing.StartJob") {
                    isStartJob = true
                }
            }
            if (classNode.superClass.name == "de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl") {
                isStartJob = true
                inheritsAbstractJob = true
            }
            if (!isStartJob) {
                return
            }
            if (inheritsAbstractJob) {
                println "Inherits AbstractStartJobImpl"
                // add generic Constructor
                classNode.addConstructor(new ConstructorNode(Opcodes.ACC_PUBLIC, new BlockStatement()))
                // add constructor with one arguments calling the super class constructor
                Parameter[] params = [new Parameter(classNode.superClass.getField("plan").type, "a")] as Parameter[]
                BlockStatement stmt = new BlockStatement()
                stmt.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.SUPER, new ArgumentListExpression(new VariableExpression("a")))))
                classNode.addConstructor(Opcodes.ACC_PUBLIC, params, ClassNode.EMPTY_ARRAY, stmt)
                // add getVersion method - actual code will be generated below
                classNode.addMethod("getVersion", Opcodes.ACC_PUBLIC, new ClassNode(String), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, new ReturnStatement(new ConstantExpression("")))

                // create the try-finally block for persistenceInterceptor in execute method
                MethodNode executeMethod = classNode.getMethod("execute", Parameter.EMPTY_ARRAY)
                BlockStatement methodCode = new BlockStatement()
                methodCode.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("persistenceInterceptor"), "init", ArgumentListExpression.EMPTY_ARGUMENTS)))
                methodCode.addStatement(executeMethod.code)
                TryCatchStatement tryCatchStatement = new TryCatchStatement(methodCode, createPersistenceInterceptorFinally())
                executeMethod.setCode(methodCode)
            }

            classNode.getMethods().each { MethodNode method ->
                createGetVersion(method)
            }
        }
    }

    private Statement createPersistenceInterceptorFinally() {
        BlockStatement stmt = new BlockStatement()
        stmt.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("persistenceInterceptor"), "flush", ArgumentListExpression.EMPTY_ARGUMENTS)))
        stmt.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("persistenceInterceptor"), "destroy", ArgumentListExpression.EMPTY_ARGUMENTS)))
        return stmt
    }
}
