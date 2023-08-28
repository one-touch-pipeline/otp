/*
 * Copyright 2011-2023 The OTP authors
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

import grails.test.hibernate.HibernateSpec

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentArtefactData
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactData

import java.time.LocalDate

class FastqcArtefactServiceSpec extends HibernateSpec implements WorkflowSystemDomainFactory, RoddyPancanFactory, FastqcDomainFactory {

    private FastqcArtefactService fastqcArtefactService

    WorkflowArtefact workflowArtefactSeqTrack1
    WorkflowArtefact workflowArtefactSeqTrack2
    WorkflowArtefact workflowArtefactFastqc1
    WorkflowArtefact workflowArtefactFastqc2

    SeqTrack seqTrack1
    SeqTrack seqTrack2
    FastqcProcessedFile fastqc1
    FastqcProcessedFile fastqc2

    List<WorkflowArtefact> workflowArtefacts
    List<SeqType> seqTypes
    List<SeqTrack> seqTracks

    @Override
    List<Class> getDomainClasses() {
        return [
                FastqFile,
                FastqImportInstance,
                FastqcProcessedFile,
                WorkflowArtefact,
                WorkflowVersionSelector,
        ]
    }

    void setup() {
        fastqcArtefactService = new FastqcArtefactService()
    }

    void setupData() {
        // artefact in input and seqType in input
        workflowArtefactSeqTrack1 = createWorkflowArtefact([artefactType: ArtefactType.FASTQ])
        seqTrack1 = createSeqTrackWithTwoFastqFileAndSpecies([workflowArtefact: workflowArtefactSeqTrack1])
        workflowArtefactFastqc1 = createWorkflowArtefact([artefactType: ArtefactType.FASTQC])
        fastqc1 = createFastqcProcessedFileWithSpecies([workflowArtefact: workflowArtefactFastqc1])

        // input list
        workflowArtefacts = [
                workflowArtefactSeqTrack1,
                workflowArtefactFastqc1,
        ]
        seqTypes = [
                seqTrack1.seqType,
                fastqc1.sequenceFile.seqType,
        ]

        seqTracks = [
                seqTrack1,
                fastqc1.sequenceFile.seqTrack,
        ]
    }

    void "fetchSeqTrackArtefacts, when called for workflowArtefacts, then return FastqcArtefactData of expected SeqTrack"() {
        given:
        setupData()

        FastqcArtefactData<SeqTrack> expected = createFastqcArtefactDataForSeqTrack(seqTrack1)

        when:
        List<FastqcArtefactData<SeqTrack>> result = fastqcArtefactService.fetchSeqTrackArtefacts(workflowArtefacts)

        then:
        result.size() == 1
        result.first() == expected
    }

    void "fetchRelatedFastqcArtefactsForSeqTracks, when called for seqTracks, then return AlignmentArtefactData of expected FastqcProcessedFile"() {
        given:
        setupData()

        List<AlignmentArtefactData<FastqcProcessedFile>> expected = [
                createFastqcArtefactDataForFastqcProcessedFile(fastqc1),
        ]

        when:
        List<AlignmentArtefactData<FastqcProcessedFile>> result = fastqcArtefactService.fetchRelatedFastqcArtefactsForSeqTracks(seqTracks)

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)
    }

    void "fetchWorkflowVersionSelectorForSeqTracks, when called for workflow and seqTracks, then return WorkflowVersionSelector"() {
        given:
        setupData()

        WorkflowVersion workflowVersion = createWorkflowVersion()

        WorkflowVersionSelector selector = createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
                seqType        : null,
        ])

        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
                seqType        : seqTrack1.seqType,
        ])

        createWorkflowVersionSelector([
                project: seqTrack1.project,
                seqType: null,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                seqType        : null,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : seqTrack1.project,
                seqType        : null,
                deprecationDate: LocalDate.now(),
        ])

        when:
        List<WorkflowVersionSelector> result = fastqcArtefactService.fetchWorkflowVersionSelectorForSeqTracks(seqTracks, [workflowVersion.workflow])

        then:
        result.size() == 1
        result.first() == selector
    }

    void "fetchRawSequenceFiles, when called for workflow and seqTracks, then return dataFileMap"() {
        given:
        setupData()

        Map<SeqTrack, List<RawSequenceFile>> expected = [
                (seqTrack1)                    : seqTrack1.sequenceFiles,
                (fastqc1.sequenceFile.seqTrack): [fastqc1.sequenceFile],
        ]

        when:
        Map<SeqTrack, List<RawSequenceFile>> result = fastqcArtefactService.fetchRawSequenceFiles(seqTracks)

        then:
        TestCase.assertContainSame(result, expected)
    }

    private SeqTrack createSeqTrackWithTwoFastqFileAndSpecies(Map parameters) {
        return createSeqTrackWithTwoFastqFile([
                sample: createSample([
                        individual: createIndividual([
                                species: createSpeciesWithStrain(),
                        ]),
                ]),
        ] + parameters)
    }

    private FastqcProcessedFile createFastqcProcessedFileWithSpecies(Map parameters) {
        return createFastqcProcessedFile([
                sequenceFile: createFastqFile([
                        seqTrack: createSeqTrack([
                                sample: createSample([
                                        individual: createIndividual([
                                                species: createSpeciesWithStrain(),
                                        ]),
                                ]),
                        ]),
                ]),
        ] + parameters)
    }

    private FastqcArtefactData<SeqTrack> createFastqcArtefactDataForSeqTrack(SeqTrack seqTrack) {
        return new FastqcArtefactData<SeqTrack>(
                seqTrack.workflowArtefact,
                seqTrack,
                seqTrack.project,
        )
    }

    private FastqcArtefactData<FastqcProcessedFile> createFastqcArtefactDataForFastqcProcessedFile(FastqcProcessedFile fastqcProcessedFile) {
        SeqTrack seqTrack = fastqcProcessedFile.sequenceFile.seqTrack
        return new FastqcArtefactData<FastqcProcessedFile>(
                fastqcProcessedFile.workflowArtefact,
                fastqcProcessedFile,
                seqTrack.project,
        )
    }
}
