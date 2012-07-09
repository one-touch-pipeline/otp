package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.plan.ValidatingJobDefinition
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

class Helper {
    JobDefinition firstJob = null
    JobDefinition previous = null
    JobDefinition watchdogJobDefinition = null
}

class JobExecutionPlanDSL {

    private static def constantParameterClosure = { JobDefinition jobDefinition, String typeName, String value ->
        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assert(type.save())
        Parameter parameter = new Parameter(type: type, value: value)
        assert(parameter.save())
        jobDefinition.addToConstantParameters(parameter)
        assert(jobDefinition.save())
    }

    private static def outputParameterClosure = { JobDefinition jobDefinition, String typeName ->
        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assert(type.save())
    }

    private static def inputParameterClosure = { JobDefinition jobDefinition, JobDefinition previous, JobExecutionPlan jep, String typeName, String fromJob, String fromParameter ->
        ParameterType inputType = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assert(inputType.save())
        if (!previous) {
            // first job - get it from StartJob
            ParameterType outputType = ParameterType.findByJobDefinitionAndName(jep.startJob, fromParameter)
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            ParameterMapping mapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            assert(mapping.save())
        } else if (previous.name == fromJob) {
            // simple case: direct mapping
            ParameterType outputType = ParameterType.findByJobDefinitionAndName(previous, fromParameter)
            assert(outputType)
            assert(outputType.parameterUsage == ParameterUsage.OUTPUT)
            ParameterMapping mapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            assert(mapping.save())
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
                    passThroughType = new ParameterType(name: fromParameter + "Passthrough", jobDefinition: mappingJob.next ? mappingJob.next : firstJob, parameterUsage: ParameterUsage.PASSTHROUGH)
                    assert(passThroughType.save())
                }
                ParameterMapping mapping = ParameterMapping.findByFromAndTo(outputType, passThroughType)
                if (!mapping) {
                    mapping = new ParameterMapping(from: outputType, to: passThroughType, job: mappingJob.next ? mappingJob.next : firstJob)
                    assert(mapping.save())
                }
                // next run
                outputType = passThroughType
                mappingJob = mappingJob.next
                if (!mappingJob) {
                    mappingJob = firstJob
                }
            }
            ParameterMapping finalMapping = new ParameterMapping(from: outputType, to: inputType, job: jobDefinition)
            assert(finalMapping.save())
        }
    }

    private static def startJobClosure = { JobExecutionPlan jep, Boolean startJobDefined, String startName, String bean, closure = null ->
        assert !startJobDefined, "Only one Start Job Definition can be defined per Job Execution Plan"
        StartJobDefinition startJobDefinition = new StartJobDefinition(name: startName, bean: bean, plan: jep)
        assert(startJobDefinition.save())
        jep.startJob = startJobDefinition
        assert(jep.save())
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

    private static def watchdogClosure = { JobDefinition jobDefinition, JobExecutionPlan jep, Helper helper, String watchdogBean ->
        ParameterType type = new ParameterType(name: "__pbsIds", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assert(type.save())
        ParameterType realmOutputType = new ParameterType(name: "__pbsRealm", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT, className: "de.dkfz.tbi.otp.ngsdata.Realm")
        assert(realmOutputType.save())
        JobDefinition watchdogJobDefinition = new JobDefinition(name: "__WatchdogFor__" + jobDefinition.name, bean: watchdogBean, plan: jep, previous: jobDefinition)
        assert(watchdogJobDefinition.save())
        ParameterType inputType = new ParameterType(name: "__pbsIds", jobDefinition: watchdogJobDefinition, parameterUsage: ParameterUsage.INPUT)
        assert(inputType.save())
        ParameterType realmInputType = new ParameterType(name: "__pbsRealm", jobDefinition: watchdogJobDefinition, parameterUsage: ParameterUsage.INPUT, className: "de.dkfz.tbi.otp.ngsdata.Realm")
        assert(realmInputType.save())
        ParameterMapping mapping = new ParameterMapping(from: type, to: inputType, job: watchdogJobDefinition)
        assert(mapping.save())
        ParameterMapping realmMapping = new ParameterMapping(from: realmOutputType, to: realmInputType, job: watchdogJobDefinition)
        assert(realmMapping.save())
        helper.watchdogJobDefinition = watchdogJobDefinition
    }

    private static def jobClosure = { JobExecutionPlan jep, Helper helper, String jobName, String bean, closure = null ->
        println "In job Closure with " + jobName
        JobDefinition jobDefinition = new JobDefinition(name: jobName, bean: bean, plan: jep, previous: helper.previous)
        assert(jobDefinition.save())
        if (!helper.firstJob) {
            helper.firstJob = jobDefinition
        }
        if (helper.previous) {
            helper.previous.next = jobDefinition
            assert(helper.previous.save())
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

    private static def pbsJobClosure = { JobExecutionPlan jep, Helper helper, String jobName, String bean, String realmId, closure = null ->
        JobExecutionPlanDSL.jobClosure(jep, helper, jobName, bean, closure)
        ParameterType realmInputType = new ParameterType(name: "__pbsRealm", jobDefinition: helper.previous,  parameterUsage: ParameterUsage.INPUT, className: "de.dkfz.tbi.otp.ngsdata.Realm")
        assert(realmInputType.save())
        Parameter parameter = new Parameter(type: realmInputType, value: realmId)
        assert(parameter.save())
        helper.previous.addToConstantParameters(parameter)
        if (helper.watchdogJobDefinition) {
            helper.previous = helper.watchdogJobDefinition
            helper.watchdogJobDefinition = null
        }
    }

    private static def validatingJobClosure = { JobExecutionPlan jep, Helper helper, String jobName, String bean, String validatorForName, closure = null ->
        JobDefinition validatorFor = JobDefinition.findByNameAndPlan(validatorForName, jep)
        assert(validatorFor)
        ValidatingJobDefinition jobDefinition = new ValidatingJobDefinition(name: jobName, bean: bean, plan: jep, previous: helper.previous, validatorFor: validatorFor)
        assert(jobDefinition.save())
        if (!helper.firstJob) {
            helper.firstJob = jobDefinition
        }
        if (helper.previous) {
            helper.previous.next = jobDefinition
            assert(helper.previous.save())
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

    public static def plan = { String name, def ctx = null, boolean validate = false, c ->
        JobExecutionPlan.withTransaction {
            JobExecutionPlan jep = new JobExecutionPlan(name: name, planVersion: 0, enabled: true)
            assert(jep.save())
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
            c.metaClass.pbsJob = { String jobName, String bean, String realmId, closure = null ->
                JobExecutionPlanDSL.pbsJobClosure(jep, helper, jobName, bean, realmId, closure)
            }
            c.metaClass.validatingJob = { String jobName, String bean, String validatorForName, closure = null ->
                JobExecutionPlanDSL.validatingJobClosure(jep, helper, jobName, bean, validatorForName, closure)
            }
            c.metaClass.initialize()
            c()
            jep.firstJob = helper.firstJob
            assert(jep.save(flush: true))
            if (validate && ctx) {
                List<String> errors = ctx.planValidatorService.validate(jep)
                if (!errors.isEmpty()) {
                    println("Errors found during validation")
                    errors.each { println(it) }
                }
                assert(errors.isEmpty())
            }
        }
        println("Plan created")
    }
}
