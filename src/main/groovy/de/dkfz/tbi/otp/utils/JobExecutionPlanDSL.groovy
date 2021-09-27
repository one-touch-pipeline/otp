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

    private static Closure constantParameterClosure = { JobDefinition jobDefinition, String typeName, String value ->
        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        type.save()
        Parameter parameter = new Parameter(type: type, value: value)
        parameter.save()
        jobDefinition.addToConstantParameters(parameter)
        jobDefinition.save()
    }

    private static Closure outputParameterClosure = { JobDefinition jobDefinition, String typeName ->
        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        type.save()
    }

    private static Closure inputParameterClosure = { JobDefinition jobDefinition,
                                                     JobDefinition previous,
                                                     JobExecutionPlan jep,
                                                     String typeName,
                                                     String fromJob,
                                                     String fromParameter ->
        ParameterType inputType = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        inputType.save()
        if (!previous) {
            // first job - get it from StartJob
            ParameterType outputType = ParameterType.findByJobDefinitionAndName(jep.startJob, fromParameter)
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            ParameterMapping mapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            mapping.save()
        } else if (previous.name == fromJob) {
            // simple case: direct mapping
            ParameterType outputType = ParameterType.findByJobDefinitionAndName(previous, fromParameter)
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            ParameterMapping mapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            mapping.save()
        } else {
            // need to add passthrough parameters
            JobDefinition mappingJob = JobDefinition.findByNameAndPlan(fromJob, jep)
            assert(mappingJob)
            ParameterType outputType = ParameterType.findByJobDefinitionAndName(mappingJob, fromParameter)
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            while (mappingJob.next != jobDefinition) {
                ParameterType passThroughType = ParameterType.findByJobDefinitionAndName(mappingJob.next, fromParameter + "Passthrough")
                if (!passThroughType) {
                    passThroughType = new ParameterType(
                            name: fromParameter + "Passthrough",
                            jobDefinition: mappingJob.next ? mappingJob.next : firstJob,
                            parameterUsage: ParameterUsage.PASSTHROUGH)
                    passThroughType.save()
                }
                ParameterMapping mapping = ParameterMapping.findByFromAndTo(outputType, passThroughType)
                if (!mapping) {
                    mapping = new ParameterMapping(from: outputType, to: passThroughType, job: mappingJob.next ? mappingJob.next : firstJob)
                    mapping.save()
                }
                // next run
                outputType = passThroughType
                mappingJob = mappingJob.next ?: firstJob
            }
            ParameterMapping finalMapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            finalMapping.save()
        }
    }

    @SuppressWarnings('ParameterReassignment')
    private static Closure startJobClosure = { JobExecutionPlan jep, Boolean startJobDefined, String startName, String bean, closure = null ->
        assert !startJobDefined, "Only one Start Job Definition can be defined per Job Execution Plan"
        StartJobDefinition startJobDefinition = new StartJobDefinition(name: startName, bean: bean, plan: jep)
        startJobDefinition.save()
        jep.startJob = startJobDefinition
        jep.save()
        if (closure) {
            closure.metaClass = new ExpandoMetaClass(closure.class)
            closure.metaClass.constantParameter = { String typeName, String value ->
                JobExecutionPlanDSL.constantParameterClosure(startJobDefinition, typeName, value)
            }
            closure.metaClass.outputParameter = { String typeName ->
                JobExecutionPlanDSL.outputParameterClosure(startJobDefinition, typeName)
            }
            closure.metaClass.initialize()
            closure()
        }
        startJobDefined = true
    }

    private static Closure watchdogClosure = { JobDefinition jobDefinition, JobExecutionPlan jep, Helper helper, String watchdogBean ->
        ParameterType type = new ParameterType(
                name: JobParameterKeys.JOB_ID_LIST, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT
        )
        type.save()
        ParameterType realmOutputType = new ParameterType(
                name: JobParameterKeys.REALM,
                jobDefinition: jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT,
                className: "de.dkfz.tbi.otp.ngsdata.Realm"
        )
        realmOutputType.save()
        JobDefinition watchdogJobDefinition = new JobDefinition(
                name: "__WatchdogFor__" + jobDefinition.name, bean: watchdogBean, plan: jep, previous: jobDefinition
        )
        watchdogJobDefinition.save()
        ParameterType inputType = new ParameterType(
                name: JobParameterKeys.JOB_ID_LIST, jobDefinition: watchdogJobDefinition, parameterUsage: ParameterUsage.INPUT
        )
        inputType.save()
        ParameterType realmInputType = new ParameterType(
                name: JobParameterKeys.REALM,
                jobDefinition: watchdogJobDefinition,
                parameterUsage: ParameterUsage.INPUT,
                className: "de.dkfz.tbi.otp.ngsdata.Realm"
        )
        realmInputType.save()
        ParameterMapping mapping = new ParameterMapping(from: type, to: inputType, job: watchdogJobDefinition)
        mapping.save()
        ParameterMapping realmMapping = new ParameterMapping(from: realmOutputType, to: realmInputType, job: watchdogJobDefinition)
        realmMapping.save()
        helper.watchdogJobDefinition = watchdogJobDefinition
    }

    private static Closure jobClosure = { JobExecutionPlan jep, Helper helper, String jobName, String bean, closure = null ->
        log.debug("In job Closure with " + jobName)
        JobDefinition jobDefinition = new JobDefinition(name: jobName, bean: bean, plan: jep, previous: helper.previous)
        jobDefinition.save()
        helper.firstJob = helper.firstJob ?: jobDefinition
        if (helper.previous) {
            helper.previous.next = jobDefinition
            helper.previous.save()
        }
        if (closure) {
            closure.metaClass = new ExpandoMetaClass(closure.class)
            closure.metaClass.constantParameter = { String typeName, String value ->
                JobExecutionPlanDSL.constantParameterClosure(jobDefinition, typeName, value)
            }
            closure.metaClass.outputParameter = { String typeName ->
                JobExecutionPlanDSL.outputParameterClosure(jobDefinition, typeName)
            }
            closure.metaClass.inputParameter = { String typeName, String fromJob, String fromParameter ->
                JobExecutionPlanDSL.inputParameterClosure(jobDefinition, helper.previous, jep, typeName, fromJob, fromParameter)
            }
            // TODO: in future have a generic watchdog which obsoletes the watchdogBean
            closure.metaClass.watchdog = { String watchdogBean ->
                JobExecutionPlanDSL.watchdogClosure(jobDefinition, jep, helper, watchdogBean)
            }
            closure.metaClass.initialize()
            closure()
        }
        helper.previous = jobDefinition
    }

    private static Closure validatingJobClosure = { JobExecutionPlan jep,
                                                    Helper helper,
                                                    String jobName,
                                                    String bean,
                                                    String validatorForName,
                                                    closure = null ->
        JobDefinition validatorFor = JobDefinition.findByNameAndPlan(validatorForName, jep)
        assert(validatorFor)
        ValidatingJobDefinition jobDefinition = new ValidatingJobDefinition(
                name: jobName, bean: bean, plan: jep, previous: helper.previous, validatorFor: validatorFor
        )
        jobDefinition.save()
        helper.firstJob = helper.firstJob ?: jobDefinition
        if (helper.previous) {
            helper.previous.next = jobDefinition
            helper.previous.save()
        }
        if (closure) {
            closure.metaClass = new ExpandoMetaClass(closure.class)
            closure.metaClass.constantParameter = { String typeName, String value ->
                JobExecutionPlanDSL.constantParameterClosure(jobDefinition, typeName, value)
            }
            closure.metaClass.outputParameter = { String typeName ->
                JobExecutionPlanDSL.outputParameterClosure(jobDefinition, typeName)
            }
            closure.metaClass.inputParameter = { String typeName, String fromJob, String fromParameter ->
                JobExecutionPlanDSL.inputParameterClosure(jobDefinition, helper.previous, jep, typeName, fromJob, fromParameter)
            }
            // TODO: in future have a generic watchdog which obsoletes the watchdogBean
            closure.metaClass.watchdog = { String watchdogBean ->
                JobExecutionPlanDSL.watchdogClosure(jobDefinition, jep, helper, watchdogBean)
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
        plan?.save()
        int version = plan ? plan.planVersion + 1 : 0

        // create the new/updated plan
        JobExecutionPlan.withTransaction {
            JobExecutionPlan jep = new JobExecutionPlan(name: name, planVersion: version, enabled: true, previousPlan: plan)
            jep.save()
            Helper helper = new Helper()
            Boolean startJobDefined = false

            // need to create our own meta class to be able to add properties
            c.metaClass = new ExpandoMetaClass(c.class)
            c.metaClass.start = { String n, String bean, closure = null ->
                JobExecutionPlanDSL.startJobClosure(jep, startJobDefined, n, bean, closure)
            }
            c.metaClass.job = { String n, String bean, closure = null ->
                JobExecutionPlanDSL.jobClosure(jep, helper, n, bean, closure)
            }
            c.metaClass.validatingJob = { String jobName, String bean, String validatorForName, closure = null ->
                JobExecutionPlanDSL.validatingJobClosure(jep, helper, jobName, bean, validatorForName, closure)
            }
            c.metaClass.initialize()
            c()
            jep.firstJob = helper.firstJob
            jep.save()
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
