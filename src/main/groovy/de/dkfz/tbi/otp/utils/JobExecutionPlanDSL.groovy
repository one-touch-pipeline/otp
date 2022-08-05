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
package de.dkfz.tbi.otp.utils

import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*

class Helper {
    JobDefinition firstJob = null
    JobDefinition previous = null
    JobDefinition watchdogJobDefinition = null
}

@SuppressWarnings("UnusedPrivateField")
@Slf4j
class JobExecutionPlanDSL {

    private final static Closure CONSTANT_PARAMETER_CLOSURE = { JobDefinition jobDefinition, String typeName, String value ->
        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        type.save(flush: true)
        Parameter parameter = new Parameter(type: type, value: value)
        parameter.save(flush: true)
        jobDefinition.addToConstantParameters(parameter)
        jobDefinition.save(flush: true)
    }

    private final static Closure OUTPUT_PARAMETER_CLOSURE = { JobDefinition jobDefinition, String typeName ->
        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        type.save(flush: true)
    }

    private final static Closure INPUT_PARAMETER_CLOSURE = { JobDefinition jobDefinition,
                                                             JobDefinition previous,
                                                             JobExecutionPlan jep,
                                                             String typeName,
                                                             String fromJob,
                                                             String fromParameter ->
        ParameterType inputType = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        inputType.save(flush: true)
        if (!previous) {
            // first job - get it from StartJob
            ParameterType outputType = CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jep.startJob, fromParameter))
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            ParameterMapping mapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            mapping.save(flush: true)
        } else if (previous.name == fromJob) {
            // simple case: direct mapping
            ParameterType outputType = CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(previous, fromParameter))
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            ParameterMapping mapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            mapping.save(flush: true)
        } else {
            // need to add passthrough parameters
            JobDefinition mappingJob = CollectionUtils.atMostOneElement(JobDefinition.findAllByNameAndPlan(fromJob, jep))
            assert(mappingJob)
            ParameterType outputType = CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(mappingJob, fromParameter))
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            while (mappingJob.next != jobDefinition) {
                ParameterType passThroughType = CollectionUtils.atMostOneElement(
                        ParameterType.findAllByJobDefinitionAndName(mappingJob.next, fromParameter + "Passthrough"))
                if (!passThroughType) {
                    passThroughType = new ParameterType(
                            name: fromParameter + "Passthrough",
                            jobDefinition: mappingJob.next ? mappingJob.next : firstJob,
                            parameterUsage: ParameterUsage.PASSTHROUGH)
                    passThroughType.save(flush: true)
                }
                ParameterMapping mapping = CollectionUtils.atMostOneElement(ParameterMapping.findAllByFromAndTo(outputType, passThroughType))
                if (!mapping) {
                    mapping = new ParameterMapping(from: outputType, to: passThroughType, job: mappingJob.next ? mappingJob.next : firstJob)
                    mapping.save(flush: true)
                }
                // next run
                outputType = passThroughType
                mappingJob = mappingJob.next ?: firstJob
            }
            ParameterMapping finalMapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            finalMapping.save(flush: true)
        }
    }

    @SuppressWarnings('ParameterReassignment')
    private final static Closure START_JOB_CLOSURE = { JobExecutionPlan jep, Boolean startJobDefined, String startName, String bean, closure = null ->
        assert !startJobDefined, "Only one Start Job Definition can be defined per Job Execution Plan"
        StartJobDefinition startJobDefinition = new StartJobDefinition(name: startName, bean: bean, plan: jep)
        startJobDefinition.save(flush: true)
        jep.startJob = startJobDefinition
        jep.save(flush: true)
        if (closure) {
            closure.metaClass = new ExpandoMetaClass(closure.class)
            closure.metaClass.constantParameter = { String typeName, String value ->
                JobExecutionPlanDSL.CONSTANT_PARAMETER_CLOSURE(startJobDefinition, typeName, value)
            }
            closure.metaClass.outputParameter = { String typeName ->
                JobExecutionPlanDSL.OUTPUT_PARAMETER_CLOSURE(startJobDefinition, typeName)
            }
            closure.metaClass.initialize()
            closure()
        }
        startJobDefined = true
    }

    private final static Closure WATCHDOG_CLOSURE = { JobDefinition jobDefinition, JobExecutionPlan jep, Helper helper, String watchdogBean ->
        ParameterType type = new ParameterType(
                name: JobParameterKeys.JOB_ID_LIST, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT
        )
        type.save(flush: true)
        ParameterType realmOutputType = new ParameterType(
                name: JobParameterKeys.REALM,
                jobDefinition: jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT,
                className: "de.dkfz.tbi.otp.ngsdata.Realm"
        )
        realmOutputType.save(flush: true)
        JobDefinition watchdogJobDefinition = new JobDefinition(
                name: "__WatchdogFor__" + jobDefinition.name, bean: watchdogBean, plan: jep, previous: jobDefinition
        )
        watchdogJobDefinition.save(flush: true)
        ParameterType inputType = new ParameterType(
                name: JobParameterKeys.JOB_ID_LIST, jobDefinition: watchdogJobDefinition, parameterUsage: ParameterUsage.INPUT
        )
        inputType.save(flush: true)
        ParameterType realmInputType = new ParameterType(
                name: JobParameterKeys.REALM,
                jobDefinition: watchdogJobDefinition,
                parameterUsage: ParameterUsage.INPUT,
                className: "de.dkfz.tbi.otp.ngsdata.Realm"
        )
        realmInputType.save(flush: true)
        ParameterMapping mapping = new ParameterMapping(from: type, to: inputType, job: watchdogJobDefinition)
        mapping.save(flush: true)
        ParameterMapping realmMapping = new ParameterMapping(from: realmOutputType, to: realmInputType, job: watchdogJobDefinition)
        realmMapping.save(flush: true)
        helper.watchdogJobDefinition = watchdogJobDefinition
    }

    private final static Closure JOB_CLOSURE = { JobExecutionPlan jep, Helper helper, String jobName, String bean, closure = null ->
        log.debug("In job Closure with " + jobName)
        JobDefinition jobDefinition = new JobDefinition(name: jobName, bean: bean, plan: jep, previous: helper.previous)
        jobDefinition.save(flush: true)
        helper.firstJob = helper.firstJob ?: jobDefinition
        if (helper.previous) {
            helper.previous.next = jobDefinition
            helper.previous.save(flush: true)
        }
        if (closure) {
            closure.metaClass = new ExpandoMetaClass(closure.class)
            closure.metaClass.constantParameter = { String typeName, String value ->
                JobExecutionPlanDSL.CONSTANT_PARAMETER_CLOSURE(jobDefinition, typeName, value)
            }
            closure.metaClass.outputParameter = { String typeName ->
                JobExecutionPlanDSL.OUTPUT_PARAMETER_CLOSURE(jobDefinition, typeName)
            }
            closure.metaClass.inputParameter = { String typeName, String fromJob, String fromParameter ->
                JobExecutionPlanDSL.INPUT_PARAMETER_CLOSURE(jobDefinition, helper.previous, jep, typeName, fromJob, fromParameter)
            }
            closure.metaClass.watchdog = { String watchdogBean ->
                JobExecutionPlanDSL.WATCHDOG_CLOSURE(jobDefinition, jep, helper, watchdogBean)
            }
            closure.metaClass.initialize()
            closure()
        }
        helper.previous = jobDefinition
    }

    private final static Closure VALIDATING_JOB_CLOSURE = { JobExecutionPlan jep,
                                                            Helper helper,
                                                            String jobName,
                                                            String bean,
                                                            String validatorForName,
                                                            closure = null ->
        JobDefinition validatorFor = CollectionUtils.atMostOneElement(JobDefinition.findAllByNameAndPlan(validatorForName, jep))
        assert(validatorFor)
        ValidatingJobDefinition jobDefinition = new ValidatingJobDefinition(
                name: jobName, bean: bean, plan: jep, previous: helper.previous, validatorFor: validatorFor
        )
        jobDefinition.save(flush: true)
        helper.firstJob = helper.firstJob ?: jobDefinition
        if (helper.previous) {
            helper.previous.next = jobDefinition
            helper.previous.save(flush: true)
        }
        if (closure) {
            closure.metaClass = new ExpandoMetaClass(closure.class)
            closure.metaClass.constantParameter = { String typeName, String value ->
                JobExecutionPlanDSL.CONSTANT_PARAMETER_CLOSURE(jobDefinition, typeName, value)
            }
            closure.metaClass.outputParameter = { String typeName ->
                JobExecutionPlanDSL.OUTPUT_PARAMETER_CLOSURE(jobDefinition, typeName)
            }
            closure.metaClass.inputParameter = { String typeName, String fromJob, String fromParameter ->
                JobExecutionPlanDSL.INPUT_PARAMETER_CLOSURE(jobDefinition, helper.previous, jep, typeName, fromJob, fromParameter)
            }
            closure.metaClass.watchdog = { String watchdogBean ->
                JobExecutionPlanDSL.WATCHDOG_CLOSURE(jobDefinition, jep, helper, watchdogBean)
            }
            closure.metaClass.initialize()
            closure()
        }
        helper.previous = jobDefinition
    }

    static Closure plan = { String name, def ctx = null, boolean validate = false, c ->
        // If there is a previous plan, obsolete it
        JobExecutionPlan plan = CollectionUtils.atMostOneElement(JobExecutionPlan.findAllByNameAndObsoleted(name, false))
        plan?.obsoleted = true
        plan?.save(flush: true)
        int version = plan ? plan.planVersion + 1 : 0

        // create the new/updated plan
        JobExecutionPlan.withTransaction {
            JobExecutionPlan jep = new JobExecutionPlan(name: name, planVersion: version, enabled: true, previousPlan: plan)
            jep.save(flush: true)
            Helper helper = new Helper()
            Boolean startJobDefined = false

            // need to create our own meta class to be able to add properties
            c.metaClass = new ExpandoMetaClass(c.class)
            c.metaClass.start = { String n, String bean, closure = null ->
                JobExecutionPlanDSL.START_JOB_CLOSURE(jep, startJobDefined, n, bean, closure)
            }
            c.metaClass.job = { String n, String bean, closure = null ->
                JobExecutionPlanDSL.JOB_CLOSURE(jep, helper, n, bean, closure)
            }
            c.metaClass.validatingJob = { String jobName, String bean, String validatorForName, closure = null ->
                JobExecutionPlanDSL.VALIDATING_JOB_CLOSURE(jep, helper, jobName, bean, validatorForName, closure)
            }
            c.metaClass.initialize()
            c()
            jep.firstJob = helper.firstJob
            jep.save(flush: true)
            if (validate && ctx) {
                List<String> errors = ctx.planValidatorService.validate(jep)
                if (!errors.isEmpty()) {
                    log.debug("Errors found during validation")
                    errors.each { log.debug(it) }
                }
                assert(errors.isEmpty())
            }
        }
        log.debug("Plan created")
    }
}
