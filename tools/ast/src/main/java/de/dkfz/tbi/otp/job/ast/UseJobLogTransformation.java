package de.dkfz.tbi.otp.job.ast;

import groovyjarjarasm.asm.*;
import org.apache.commons.logging.*;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.transform.*;


/**
 * This AST transformation adds a field "private Log log" to classes annotated
 * with {@link UseJobLog}.
 * This is used for the job log system, which logs to a separate log file
 * for every {@link de.dkfz.tbi.otp.job.processing.Job} instance resp.
 * {@link de.dkfz.tbi.otp.job.processing.ProcessingStep} by setting "log" to a
 * new {@link de.dkfz.tbi.otp.utils.logging.JobLog}
 * (in {@link de.dkfz.tbi.otp.job.scheduler.SchedulerService#createJob})
 * in every new job object after it is created.
 * Without this AST transformation Grails would automatically add a static log
 * field to all jobs (in {@link org.codehaus.groovy.grails.compiler.logging.LoggingTransformer}),
 * which would mean every job from the same class would use the same log.
 *
 * @see UseJobLog
 * @see de.dkfz.tbi.otp.utils.logging.JobLog
 * @see de.dkfz.tbi.otp.job.scheduler.SchedulerService#createJob
 * @see de.dkfz.tbi.otp.utils.logging.JobLogMessage
 * @see de.dkfz.tbi.otp.utils.logging.JobAppender
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class UseJobLogTransformation implements ASTTransformation {

    public UseJobLogTransformation() {}

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof ClassNode)) {
            return;
        }
        addLog((ClassNode) astNodes[1]);
    }

    private static void addLog(ClassNode classNode) {
        classNode.addField("log", Opcodes.ACC_PRIVATE, new ClassNode(Log.class), new EmptyExpression());
    }
}
