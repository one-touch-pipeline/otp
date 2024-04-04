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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactData

import java.nio.file.Paths

class FastqcDeciderSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, FastqcDomainFactory, FastqcWorkflowDomainFactory {

    protected FastqcDecider decider

    private WorkflowVersion wesWorkflowVersion
    private WorkflowVersion bashWorkflowVersion
    private WorkflowVersion usedVersion

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                WorkflowRunInputArtefact,
                WorkflowVersionSelector,
        ]
    }

    void setupData(boolean useWes) {
        decider = new FastqcDecider()

        wesWorkflowVersion = createWesFastqcWorkflowVersion()
        bashWorkflowVersion = createBashFastqcWorkflowVersion()
        usedVersion = useWes ? wesWorkflowVersion : bashWorkflowVersion
    }

    @Unroll
    void "decide, when seqTrack, then create new artefacts and add no warning"() {
        given:
        setupData(useWes)

        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.FASTQ,
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile([
                workflowArtefact: workflowArtefact
        ])
        FastqcArtefactData<SeqTrack> fastqcArtefactData = new FastqcArtefactData<SeqTrack>(workflowArtefact, seqTrack, seqTrack.project)

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = seqTrack.sequenceFiles.groupBy {
            it.seqTrack
        }

        WorkflowVersionSelector selector = createWorkflowVersionSelector([
                workflowVersion: usedVersion,
                project        : seqTrack.project,
                seqType        : null,
        ])

        and: 'services'
        createServicesForCreateWorkflowRunsAndOutputArtefacts(usedVersion, seqTrack)
        decider.fastqcArtefactService = Mock(FastqcArtefactService) {
            1 * fetchSeqTrackArtefacts([workflowArtefact]) >> [fastqcArtefactData]
            1 * fetchRawSequenceFiles([seqTrack]) >> rawSequenceFileMap
            1 * fetchRelatedFastqcArtefactsForSeqTracks([seqTrack]) >> []
            1 * fetchWorkflowVersionSelectorForSeqTracks([seqTrack], _) >> [selector]
            0 * _
        }
        decider.workflowService = Mock(WorkflowService) {
            1 * getExactlyOneWorkflow(WesFastQcWorkflow.WORKFLOW) >> wesWorkflowVersion.workflow
            1 * getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW) >> bashWorkflowVersion.workflow
            0 * _
        }

        when:
        DeciderResult deciderResult = decider.decide([workflowArtefact], [:])

        then:
        deciderResult.newArtefacts.size() == 2
        deciderResult.warnings.empty

        where:
        name   | useWes
        'bash' | false
        'wes'  | true
    }

    @Unroll
    void "decide, when no seqtrack, then add warning"() {
        given:
        setupData(useWes)

        decider.workflowService = Mock(WorkflowService) {
            1 * getExactlyOneWorkflow(WesFastQcWorkflow.WORKFLOW) >> wesWorkflowVersion.workflow
            1 * getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW) >> bashWorkflowVersion.workflow
            0 * _
        }
        decider.fastqcArtefactService = Mock(FastqcArtefactService) {
            1 * fetchSeqTrackArtefacts([]) >> []
            0 * _
        }

        when:
        DeciderResult deciderResult = decider.decide([], [:])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.empty
        deciderResult.infos.any {
            it.contains('no data found for')
        }

        where:
        name   | useWes
        'bash' | false
        'wes'  | true
    }

    @Unroll
    void "decide, when no selector defined, then add warning"() {
        given:
        setupData(useWes)

        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.FASTQ,
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile([
                workflowArtefact: workflowArtefact
        ])
        FastqcArtefactData<SeqTrack> fastqcArtefactData = new FastqcArtefactData<SeqTrack>(workflowArtefact, seqTrack, seqTrack.project)

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = seqTrack.sequenceFiles.groupBy {
            it.seqTrack
        }

        and: 'services'
        decider.fastqcArtefactService = Mock(FastqcArtefactService) {
            1 * fetchSeqTrackArtefacts([workflowArtefact]) >> [fastqcArtefactData]
            1 * fetchRawSequenceFiles([seqTrack]) >> rawSequenceFileMap
            1 * fetchRelatedFastqcArtefactsForSeqTracks([seqTrack]) >> []
            1 * fetchWorkflowVersionSelectorForSeqTracks([seqTrack], _) >> []
            0 * _
        }
        decider.workflowService = Mock(WorkflowService) {
            1 * getExactlyOneWorkflow(WesFastQcWorkflow.WORKFLOW) >> wesWorkflowVersion.workflow
            1 * getExactlyOneWorkflow(BashFastQcWorkflow.WORKFLOW) >> bashWorkflowVersion.workflow
            0 * _
        }

        when:
        DeciderResult deciderResult = decider.decide([workflowArtefact], [:])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 1
        deciderResult.warnings.first().contains('since no workflow version configured')

        where:
        name   | useWes
        'bash' | false
        'wes'  | true
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #name, then create new FastqcProcessedFile file and add no warning"() {
        given:
        setupData(useWes)

        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.FASTQ,
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile([
                workflowArtefact: workflowArtefact
        ])
        FastqcArtefactData<SeqTrack> fastqcArtefactData = new FastqcArtefactData<SeqTrack>(workflowArtefact, seqTrack, seqTrack.project)

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = seqTrack.sequenceFiles.groupBy {
            it.seqTrack
        }

        WorkflowVersionSelector selector = createWorkflowVersionSelector([
                workflowVersion: usedVersion,
                project        : seqTrack.project,
                seqType        : null,
        ])

        and: 'services'
        createServicesForCreateWorkflowRunsAndOutputArtefacts(usedVersion, seqTrack)

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(fastqcArtefactData, [], rawSequenceFileMap, selector)

        then:
        deciderResult.newArtefacts.size() == 2
        deciderResult.warnings.empty

        WorkflowArtefact workflowArtefact1 = deciderResult.newArtefacts[0]
        workflowArtefact1.artefactType == ArtefactType.FASTQC
        workflowArtefact1.outputRole.startsWith(BashFastQcWorkflow.OUTPUT_FASTQC)

        WorkflowArtefact workflowArtefact2 = deciderResult.newArtefacts[1]
        workflowArtefact2.artefactType == ArtefactType.FASTQC
        workflowArtefact2.outputRole.startsWith(BashFastQcWorkflow.OUTPUT_FASTQC)

        WorkflowRun run = workflowArtefact1.producedBy
        run.workflow == usedVersion.workflow
        run.workflowVersion == usedVersion

        List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.list()
        fastqcProcessedFiles.size() == 2
        TestCase.assertContainSame(fastqcProcessedFiles*.sequenceFile, seqTrack.sequenceFiles)

        where:
        name   | useWes
        'bash' | false
        'wes'  | true
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #name, then create no FastqcProcessedFile file and add a warning"() {
        given:
        setupData(useWes)

        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.FASTQ,
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile([
                workflowArtefact: workflowArtefact
        ])
        FastqcArtefactData<SeqTrack> fastqcArtefactDataSeqTrack = new FastqcArtefactData<SeqTrack>(workflowArtefact, seqTrack, seqTrack.project)

        Map<SeqTrack, List<RawSequenceFile>> rawSequenceFileMap = seqTrack.sequenceFiles.groupBy {
            it.seqTrack
        }

        List<FastqcArtefactData> fastqcArtefactDataFasqc = seqTrack.sequenceFiles.collect {
            FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                    sequenceFile    : it,
                    workflowArtefact: createWorkflowArtefact([
                            artefactType: ArtefactType.FASTQC,
                    ]),
            ])
            new FastqcArtefactData<FastqcProcessedFile>(fastqcProcessedFile.workflowArtefact, fastqcProcessedFile, fastqcProcessedFile.sequenceFile.project)
        }

        WorkflowVersionSelector selector = createWorkflowVersionSelector([
                workflowVersion: usedVersion,
                project        : seqTrack.project,
                seqType        : null,
        ])

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(fastqcArtefactDataSeqTrack, fastqcArtefactDataFasqc, rawSequenceFileMap, selector)

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 1
        deciderResult.warnings.first().contains('since fastqc already exist')

        where:
        name   | useWes
        'bash' | false
        'wes'  | true
    }

    private void createServicesForCreateWorkflowRunsAndOutputArtefacts(WorkflowVersion workflowVersion, SeqTrack seqTrack) {
        boolean uuidUsed = (workflowVersion == wesWorkflowVersion)
        decider.fastQcProcessedFileService = Mock(FastQcProcessedFileService) {
            1 * buildWorkingPath(workflowVersion) >> "path"
            1 * useUuid(workflowVersion) >> uuidUsed
            0 * _
        }
        decider.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            (uuidUsed ? 0 : 1) * fastqcOutputDirectory(_) >> Paths.get('output')
            0 * _
        }
        decider.workflowRunService = Mock(WorkflowRunService) {
            1 * buildWorkflowRun(workflowVersion.workflow, seqTrack.project.processingPriority, _, seqTrack.project, _, _, workflowVersion) >> {
                Workflow workflowParam, ProcessingPriority priorityParam, String workDirectoryParam, Project projectParam,
                List<String> displayNameLinesParam, String shortNameParam, WorkflowVersion workflowVersionParam ->
                    new WorkflowRun([ // codenarc-disable-line
                                      workDirectory   : workDirectoryParam,
                                      state           : WorkflowRun.State.PENDING,
                                      project         : projectParam,
                                      combinedConfig  : null,
                                      priority        : priorityParam,
                                      restartedFrom   : null,
                                      skipMessage     : null,
                                      workflowSteps   : [],
                                      workflow        : workflowParam,
                                      workflowVersion : workflowVersionParam,
                                      displayName     : displayNameLinesParam.join(', '),
                                      shortDisplayName: shortNameParam,
                    ]).save(flush: false)
            }
            0 * _
        }
        decider.workflowArtefactService = Mock(WorkflowArtefactService) {
            2 * buildWorkflowArtefact(_) >> { WorkflowArtefactValues values ->
                return new WorkflowArtefact([
                        producedBy      : values.run,
                        outputRole      : values.role,
                        withdrawnDate   : null,
                        withdrawnComment: null,
                        state           : WorkflowArtefact.State.PLANNED_OR_RUNNING,
                        artefactType    : values.artefactType,
                        displayName     : values.displayNameLines.join(', '),
                ]).save(flush: false)
            }
            0 * _
        }
    }
}
