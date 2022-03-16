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

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ReferenceGenomeSelectorServiceSpec extends Specification implements ServiceUnitTest<ReferenceGenomeSelectorService>, DataTest, DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenomeSelector,
                SeqType,
                Workflow,
        ]
    }

    void "test createOrUpdate, if no selector exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        Set<SpeciesWithStrain> species = [createSpeciesWithStrain()] as Set
        Workflow workflow = createWorkflow()
        ReferenceGenome referenceGenome = createReferenceGenome(species: species)

        when:
        service.createOrUpdate(project, seqType, species, workflow, referenceGenome)

        then:
        ReferenceGenomeSelector selector = exactlyOneElement(ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(project, seqType, workflow)
                .findAll { it.species == species })
        selector.referenceGenome == referenceGenome
    }

    void "test createOrUpdate, if a matching selector exists"() {
        given:
        ReferenceGenomeSelector existing = createReferenceGenomeSelector()
        ReferenceGenome referenceGenome = createReferenceGenome()

        when:
        service.createOrUpdate(existing.project, existing.seqType, existing.species, existing.workflow, referenceGenome)

        then:
        ReferenceGenomeSelector selector = exactlyOneElement(
                ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(existing.project, existing.seqType, existing.workflow)
                        .findAll { it.species == existing.species })
        selector == existing
        selector.referenceGenome == referenceGenome
    }
}
