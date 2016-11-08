package de.dkfz.tbi.otp.job.plan

import grails.converters.*

class JobErrorDefinitionController {

    JobErrorDefinitionService jobErrorDefinitionService

    def index() {
        Map jobErrorDefinitions = jobErrorDefinitionService.getAllJobErrorDefinition()
        List<JobDefinition> jobDefinitions = jobErrorDefinitionService.getJobDefinition(jobErrorDefinitions)
        JobDefinition jobDefinition = JobDefinition.findByName(params.job) ?: null
        [
                jobErrorDefinition: jobErrorDefinitions.findAll { ((JobErrorDefinition)it.key).jobDefinitions*.name.contains(jobDefinition?.name) } ?: jobErrorDefinitions,
                jobDefinitions: ["No filter"] + jobDefinitions*.name,
                jobDefinition: jobDefinition?.name,
                typeDropDown: JobErrorDefinition.Type,
                actionDropDown: JobErrorDefinition.Action,
        ]
    }

    JSON addJobErrorDefinition(UpdateJobErrorDefinitionCommand cmd){
        checkErrorAndCallMethod(cmd, { jobErrorDefinitionService.addErrorExpression(cmd.typeSelect, cmd.actionSelect, cmd.errorExpression, cmd.basedJobErrorDefinition) })
    }

    JSON addNewJobErrorDefinition(UpdateNewJobErrorDefinitionCommand cmd){
        checkErrorAndCallMethod(cmd, { jobErrorDefinitionService.addErrorExpressionFirstLevel(JobErrorDefinition.Type.MESSAGE, cmd.actionSelect, cmd.errorExpression) })
    }

    JSON updateErrorExpression(UpdateErrorExpressionCommand cmd){
        checkErrorAndCallMethod(cmd, { jobErrorDefinitionService.updateErrorExpression(cmd.jobErrorDefinition, cmd.errorExpression) })
    }

    JSON addNewJob(UpdateAddNewJobCommand cmd){
        checkErrorAndCallMethod(cmd, { jobErrorDefinitionService.addNewJob(cmd.jobErrorDefinition, cmd.jobDefinition) })
    }

    private void checkErrorAndCallMethod(Serializable cmd, Closure method) {
        Map data
        if (cmd.hasErrors()) {
            data = getErrorData(cmd.errors.getFieldError())
        } else {
            method()
            data = [success: true]
        }
        render data as JSON
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

    void setValue(String value) {
        this.jobDefinition = JobDefinition.findByName(value)
    }
}
