/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project

class WorkflowSelectionServiceSpec extends Specification implements ServiceUnitTest<WorkflowSelectionService>, WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                ReferenceGenomeSelector,
                WorkflowVersionSelector,
                Project,
                ProcessingPriority,
        ]
    }

    @Unroll()
    void "deleteAndDeprecateSelectors, #should, when refGenomeSelector is #rgSelectorDefined and workflowVersionSelector #wvSelectorDefined"() {
        given:
        service.referenceGenomeSelectorService = Mock(ReferenceGenomeSelectorService)
        service.workflowVersionSelectorService = Mock(WorkflowVersionSelectorService)
        ReferenceGenomeSelector rgSelector = rgSelectorDefined ? createReferenceGenomeSelector() : null
        WorkflowVersionSelector wvSelector = wvSelectorDefined ? createWorkflowVersionSelector() : null

        when:
        service.deleteAndDeprecateSelectors(wvSelector, rgSelector)

        then:
        (rgSelectorDefined ? 1 : 0) * service.referenceGenomeSelectorService.deleteSelector(rgSelector)
        (wvSelectorDefined ? 1 : 0) * service.workflowVersionSelectorService.deprecateSelectorIfUnused(wvSelector)

        where:
        should                                                                                   | rgSelectorDefined | wvSelectorDefined
        'should only call methods to delete workflowVersionSelector and referenceGenomeSelector' | 'defined'         | 'defined'
        'should only call method to delete workflowVersionSelector'                              | null              | 'defined'
        'should only call method to delete referenceGenomeSelector'                              | 'defined'         | null
    }
}
