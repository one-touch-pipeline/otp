/*
 * Copyright 2011-2019 The OTP authors
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
