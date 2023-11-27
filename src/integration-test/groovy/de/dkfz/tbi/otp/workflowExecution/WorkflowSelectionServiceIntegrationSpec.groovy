/*
 * Copyright 2011-2021 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.Strain

@Rollback
@Integration
class WorkflowSelectionServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowSelectionService workflowSelectionService
    ReferenceGenomeSelectorService referenceGenomeSelectorService
    WorkflowVersionSelectorService workflowVersionSelectorService
    WorkflowVersionService workflowVersionService
    ReferenceGenomeService referenceGenomeService

    void setup() {
        workflowSelectionService = new WorkflowSelectionService()
        workflowSelectionService.referenceGenomeSelectorService = referenceGenomeSelectorService
        workflowSelectionService.workflowVersionSelectorService = workflowVersionSelectorService
        workflowSelectionService.workflowVersionService = workflowVersionService
        workflowSelectionService.referenceGenomeService = referenceGenomeService
    }

    @Unroll
    @SuppressWarnings("LineLength")
    // suppressed because breaking the line would make the where clause less readable
    void "getPossibleAlignmentOptions, should filter correctly when workflowSelected #workflowSelected and seqType "() {
        given:
        workflowSelectionService.workflowService = Mock(WorkflowService)

        Species species1 = createSpecies()
        Species species2 = createSpecies()
        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain([strain: [name: 'sws1'], species: species1])
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain([strain: [name: 'sws2'], species: species1])
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain([strain: [name: 'sws3']])
        createSpeciesWithStrain([strain: [name: 'sws4'], species: species2])

        Workflow workflow1 = createWorkflow([name: 'w1'])
        Workflow workflow2 = createWorkflow([name: 'w2'])

        ReferenceGenome refGenome1a = createReferenceGenome([name: 'rg1a', species: [] as Set, speciesWithStrain: [speciesWithStrain1] as Set])
        ReferenceGenome refGenome1b = createReferenceGenome([name: 'rg1b', species: [species1] as Set, speciesWithStrain: [speciesWithStrain3] as Set])
        ReferenceGenome refGenome1c = createReferenceGenome([name: 'rg1c', species: [] as Set, speciesWithStrain: [speciesWithStrain2, speciesWithStrain3] as Set])
        ReferenceGenome refGenome2a = createReferenceGenome([name: 'rg2a', species: [species1, species2] as Set, speciesWithStrain: [speciesWithStrain3] as Set])
        ReferenceGenome refGenome2b = createReferenceGenome([name: 'rg2b', species: [species1, species2] as Set, speciesWithStrain: [speciesWithStrain3] as Set])

        SeqType seqType1a = createSeqType([displayName: 's1a'])
        SeqType seqType1b = createSeqType([displayName: 's1b'])
        SeqType seqType1c = createSeqType([displayName: 's1c'])
        SeqType seqType2a = createSeqType([displayName: 's2a'])
        SeqType seqType2b = createSeqType([displayName: 's2b'])

        WorkflowVersion version1a = createWorkflowVersion([
                workflow               : workflow1,
                workflowVersion        : 'v1a',
                allowedReferenceGenomes: [refGenome1a, refGenome1b],
                supportedSeqTypes      : [seqType1a],
        ])
        createWorkflowVersion([
                workflow               : workflow1,
                workflowVersion        : 'v1b',
                allowedReferenceGenomes: [refGenome1b, refGenome1c],
                supportedSeqTypes      : [seqType1a, seqType1b, seqType1c],
        ])
        createWorkflowVersion([
                workflow               : workflow2,
                workflowVersion        : 'v2',
                allowedReferenceGenomes: [refGenome2a, refGenome2b],
                supportedSeqTypes      : [seqType2a, seqType2b],
        ])

        when:
        WorkflowSelectionOptionDTO selectedOption = new WorkflowSelectionOptionDTO([
                workflow       : workflowSelected ? workflow1 : null,
                workflowVersion: versionSelected ? version1a : null,
                seqType        : seqTypeSelected ? seqType1a : null,
                species        : speciesSelected ? [speciesWithStrain1] : null,
                refGenome      : refGenomeSelected ? refGenome2a : null,
        ])
        WorkflowSelectionOptionsDTO result = workflowSelectionService.getPossibleAlignmentOptions(selectedOption)

        then:
        TestCase.assertContainSame(result.workflows*.name, expectedWorkflows)
        TestCase.assertContainSame(result.workflowVersions*.workflowVersion, expectedVersions)
        TestCase.assertContainSame(result.seqTypes*.displayName, expectedSeqTypes)
        TestCase.assertContainSame(result.refGenomes*.name, expectedRefGenomes)
        TestCase.assertContainSame(result.species*.strain*.name, expectedSpecies)

        then:
        (workflowSelected && versionSelected ? 0 : 1) * workflowSelectionService.workflowService.findAllAlignmentWorkflows() >> [workflow1, workflow2]

        where:
        workflowSelected | versionSelected | seqTypeSelected | speciesSelected | refGenomeSelected | expectedWorkflows | expectedVersions     | expectedSeqTypes                    | expectedSpecies                  | expectedRefGenomes
        true             | false           | false           | false           | false             | ['w1', 'w2']      | ['v1a', 'v1b']       | ['s1a', 's1b', 's1c']               | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b', 'rg1c']
        false            | true            | false           | false           | false             | ['w1']            | ['v1a', 'v1b', 'v2'] | ['s1a']                             | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b']
        false            | false           | true            | false           | false             | ['w1']            | ['v1a', 'v1b']       | ['s1a', 's1b', 's1c', 's2a', 's2b'] | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b', 'rg1c']
        false            | false           | false           | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3', 'sws4']         | ['rg1a']
        false            | false           | false           | false           | true              | ['w2']            | ['v2']               | ['s2a', 's2b']                      | ['sws1', 'sws2', 'sws3', 'sws4'] | ['rg1a', 'rg1b', 'rg1c', 'rg2a', 'rg2b']
        true             | true            | false           | false           | false             | ['w1']            | ['v1a', 'v1b']       | ['s1a']                             | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b']
        true             | false           | true            | false           | false             | ['w1']            | ['v1a', 'v1b']       | ['s1a', 's1b', 's1c']               | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b', 'rg1c']
        true             | false           | false           | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
        false            | true            | true            | false           | false             | ['w1']            | ['v1a', 'v1b']       | ['s1a']                             | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b']
        false            | true            | false           | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
        false            | false           | true            | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
        false            | false           | false           | true            | true              | ['w2']            | ['v2']               | ['s2a', 's2b']                      | ['sws1', 'sws3', 'sws4']         | ['rg1a', 'rg2a']
        true             | true            | true            | false           | false             | ['w1']            | ['v1a', 'v1b']       | ['s1a']                             | ['sws1', 'sws2', 'sws3']         | ['rg1a', 'rg1b']
        true             | true            | false           | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
        true             | false           | true            | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
        false            | true            | true            | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
        true             | true            | true            | true            | false             | ['w1']            | ['v1a']              | ['s1a']                             | ['sws1', 'sws3']                 | ['rg1a']
    }

    void "getPossibleAlignmentOptions, should return #expectedRefGenomes when species with strains #selectedSpecies are selected "() {
        given:
        workflowSelectionService.workflowService = Mock(WorkflowService)

        Species species1 = createSpecies()
        Species species2 = createSpecies()

        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain([species: species1, strain: [name: 'sws1']])
        createSpeciesWithStrain([species: species1, strain: [name: 'sws2']])
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain([species: species2, strain: [name: 'sws3']])
        SpeciesWithStrain speciesWithStrain4 = createSpeciesWithStrain([strain: [name: 'sws4']])
        createSpeciesWithStrain([strain: [name: 'sws5']])

        ReferenceGenome refGenome1 = createReferenceGenome([name: 'rg1', species: [species1], speciesWithStrain: [speciesWithStrain4]])
        ReferenceGenome refGenome2 = createReferenceGenome([name: 'rg2', species: [species1], speciesWithStrain: []])
        ReferenceGenome refGenome3 = createReferenceGenome([name: 'rg3', species: [], speciesWithStrain: [speciesWithStrain1, speciesWithStrain3]])
        ReferenceGenome refGenome4 = createReferenceGenome([name: 'rg4', species: [species1], speciesWithStrain: [speciesWithStrain3]])

        SeqType seqType = createSeqType()
        Workflow workflow = createWorkflow()
        createWorkflowVersion([
                workflow               : workflow,
                allowedReferenceGenomes: [refGenome1, refGenome2, refGenome3, refGenome4],
                supportedSeqTypes      : [seqType],
        ])

        when:
        WorkflowSelectionOptionDTO selectedOption = new WorkflowSelectionOptionDTO([
                workflow       : null,
                workflowVersion: null,
                seqType        : null,
                species        : selectedSpecies.collectMany { strainName -> SpeciesWithStrain.findAllByStrain(Strain.findByName(strainName)) },
                refGenome      : null,
        ])
        WorkflowSelectionOptionsDTO result = workflowSelectionService.getPossibleAlignmentOptions(selectedOption)

        then:
        TestCase.assertContainSame(result.refGenomes*.name, expectedRefGenomes)

        then:
        1 * workflowSelectionService.workflowService.findAllAlignmentWorkflows() >> [workflow]

        where:
        selectedSpecies  | expectedRefGenomes
        ['sws1', 'sws4'] | ['rg1']
        ['sws2', 'sws4'] | ['rg1']
        ['sws1']         | ['rg2']
        ['sws2']         | ['rg2']
        ['sws1', 'sws3'] | ['rg3', 'rg4']
        ['sws2', 'sws3'] | ['rg4']
    }
}
