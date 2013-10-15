package de.dkfz.tbi.ngstools.qualityAssessment;

import java.lang.annotation.*

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileCanRead {
}
