package de.dkfz.tbi.otp.qcTrafficLight

import java.lang.annotation.*

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.ElementType.METHOD

@Target([METHOD, FIELD])
@Retention(RetentionPolicy.RUNTIME)
@interface QcThresholdEvaluated {
}
