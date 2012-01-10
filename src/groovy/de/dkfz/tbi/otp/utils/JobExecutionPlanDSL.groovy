package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage

class JobExecutionPlanDSL {
    public static def plan = { String name, c ->
        JobExecutionPlan.withTransaction {
            JobExecutionPlan jep = new JobExecutionPlan(name: name, planVersion: 0, enabled: true)
            assert(jep.save())
            JobDefinition firstJob = null
            JobDefinition previous = null
            c.start = { String startName, String bean, closure = null ->
                StartJobDefinition startJobDefinition = new StartJobDefinition(name: startName, bean: bean, plan: jep)
                assert(startJobDefinition.save())
                jep.startJob = startJobDefinition
                assert(jep.save())
                if (closure) {
                    closure.constantParameter = { String typeName, String value ->
                        ParameterType type = new ParameterType(name: typeName, jobDefinition: startJobDefinition, parameterUsage: ParameterUsage.INPUT)
                        assert(type.save())
                        Parameter parameter = new Parameter(type: type, value: value)
                        assert(parameter.save())
                        startJobDefinition.addToConstantParameters(parameter)
                        assert(startJobDefinition.save())
                    }
                    closure.outputParameter = { String typeName ->
                        ParameterType type = new ParameterType(name: typeName, jobDefinition: startJobDefinition, parameterUsage: ParameterUsage.OUTPUT)
                        assert(type.save())
                    }
                    closure()
                }
            }
            c.job = { String jobName, String bean, closure = null ->
                JobDefinition jobDefinition = new JobDefinition(name: jobName, bean: bean, plan: jep, previous: previous)
                assert(jobDefinition.save())
                if (!firstJob) {
                    firstJob = jobDefinition
                }
                if (previous) {
                    previous.next = jobDefinition
                    assert(previous.save())
                }
                if (closure) {
                    closure.constantParameter = { String typeName, String value ->
                        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
                        assert(type.save())
                        Parameter parameter = new Parameter(type: type, value: value)
                        assert(parameter.save())
                        jobDefinition.addToConstantParameters(parameter)
                        assert(jobDefinition.save())
                    }
                    closure.outputParameter = { String typeName ->
                        ParameterType type = new ParameterType(name: typeName, jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
                        assert(type.save())
                    }
                    closure.inputParameter = { String typeName, String fromJob, String fromParameter ->
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
                    closure()
                }
                previous = jobDefinition
            }
            c()
            jep.firstJob = firstJob
            assert(jep.save(flush: true))
        }
        println("Plan created")
    }
}
