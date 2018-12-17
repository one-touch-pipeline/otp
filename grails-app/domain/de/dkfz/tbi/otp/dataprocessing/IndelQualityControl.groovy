package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue
import de.dkfz.tbi.otp.utils.Entity

class IndelQualityControl implements Entity, QcTrafficLightValue {

    IndelCallingInstance indelCallingInstance

    String file

    @QcThresholdEvaluated
    int numIndels

    @QcThresholdEvaluated
    int numIns

    @QcThresholdEvaluated
    int numDels

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numSize4_10

    @QcThresholdEvaluated
    int numSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numInsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numInsSize4_10

    @QcThresholdEvaluated
    int numInsSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numDelsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    int numDelsSize4_10

    @QcThresholdEvaluated
    int numDelsSize11plus

    @QcThresholdEvaluated
    double percentIns

    @QcThresholdEvaluated
    double percentDels

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentSize4_10

    @QcThresholdEvaluated
    double percentSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentInsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentInsSize4_10

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentInsSize11plus

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentDelsSize1_3

    @SuppressWarnings("PropertyName")
    @QcThresholdEvaluated
    double percentDelsSize4_10

    @QcThresholdEvaluated
    double percentDelsSize11plus


    static constraints = {
        file(validator: { OtpPath.isValidAbsolutePath(it) })
        indelCallingInstance unique: true
    }

    static belongsTo = [
        indelCallingInstance: IndelCallingInstance,
    ]

    static mapping = {
        file type: "text"
    }
}
