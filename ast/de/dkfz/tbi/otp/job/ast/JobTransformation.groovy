package de.dkfz.tbi.otp.job.ast

import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST Transformation adding the JobExecution annotation to the Job's execute method.
 * 
 * This transformation finds all classes implementing the Job interface and annotates
 * the execute method with the {@link JobExecution} annotation, so that implementers of
 * this interface do not have to care about it.
 *
 * The annotation JobExecution is required by the Aspect intercepting the JobExecution.
 *
 * @see JobExecution
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class JobTransformation implements ASTTransformation {

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        List classNodes = sourceUnit.getAST()?.getClasses()
        
        classNodes.each { classNode ->
            if (classNode.isInterface()) {
                // skip interfaces
                return
            }
            // test whether the class implements the Job interface
            boolean isJob = false
            classNode.getAllInterfaces().each {
                if (it.name == "de.dkfz.tbi.otp.job.processing.Job") {
                    isJob = true
                }
            }
            if (!isJob) {
                return
            }

            // add the Annotation to the execute method
            classNode.getMethods().each { method ->
                if (method.getName() == "execute") {
                    method.addAnnotation(new AnnotationNode(new ClassNode(this.class.classLoader.loadClass("de.dkfz.tbi.otp.job.scheduler.JobExecution"))))
                }
            }
        }
    }

}
