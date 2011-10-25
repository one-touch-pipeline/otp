package de.dkfz.tbi.otp.job.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
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
            classNode.getAllInterfaces().each {
                if (it.name == "de.dkfz.tbi.otp.job.processing.StartJob") {
                    isStartJob = true
                }
                if (!isStartJob) {
                    return
                }

                classNode.getMethods().each { MethodNode method ->
                    createGetVersion(method)
                }
            }
        }
    }
}
