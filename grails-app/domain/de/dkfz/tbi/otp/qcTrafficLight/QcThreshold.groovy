package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class QcThreshold implements Entity {

    Project project

    SeqType seqType
    // i.e. duplicates
    String qcProperty1
    // this value can be nullable, it can be used if two qc values shall be compared
    String qcProperty2
    double warningThreshold
    double errorThreshold
    Compare compare
    // it can happen that a property has the same name in different qc classes, therefore the class has also to be stored
    String qcClass

    static constraints = {
        project nullable: true
        seqType nullable: true
        qcProperty2 nullable: true
    }

    static enum Compare {
        biggerThanThreshold,
        smallerThanThreshold,
        biggerThanQcProperty2,
        smallerThanQcProperty2,
        biggerThanThresholdFactorExternalValue,
        smallerThanThresholdFactorExternalValue,
    }

    static enum WarningLevel {
        NO('okay'),
        WarningLevel1('warning1'),
        WarningLevel2('warning2')

        final String styleClass

        WarningLevel(String styleClass) {
            this.styleClass = styleClass
        }
    }

    WarningLevel qcPassed(QcTrafficLightValue qc, double externalValue = 0) {
        switch (this.compare) {
            case QcThreshold.Compare.biggerThanQcProperty2:
                isBiggerThanQcProperty2(qc)
                break
            case QcThreshold.Compare.smallerThanQcProperty2:
                isSmallerThanQcProperty2(qc)
                break
            case QcThreshold.Compare.biggerThanThreshold:
                isBiggerThanThreshold(qc)
                break
            case QcThreshold.Compare.smallerThanThreshold:
                isSmallerThanThreshold(qc)
                break
            case QcThreshold.Compare.biggerThanThresholdFactorExternalValue:
                isBiggerThanThresholdFactorExternalValue(qc, externalValue)
                break
            case QcThreshold.Compare.smallerThanThresholdFactorExternalValue:
                isSmallerThanThresholdFactorExternalValue(qc, externalValue)
                break
            default: throw new RuntimeException("No other comparison is defined yet")
        }
    }

    private WarningLevel isBiggerThanQcProperty2(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null || qc."${qcProperty2}" == null) {
            return WarningLevel.NO
        }
        return warningLevel((qc."${qcProperty1}" - qc."${qcProperty2}") > warningThreshold, (qc."${qcProperty1}" - qc."${qcProperty2}") > errorThreshold)
    }

    private WarningLevel isSmallerThanQcProperty2(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null || qc."${qcProperty2}" == null) {
            return WarningLevel.NO
        }
        return warningLevel((qc."${qcProperty2}" - qc."${qcProperty1}") > warningThreshold, (qc."${qcProperty2}" - qc."${qcProperty1}") > errorThreshold)
    }

    private WarningLevel isBiggerThanThreshold(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null) {
            return WarningLevel.NO
        }
        return warningLevel(qc."${qcProperty1}" > warningThreshold, qc."${qcProperty1}" > errorThreshold)
    }

    private WarningLevel isSmallerThanThreshold(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null) {
            return WarningLevel.NO
        }
        return warningLevel(qc."${qcProperty1}" < warningThreshold, qc."${qcProperty1}" < errorThreshold)
    }

    private WarningLevel isBiggerThanThresholdFactorExternalValue(QcTrafficLightValue qc, double externalValue) {
        if (qc."${qcProperty1}" == null) {
            return WarningLevel.NO
        }
        return warningLevel(qc."${qcProperty1}" > (warningThreshold * externalValue), qc."${qcProperty1}" > (errorThreshold * externalValue))
    }

    private WarningLevel isSmallerThanThresholdFactorExternalValue(QcTrafficLightValue qc, double externalValue) {
        if (qc."${qcProperty1}" == null) {
            return WarningLevel.NO
        }
        return warningLevel (qc."${qcProperty1}" < (warningThreshold * externalValue), qc."${qcProperty1}" < (errorThreshold * externalValue))
    }

    private static WarningLevel warningLevel(boolean conditionForLevel1, boolean conditionForLevel2) {
        if (conditionForLevel2) {
            return WarningLevel.WarningLevel2
        } else if (conditionForLevel1) {
            return WarningLevel.WarningLevel1
        } else {
            return WarningLevel.NO
        }
    }
}
