package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.transform.*

import static org.codehaus.groovy.ast.expr.ArgumentListExpression.*

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
                println "Applying StartJobTransformation to ${classNode}"  // log.debug instead of println does not produce any output on stdout
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
                MethodNode executeMethod = classNode.getMethod('execute', Parameter.EMPTY_ARRAY)
                VariableScope scope = executeMethod.variableScope

                Statement wrappedExecuteMethod = new TryCatchStatement(
                        // try
                        new BlockStatement([
                                persistenceInterceptor('init'),
                                executeMethod.code,
                        ], scope),
                        // finally
                        new BlockStatement([
                                persistenceInterceptor('flush'),
                                persistenceInterceptor('destroy'),
                        ], scope)
                )
                executeMethod.setCode(wrappedExecuteMethod)
            }

            classNode.getMethods().each { MethodNode method ->
                createGetVersion(method)
            }
        }
    }

    private static Statement persistenceInterceptor(String method) {
        new ExpressionStatement(new MethodCallExpression(new VariableExpression('persistenceInterceptor'), method, EMPTY_ARGUMENTS))
    }
}
