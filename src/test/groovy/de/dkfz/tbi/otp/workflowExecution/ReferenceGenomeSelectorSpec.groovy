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
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

class ReferenceGenomeSelectorSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ReferenceGenomeSelector,
                Project,
                Workflow,
        ]
    }

    void "validator, should prevent creating ReferenceGenomeSelectors with same workflow, project, seqType and species"() {
        given:
        ReferenceGenomeSelector referenceGenomeSelector = createReferenceGenomeSelector()

        when:
        createReferenceGenomeSelector([
                project : referenceGenomeSelector.project,
                workflow: referenceGenomeSelector.workflow,
                seqType : referenceGenomeSelector.seqType,
                species : referenceGenomeSelector.species,
        ])

        then:
        thrown(ValidationException)
    }

    void "validator, should allow creating ReferenceGenomeSelectors with different workflow, project, seqType or species"() {
        given:
        SpeciesWithStrain species1 = createSpeciesWithStrain()
        SpeciesWithStrain species2 = createSpeciesWithStrain()
        ReferenceGenomeSelector referenceGenomeSelector = createReferenceGenomeSelector([
                species: [species1, species2],
        ])

        when:
        createReferenceGenomeSelector([
                project : referenceGenomeSelector.project,
                workflow: referenceGenomeSelector.workflow,
                seqType : referenceGenomeSelector.seqType,
        ])
        createReferenceGenomeSelector([
                project : referenceGenomeSelector.project,
                workflow: referenceGenomeSelector.workflow,
                species : referenceGenomeSelector.species,
        ])
        createReferenceGenomeSelector([
                project: referenceGenomeSelector.project,
                seqType: referenceGenomeSelector.seqType,
                species: referenceGenomeSelector.species,
        ])
        createReferenceGenomeSelector([
                workflow: referenceGenomeSelector.workflow,
                seqType : referenceGenomeSelector.seqType,
                species : referenceGenomeSelector.species,
        ])
        createReferenceGenomeSelector([
                project : referenceGenomeSelector.project,
                workflow: referenceGenomeSelector.workflow,
                seqType : referenceGenomeSelector.seqType,
                species : [species1],
        ])

        then:
        notThrown(ValidationException)
    }
}
