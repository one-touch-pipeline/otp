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

package de.dkfz.tbi.otp.qcTrafficLight

import grails.util.Holders
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

import java.lang.reflect.Member

@ToString(includeNames = true, includePackage = false)
class QcThreshold implements Entity {

    Project project

    SeqType seqType
    // i.e. duplicates
    String qcProperty1
    // this value can be nullable, it can be used if two qc values shall be compared
    String qcProperty2
    Double errorThresholdLower
    Double warningThresholdLower
    Double warningThresholdUpper
    Double errorThresholdUpper
    ThresholdStrategy compare
    // it can happen that a property has the same name in different qc classes, therefore the class has also to be stored
    String qcClass

    static constraints = {
        qcProperty1(validator: { val, obj, Errors errors ->
            QcThreshold qcThreshold = CollectionUtils.atMostOneElement(QcThreshold.findAllByProjectAndSeqTypeAndQcClassAndQcProperty1(
                    obj.project, obj.seqType, obj.qcClass, val))
            if (qcThreshold && qcThreshold != obj) {
                errors.rejectValue('qcProperty1', "default.invalid.validator.message",
                        [QcThreshold.simpleName, "qcProperty1", val].toArray(),
                        "QcThreshold for '${obj.qcClass}.${val}' with sequencing type '${obj.seqType}' in project '${obj.project}' already exists")
            }
            validateProperty(val, obj.qcClass, errors, "qcProperty1")
        })
        project nullable: true
        qcProperty2 nullable: true, validator: { val, obj, Errors errors ->
            if (val != null && val == obj.qcProperty1) {
                errors.rejectValue 'qcProperty2', "default.invalid.validator.message",
                        [QcThreshold.simpleName, "qcProperty2", val].toArray(),
                        "property 1 must not be the same as property 2"
            }
            if (obj.compare == ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY) {
                if (val == null) {
                    errors.rejectValue 'qcProperty2', "default.invalid.validator.message",
                            [QcThreshold.simpleName, "qcProperty2", val].toArray(),
                            "Specify which property to compare to (property 2 not set)"
                } else {
                    validateProperty(val, obj.qcClass, errors, "qcProperty2")
                }
            }
            if (obj.compare != ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY && val) {
                errors.rejectValue 'qcProperty2', "default.invalid.validator.message",
                        [QcThreshold.simpleName, "qcProperty2", val].toArray(),
                        "Property 2 must not be set when not using it"
            }
        }
        warningThresholdLower(nullable: true, validator: { val, obj, Errors errors ->
            if (obj.compare in [ThresholdStrategy.ABSOLUTE_LIMITS, ThresholdStrategy.RATIO_TO_EXTERNAL_VALUE]) {
                if (val == null) {
                    if (obj.errorThresholdLower != null) {
                        errors.rejectValue('warningThresholdLower', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdLower", val].toArray(),
                                'Please also set a lower WARNING threshold when defining a lower ERROR threshold')
                    }
                    if (obj.warningThresholdUpper == null || obj.errorThresholdUpper == null) {
                        errors.rejectValue('warningThresholdLower', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdLower", val].toArray(),
                                'When leaving the lower thresholds empty, please define BOTH upper warning and upper error thresholds.')
                    }
                } else {
                    if (obj.errorThresholdLower == null) {
                        errors.rejectValue('warningThresholdLower', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdLower", val].toArray(),
                                'When setting a lower WARNING threshold, please also define the lower ERROR threshold')
                    }
                    if (obj.warningThresholdUpper != null && val >= obj.warningThresholdUpper) {
                        errors.rejectValue('warningThresholdLower', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdLower", val].toArray(),
                                'LOWER warning threshold must be smaller than UPPER warning threshold')
                    }
                    if (val <= obj.errorThresholdLower) {
                        errors.rejectValue('warningThresholdLower', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdLower", val].toArray(),
                                'Lower WARNING threshold must be bigger than lower ERROR threshold')
                    }
                }
            }
        })
        warningThresholdUpper(nullable: true, validator: { val, obj, Errors errors ->
            if (obj.compare in [ThresholdStrategy.ABSOLUTE_LIMITS, ThresholdStrategy.RATIO_TO_EXTERNAL_VALUE]) {
                if (val == null) {
                    if (obj.errorThresholdUpper != null) {
                        errors.rejectValue('warningThresholdUpper', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdUpper", val].toArray(),
                                'Please also set a upper WARNING threshold when defining a upper ERROR threshold')
                    }
                } else {
                    if (obj.errorThresholdUpper == null) {
                        errors.rejectValue('warningThresholdUpper', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdUpper", val].toArray(),
                                'When setting an upper WARNING threshold, please also define an upper ERROR threshold')
                    }
                    if (val >= obj.errorThresholdUpper) {
                        errors.rejectValue('warningThresholdUpper', "default.invalid.validator.message",
                                [QcThreshold.simpleName, "warningThresholdUpper", val].toArray(),
                                'Upper WARNING threshold must be smaller than upper ERROR threshold')
                    }
                }
            }
        })
        errorThresholdLower(nullable: true)
        errorThresholdUpper(nullable: true)
        qcClass validator: { val, obj, Errors errors ->
            validateClass(val, errors, "qcClass")
        }
    }


    @TupleConstructor
    static enum ThresholdStrategy {
        ABSOLUTE_LIMITS("absolute limits"),
        DIFFERENCE_WITH_OTHER_PROPERTY("difference with other property"),
        RATIO_TO_EXTERNAL_VALUE("ratio to external value"),

        final String displayName
    }

    static enum ThresholdLevel {
        OKAY,
        WARNING,
        ERROR,
    }


    static void validateProperty(String val, String cls, Errors errors, String propertyName) {
        if (!(val in getValidQcPropertyForQcClass(cls))) {
            errors.rejectValue(propertyName, "default.invalid.validator.message",
                    [QcThreshold.simpleName, propertyName, val].toArray(),
                    "'${val}' is not a valid property for ${cls}")
        }
    }

    static void validateClass(String val, Errors errors, String propertyName) {
        if (!(val in getValidQcClass()*.name)) {
            errors.rejectValue(propertyName, "default.invalid.validator.message",
                    [QcThreshold.simpleName, propertyName, val].toArray(),
                    "'${val}' is not a valid qc class")
        }
    }


    private static List<Member> getAnnotatedMembers(Class type) {
        List<Member> members = []
        for (Class c = type; c != null; c = c.getSuperclass()) {
            [c.declaredFields, c.declaredMethods].each {
                members.addAll(it.findAll { it?.isAnnotationPresent(QcThresholdEvaluated) })
            }
        }
        return members
    }

    static List<String> getValidQcPropertyForQcClass(String cl) {
        Class clasz = getValidQcClass().find { it.name == cl }
        if (clasz) {
            List<String> propertiesWithAnnotations = getAnnotatedMembers(clasz)
                    .collect { it.name }

            clasz.metaClass.properties.findAll {
                it.name in propertiesWithAnnotations ||
                        MetaProperty.getGetterName(it.name, it.type) in propertiesWithAnnotations
            }.collect {
                it.name
            }.sort()
        } else {
            []
        }
    }

    static String convertShortToLongName(String s) {
        getValidQcClass().find { it.simpleName == s }.name
    }

    static List<Class> getValidQcClass() {
        GrailsClass[] classes = Holders.grailsApplication.getArtefacts('Domain')
        classes*.clazz.findAll { QcTrafficLightValue.class.isAssignableFrom(it) }
    }


    ThresholdLevel qcPassed(QcTrafficLightValue qc, Double externalValue = 0) {
        switch (this.compare) {
            case ThresholdStrategy.ABSOLUTE_LIMITS:
                compareToThreshold(qc)
                break
            case ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY:
                compareThresholdToDifferenceWithQcProperty2(qc)
                break
            case ThresholdStrategy.RATIO_TO_EXTERNAL_VALUE:
                compareThresholdToRatioWithExternalValue(qc, externalValue)
                break
            default: throw new RuntimeException("No other comparison is defined yet")
        }
    }

    private ThresholdLevel compareToThreshold(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null) {
            return ThresholdLevel.OKAY
        }
        return getWarningLevel(qc."${qcProperty1}")
    }

    private ThresholdLevel compareThresholdToDifferenceWithQcProperty2(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null || qc."${qcProperty2}" == null) {
            return ThresholdLevel.OKAY
        }
        return getWarningLevel(qc."${qcProperty1}" - qc."${qcProperty2}")
    }

    private ThresholdLevel compareThresholdToRatioWithExternalValue(QcTrafficLightValue qc, Double externalValue) {
        if (qc."${qcProperty1}" == null || externalValue == null || externalValue == 0) {
            return ThresholdLevel.OKAY
        }
        return getWarningLevel(qc."${qcProperty1}" / externalValue)
    }

    private ThresholdLevel getWarningLevel(double value) {
        return warningLevel(
                ((value < (warningThresholdLower != null ? warningThresholdLower : Double.MIN_VALUE)) ||
                        ((warningThresholdUpper != null ? warningThresholdUpper : Double.MAX_VALUE) < value)),
                ((value < (errorThresholdLower != null ? errorThresholdLower : Double.MIN_VALUE)) ||
                        ((errorThresholdUpper != null ? errorThresholdUpper : Double.MAX_VALUE) < value))
        )
    }

    private ThresholdLevel warningLevel(boolean conditionForWarning, boolean conditionForError) {
        if (conditionForError) {
            return ThresholdLevel.ERROR
        } else if (conditionForWarning) {
            return ThresholdLevel.WARNING
        } else {
            return ThresholdLevel.OKAY
        }
    }
}
