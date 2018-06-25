package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

class IndelQualityControl implements Entity, QcTrafficLightValue {

    IndelCallingInstance indelCallingInstance

    String file

    @QcThresholdEvaluated
    int numIndels

    @QcThresholdEvaluated
    int numIns

    @QcThresholdEvaluated
    int numDels

    @QcThresholdEvaluated
    int numSize1_3

    @QcThresholdEvaluated
    int numSize4_10

    @QcThresholdEvaluated
    int numSize11plus

    @QcThresholdEvaluated
    int numInsSize1_3

    @QcThresholdEvaluated
    int numInsSize4_10

    @QcThresholdEvaluated
    int numInsSize11plus

    @QcThresholdEvaluated
    int numDelsSize1_3

    @QcThresholdEvaluated
    int numDelsSize4_10

    @QcThresholdEvaluated
    int numDelsSize11plus

    @QcThresholdEvaluated
    double percentIns

    @QcThresholdEvaluated
    double percentDels

    @QcThresholdEvaluated
    double percentSize1_3

    @QcThresholdEvaluated
    double percentSize4_10

    @QcThresholdEvaluated
    double percentSize11plus

    @QcThresholdEvaluated
    double percentInsSize1_3

    @QcThresholdEvaluated
    double percentInsSize4_10

    @QcThresholdEvaluated
    double percentInsSize11plus

    @QcThresholdEvaluated
    double percentDelsSize1_3

    @QcThresholdEvaluated
    double percentDelsSize4_10

    @QcThresholdEvaluated
    double percentDelsSize11plus


    static constraints = {
        file(validator: { OtpPath.isValidAbsolutePath(it) })
        indelCallingInstance unique: true
    }

    static belongsTo = [
        indelCallingInstance: IndelCallingInstance
    ]

    static mapping = {
        file type: "text"
    }
}
