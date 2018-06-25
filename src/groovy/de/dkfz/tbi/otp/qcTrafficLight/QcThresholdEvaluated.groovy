package de.dkfz.tbi.otp.qcTrafficLight

import java.lang.annotation.*

import static java.lang.annotation.ElementType.*

@Target([METHOD, FIELD])
@Retention(RetentionPolicy.RUNTIME)
public @interface QcThresholdEvaluated {
}
