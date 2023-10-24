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
import de.dkfz.tbi.otp.ngsdata.taxonomy.*
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ReferenceGenomeSelectorServiceSpec extends Specification implements ServiceUnitTest<ReferenceGenomeSelectorService>, DataTest, DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                ReferenceGenomeSelector,
                SeqType,
                Workflow,
                ReferenceGenome,
                Species,
                SpeciesCommonName,
        ]
    }

    void "test createOrUpdate, if no selector exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        Set<SpeciesWithStrain> species = [createSpeciesWithStrain()] as Set
        Workflow workflow = createWorkflow()
        ReferenceGenome referenceGenome = createReferenceGenome(species: [] as Set, speciesWithStrain: species)

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

    void "test getMappingOfSpeciesCombinationsToReferenceGenomes"() {
        given:
        SpeciesWithStrain s1 = createSpeciesWithStrain()
        SpeciesWithStrain s2 = createSpeciesWithStrain()
        SpeciesWithStrain s3 = createSpeciesWithStrain()
        SpeciesWithStrain s4 = createSpeciesWithStrain()
        SpeciesWithStrain s5 = createSpeciesWithStrain()
        SpeciesWithStrain s6 = createSpeciesWithStrain()
        SpeciesWithStrain s7 = createSpeciesWithStrain()
        SpeciesWithStrain s8 = createSpeciesWithStrain()
        SpeciesWithStrain s81 = createSpeciesWithStrain()

        Species sp = createSpecies()
        SpeciesWithStrain s9 = createSpeciesWithStrain(species: sp)
        SpeciesWithStrain s0 = createSpeciesWithStrain(species: sp)

        Species sp2 = createSpecies()
        SpeciesWithStrain s11 = createSpeciesWithStrain(species: sp2)
        createSpeciesWithStrain(species: sp2)

        Species sp3 = createSpecies()
        SpeciesWithStrain s13 = createSpeciesWithStrain(species: sp3)
        SpeciesWithStrain s14 = createSpeciesWithStrain(species: sp3)

        Project project = createProject(speciesWithStrains: [s2, s3, s4, s5, s6, s7, s8, s9, s0, s11] as Set)

        createReferenceGenome(species: [s1.species] as Set, speciesWithStrain: [] as Set)
        ReferenceGenome r2 = createReferenceGenome(species: [s3.species] as Set, speciesWithStrain: [] as Set)
        ReferenceGenome r3 = createReferenceGenome(species: [] as Set, speciesWithStrain: [s4] as Set)
        ReferenceGenome r31 = createReferenceGenome(species: [] as Set, speciesWithStrain: [s4] as Set)
        ReferenceGenome r4 = createReferenceGenome(species: [s5.species] as Set, speciesWithStrain: [s6] as Set)
        ReferenceGenome r5 = createReferenceGenome(species: [s7.species] as Set, speciesWithStrain: [s8] as Set)
        ReferenceGenome r6 = createReferenceGenome(species: [] as Set, speciesWithStrain: [s8, s81] as Set)
        ReferenceGenome r7 = createReferenceGenome(species: [sp] as Set, speciesWithStrain: [s8] as Set)
        ReferenceGenome r8 = createReferenceGenome(species: [sp2] as Set, speciesWithStrain: [] as Set)
        ReferenceGenome r9 = createReferenceGenome(species: [sp3] as Set, speciesWithStrain: [s3] as Set)

        expect:
        service.getMappingOfSpeciesCombinationsToReferenceGenomes(project) == [
                ([s3] as Set)     : [r2],
                ([s4] as Set)     : [r3, r31],
                ([s5, s6] as Set) : [r4],
                ([s7, s8] as Set) : [r5],
                ([s8, s81] as Set): [r6],

                ([s8, s9] as Set) : [r7],
                ([s8, s0] as Set) : [r7],

                ([s11] as Set)    : [r8],

                ([s13, s3] as Set): [r9],
                ([s14, s3] as Set): [r9],
        ]
    }

    void "test hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies, when species is known to OTP, then returns true"() {
        given:
        List<String> speciesStringList = ['human', 'mouse']
        List<SpeciesWithStrain> speciesWithStrainList = []
        speciesStringList.unique().each { String str ->
            SpeciesCommonName speciesCommonName = createSpeciesCommonName(name: str)
            SpeciesWithStrain speciesWithStrain = createSpeciesWithStrain([species: createSpecies([speciesCommonName: speciesCommonName])])
            speciesWithStrainList.add(speciesWithStrain)
        }
        Project project1 = createProject()
        SeqType seqType1 = createSeqTypePaired()
        ReferenceGenome referenceGenome1 = createReferenceGenome(species: [] as Set, speciesWithStrain: speciesWithStrainList as Set)
        Workflow workflow = createWorkflow()

        and:
        ReferenceGenomeSelector referenceGenomeSelector = createReferenceGenomeSelector([
                project        : project1,
                seqType        : seqType1,
                referenceGenome: referenceGenome1,
                workflow       : workflow,
        ])

        and:
        Map<String, OtpWorkflow> alignmentWorkflows = [(referenceGenomeSelector.workflow.beanName): Mock(OtpWorkflow)]
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupAlignableOtpWorkflowBeans() >> alignmentWorkflows
        }

        expect:
        service.hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies(project1, seqType1, speciesWithStrainList)
    }

    void "test hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies, when species is not known to OTP, then returns false"() {
        given:
        List<SpeciesWithStrain> speciesWithStrainList = []
        Project project1 = createProject()
        SeqType seqType1 = createSeqTypePaired()
        createWorkflow()

        expect:
        !service.hasReferenceGenomeConfigForProjectAndSeqTypeAndSpecies(project1, seqType1, speciesWithStrainList)
    }
}
