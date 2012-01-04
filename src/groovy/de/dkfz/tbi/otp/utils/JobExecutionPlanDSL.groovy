package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition

class JobExecutionPlanDSL {
    public static def plan = { String name, c ->
        JobExecutionPlan.withTransaction {
            JobExecutionPlan jep = new JobExecutionPlan(name: name, planVersion: 0, enabled: true)
            assert(jep.save())
            JobDefinition firstJob = null
            JobDefinition previous = null
            c.start = { String startName, String bean ->
                StartJobDefinition startJobDefinition = new StartJobDefinition(name: startName, bean: bean, plan: jep)
                assert(startJobDefinition.save())
                jep.startJob = startJobDefinition
                assert(jep.save())
            }
            c.job = { String jobName, String bean ->
                JobDefinition jobDefinition = new JobDefinition(name: jobName, bean: bean, plan: jep, previous: previous)
                assert(jobDefinition.save())
                if (!firstJob) {
                    firstJob = jobDefinition
                }
                if (previous) {
                    previous.next = jobDefinition
                    assert(previous.save())
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
