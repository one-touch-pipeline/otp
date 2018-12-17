package de.dkfz.tbi.otp.job.plan

import grails.plugin.springsecurity.acl.AclUtilService
import org.springframework.security.access.prepost.PreAuthorize

class JobErrorDefinitionService {

    AclUtilService aclUtilService

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Map<Object, Object> getAllJobErrorDefinition() {
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
        } else {
            Map newValueMap = [:]
            value.checkFurtherJobErrors.each { JobErrorDefinition newValue ->
                Map jed = getValues(newValue)
                newValueMap.putAll(jed)
            }
            return [(value): newValueMap]
        }
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
