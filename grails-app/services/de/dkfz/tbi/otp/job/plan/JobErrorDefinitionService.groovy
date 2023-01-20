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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class JobErrorDefinitionService {

    JobDefinition findByName(String name) {
        return CollectionUtils.atMostOneElement(JobDefinition.findAllByName(name)) ?: null
    }

    List<JobDefinition> findAll() {
        return JobDefinition.findAll()
    }

    JobDefinition findByjobDefinitionNameAndjobExecutionPlanName(String jobDefinitionName, String jobExecutionPlanName) {
        return CollectionUtils.atMostOneElement(JobDefinition.findAllByNameAndPlan(jobDefinitionName,
                CollectionUtils.atMostOneElement(JobExecutionPlan.findAllByNameAndObsoleted(jobExecutionPlanName, false))))
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Map<Object, Object> allJobErrorDefinition() {
        List<JobErrorDefinition> jobErrorDefinitionList = JobErrorDefinition.list()

        Map jobErrorDefinition = [:]
        jobErrorDefinitionList.each { JobErrorDefinition value ->
            Map jed = getValues(value)
            jobErrorDefinition.putAll(jed)
        }
        return findDuplicates(jobErrorDefinition)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List getJobDefinition(Map jobErrorDefinitionMap) {
        List jobErrorDefinitions = []
        jobErrorDefinitionMap.each { key, value ->
            JobErrorDefinition jobErrorDefinitionObject = (JobErrorDefinition) key
            jobErrorDefinitionObject.jobDefinitions.each { JobDefinition jobDefinition ->
                jobErrorDefinitions << jobDefinition
            }
        }
        jobErrorDefinitions.unique().sort()
        return jobErrorDefinitions
    }

    Map<Object, Object> getValues(JobErrorDefinition value) {
        if (value.action != JobErrorDefinition.Action.CHECK_FURTHER) {
            return [(value): value.errorExpression]
        }
        Map newValueMap = [:]
        value.checkFurtherJobErrors.each { JobErrorDefinition newValue ->
            Map jed = getValues(newValue)
            newValueMap.putAll(jed)
        }
        return [(value): newValueMap]
    }

    Map<Object, Object> findDuplicates(Map jobErrorDefinition) {
        List list = []
        jobErrorDefinition.each { key, value ->
            if (value instanceof Map) {
                value.each { k, v ->
                    list.add(k)
                }
            }
        }
        list.each { JobErrorDefinition listObject ->
            jobErrorDefinition.remove(listObject)
        }
        return jobErrorDefinition
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addErrorExpressionFirstLevel(JobErrorDefinition.Type type, String action, String errorExpression) {
        JobErrorDefinition newJobErrorDefinition = new JobErrorDefinition(
                type: type,
                action: action,
                errorExpression: errorExpression,
        )
        newJobErrorDefinition.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addErrorExpression(String type, String action, String errorExpression, JobErrorDefinition jobErrorDefinition) {
        JobErrorDefinition newJobErrorDefinition = new JobErrorDefinition(
                type: type,
                action: action,
                errorExpression: errorExpression,
        )
        newJobErrorDefinition.save(flush: true)
        jobErrorDefinition.checkFurtherJobErrors.add(newJobErrorDefinition)
        jobErrorDefinition.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void updateErrorExpression(JobErrorDefinition jobErrorDefinition, String errorExpression) {
        jobErrorDefinition.errorExpression = errorExpression
        jobErrorDefinition.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addNewJob(JobErrorDefinition jobErrorDefinition, JobDefinition jobDefinition) {
        jobErrorDefinition.jobDefinitions.add(jobDefinition)
        jobErrorDefinition.save(flush: true)
    }
}
