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
package de.dkfz.tbi.otp.job.plan

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.utils.CollectionUtils

@Secured("hasRole('ROLE_ADMIN')")
class JobErrorDefinitionController implements CheckAndCall {

    JobErrorDefinitionService jobErrorDefinitionService

    def index() {
        Map jobErrorDefinitions = jobErrorDefinitionService.allJobErrorDefinition
        List<JobDefinition> jobDefinitions = jobErrorDefinitionService.getJobDefinition(jobErrorDefinitions)
        JobDefinition jobDefinition = CollectionUtils.atMostOneElement(JobDefinition.findAllByName(params.job)) ?: null
        [
                jobErrorDefinition: jobErrorDefinitions.findAll {
                    ((JobErrorDefinition) it.key).jobDefinitions*.name.contains(jobDefinition?.name)
                } ?: jobErrorDefinitions,
                jobDefinitions    : ["No filter"] + jobDefinitions*.name.unique().sort(),
                jobDefinition     : jobDefinition?.name,
                typeDropDown      : JobErrorDefinition.Type,
                actionDropDown    : JobErrorDefinition.Action,
                allJobDefinition  : allJobDefinitions,
        ]
    }

    JSON addJobErrorDefinition(UpdateJobErrorDefinitionCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            jobErrorDefinitionService.addErrorExpression(cmd.typeSelect, cmd.actionSelect, cmd.errorExpression, cmd.basedJobErrorDefinition)
        }
    }

    JSON addNewJobErrorDefinition(UpdateNewJobErrorDefinitionCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            jobErrorDefinitionService.addErrorExpressionFirstLevel(JobErrorDefinition.Type.MESSAGE, cmd.actionSelect, cmd.errorExpression)
        }
    }

    JSON updateErrorExpression(UpdateErrorExpressionCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            jobErrorDefinitionService.updateErrorExpression(cmd.jobErrorDefinition, cmd.errorExpression)
        }
    }

    JSON addNewJob(UpdateAddNewJobCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            jobErrorDefinitionService.addNewJob(cmd.jobErrorDefinition, cmd.jobDefinition)
        }
    }

    private List getAllJobDefinitions() {
        List allJobDefinition = JobDefinition.findAll()
        List jobNames = []

        allJobDefinition.each {
            if (!it.plan.obsoleted && it.class != StartJobDefinition) {
                jobNames.add("${it.name} - ${it.plan.name}")
            }
        }
        return jobNames.sort()
    }
}

class UpdateNewJobErrorDefinitionCommand implements Validateable {
    String errorExpression
    String actionSelect
}

class UpdateJobErrorDefinitionCommand implements Validateable {
    String level
    String errorExpression
    String typeSelect
    String actionSelect
    JobErrorDefinition basedJobErrorDefinition
}

class UpdateErrorExpressionCommand implements Validateable {
    String errorExpression
    JobErrorDefinition jobErrorDefinition

    void setValue(String value) {
        this.errorExpression = value
    }
}

class UpdateAddNewJobCommand implements Validateable {
    JobErrorDefinition jobErrorDefinition
    JobDefinition jobDefinition

    void setJobDefinitionString(String jobDefinitionString) {
        String jobDefinitionName = jobDefinitionString.substring(0, jobDefinitionString.indexOf('-') - 1)
        String jobExecutionPlanName = jobDefinitionString.substring(jobDefinitionString.indexOf('-') + 2)

        this.jobDefinition = CollectionUtils.atMostOneElement(JobDefinition.findAllByNameAndPlan(jobDefinitionName,
                CollectionUtils.atMostOneElement(JobExecutionPlan.findAllByNameAndObsoleted(jobExecutionPlanName, false))))
    }
}
