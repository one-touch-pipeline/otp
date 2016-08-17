package de.dkfz.tbi.otp.job.ast;

import org.codehaus.groovy.transform.*;

import java.lang.annotation.*;


@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(classes = {UseJobLogTransformation.class})
public @interface UseJobLog {
}
