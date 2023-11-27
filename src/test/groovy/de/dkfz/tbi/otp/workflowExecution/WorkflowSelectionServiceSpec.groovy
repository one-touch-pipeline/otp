/*
 * Copyright 2011-2022 The OTP authors
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

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
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

    void "createOrUpdate, should update or create workflowVersionSelector and referenceGenomeSelector and return the results"() {
        given:
        service.referenceGenomeSelectorService = Mock(ReferenceGenomeSelectorService)
        service.workflowVersionSelectorService = Mock(WorkflowVersionSelectorService)
        ReferenceGenomeSelector rgSelector = createReferenceGenomeSelector()
        WorkflowVersionSelector wvSelector = createWorkflowVersionSelector()

        Project project = createProject()
        SeqType seqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion()
        SpeciesWithStrain species = createSpeciesWithStrain()
        Workflow workflow = createWorkflow()
        ReferenceGenome refGenome = createReferenceGenome()

        when:
        WorkflowSelectionSelectorDTO result = service.createOrUpdate(project, seqType, version, [species], workflow, refGenome)

        then:
        result.wvSelector == wvSelector
        result.rgSelector == rgSelector

        and:
        1 * service.referenceGenomeSelectorService.createOrUpdate(project, seqType, [species], workflow, refGenome) >> rgSelector
        1 * service.workflowVersionSelectorService.createOrUpdate(project, seqType, version) >> wvSelector
    }

    void "deleteAndDeprecateSelectors, should call methods to delete workflowVersionSelector and referenceGenomeSelector"() {
        given:
        service.referenceGenomeSelectorService = Mock(ReferenceGenomeSelectorService)
        service.workflowVersionSelectorService = Mock(WorkflowVersionSelectorService)
        ReferenceGenomeSelector rgSelector = createReferenceGenomeSelector()
        WorkflowVersionSelector wvSelector = createWorkflowVersionSelector()

        when:
        service.deleteAndDeprecateSelectors(wvSelector, rgSelector)

        then:
        1 * service.referenceGenomeSelectorService.deleteSelector(rgSelector)
        1 * service.workflowVersionSelectorService.deprecateSelectorIfUnused(wvSelector)
    }
}
