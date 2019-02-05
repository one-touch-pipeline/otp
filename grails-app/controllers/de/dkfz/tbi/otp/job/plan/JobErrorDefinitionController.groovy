package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.CheckAndCall
import grails.converters.*

class JobErrorDefinitionController implements CheckAndCall {

    JobErrorDefinitionService jobErrorDefinitionService

    def index() {
        Map jobErrorDefinitions = jobErrorDefinitionService.getAllJobErrorDefinition()
        List<JobDefinition> jobDefinitions = jobErrorDefinitionService.getJobDefinition(jobErrorDefinitions)
        JobDefinition jobDefinition = JobDefinition.findByName(params.job) ?: null
        [
                jobErrorDefinition: jobErrorDefinitions.findAll {
                    ((JobErrorDefinition) it.key).jobDefinitions*.name.contains(jobDefinition?.name)
                } ?: jobErrorDefinitions,
                jobDefinitions    : ["No filter"] + jobDefinitions*.name.unique().sort(),
                jobDefinition     : jobDefinition?.name,
                typeDropDown      : JobErrorDefinition.Type,
                actionDropDown    : JobErrorDefinition.Action,
                allJobDefinition  : getAllJobDefinitions(),
        ]
    }

    JSON addJobErrorDefinition(UpdateJobErrorDefinitionCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            jobErrorDefinitionService.addErrorExpression(cmd.typeSelect, cmd.actionSelect, cmd.errorExpression, cmd.basedJobErrorDefinition)
        })
    }

    JSON addNewJobErrorDefinition(UpdateNewJobErrorDefinitionCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            jobErrorDefinitionService.addErrorExpressionFirstLevel(JobErrorDefinition.Type.MESSAGE, cmd.actionSelect, cmd.errorExpression)
        })
    }

    JSON updateErrorExpression(UpdateErrorExpressionCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            jobErrorDefinitionService.updateErrorExpression(cmd.jobErrorDefinition, cmd.errorExpression)
        })
    }

    JSON addNewJob(UpdateAddNewJobCommand cmd) {
        checkErrorAndCallMethod(cmd, { jobErrorDefinitionService.addNewJob(cmd.jobErrorDefinition, cmd.jobDefinition) })
    }

    private List getAllJobDefinitions() {
        List allJobDefinition = JobDefinition.findAll()
        List jobNames = []

        allJobDefinition.each {
            if (!it.plan.obsoleted && it.getClass() != StartJobDefinition) {
                jobNames.add("${it.name} - ${it.plan.name}")
            }
        }
        return jobNames.sort()
    }
}

class UpdateNewJobErrorDefinitionCommand implements Serializable {
    String errorExpression
    String actionSelect
}

class UpdateJobErrorDefinitionCommand implements Serializable {
    String level
    String errorExpression
    String typeSelect
    String actionSelect
    JobErrorDefinition basedJobErrorDefinition
}

class UpdateErrorExpressionCommand implements Serializable {
    String errorExpression
    JobErrorDefinition jobErrorDefinition

    void setValue(String value) {
        this.errorExpression = value
    }
}

class UpdateAddNewJobCommand implements Serializable {
    JobErrorDefinition jobErrorDefinition
    JobDefinition jobDefinition

    void setJobDefinitionString(String jobDefinitionString) {
        String jobDefinitionName = jobDefinitionString.substring(0, jobDefinitionString.indexOf('-') - 1)
        String jobExecutionPlanName = jobDefinitionString.substring(jobDefinitionString.indexOf('-') + 2)

        this.jobDefinition = JobDefinition.findByNameAndPlan(jobDefinitionName, JobExecutionPlan.findByNameAndObsoleted(jobExecutionPlanName, false))
    }
}
