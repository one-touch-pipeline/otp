/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import de.dkfz.tbi.otp.project.Project

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class ProcessingPriorityService {

    ProcessingOptionService processingOptionService

    List<ProcessingPriority> allSortedByPriority() {
        return ProcessingPriority.list(sort: 'priority')
    }

    ProcessingPriority defaultPriority() {
        return CollectionUtils.atMostOneElement(
                ProcessingPriority.findAllByName(
                        processingOptionService.findOptionAsString(ProcessingOption.OptionName.PROCESSING_PRIORITY_DEFAULT_NAME)
                )
        )
    }

    //operations on Priority domain object
    ProcessingPriority getPriority(Long id) {
        return ProcessingPriority.get(id)
    }

    int getPriorityListCount() {
        return ProcessingPriority.count()
    }

    List<ProcessingPriority> getPriorityList(Map params) {
        return ProcessingPriority.list(params)
    }

    ProcessingPriority savePriority(ProcessingPriority processingPriority) {
        return processingPriority.save()
    }

    void deletePriority(Long id) {
        ProcessingPriority.get(id).delete()
    }

    Map getReferences(ProcessingPriority processingPriority) {
        Map references = [:]

        //if it is defined in ProcessingOption as default
        String defaultName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.PROCESSING_PRIORITY_DEFAULT_NAME)
        if (processingPriority.name == defaultName) {
            references.put(ProcessingOption.OptionName.PROCESSING_PRIORITY_DEFAULT_NAME, ProcessingOption.class.simpleName)
        }

        //if it is used by WorkflowRun, WorkflowRunStep, Project
        Project.findAllByProcessingPriority(processingPriority).each {
            references.put(it.id, it.class.simpleName)
        }

        WorkflowRun.findAllByPriority(processingPriority).each {
            references.put(it.id, it.class.simpleName)
        }

        return references
    }
}
