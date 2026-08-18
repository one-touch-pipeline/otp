/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.notification

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Rollback
@Integration
class WorkflowNotificationContentServiceIntegrationSpec extends Specification implements RoddyPanCancerFactory, WorkflowSystemDomainFactory {

    WorkflowNotificationContentService workflowNotificationContentService

    void "fetchOutputSampleRows, projects the scalar sample data of the output seq tracks of the given runs"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowArtefact artefact = createWorkflowArtefact(producedBy: run, outputRole: DataInstallationWorkflow.OUTPUT_FASTQ)
        Sample sample = createSample(individual: createIndividual(pid: "pidX"), sampleType: createSampleType(name: "tumor"))
        SeqType seqType = createSeqType()
        createSeqTrack(sample: sample, seqType: seqType, sampleIdentifier: "sampleId1", workflowArtefact: artefact)

        when:
        List<SampleNotificationRow> rows = workflowNotificationContentService.fetchOutputSampleRows([run], DataInstallationWorkflow.OUTPUT_FASTQ)

        then:
        rows.size() == 1
        rows.first().pid == "pidX"
        rows.first().sampleTypeName == "tumor"
        rows.first().seqTypeDisplayName == seqType.displayNameWithLibraryLayout
        rows.first().sampleIdentifier == "sampleId1"
        rows.first().projectId == sample.individual.project.id
        rows.first().projectName == sample.individual.project.name
        rows.first().workflowRunId == run.id
    }

    void "fetchInputSampleRows, projects the scalar sample data of the input seq tracks linked via the numbered input role"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowArtefact artefact = createWorkflowArtefact()
        Sample sample = createSample(individual: createIndividual(pid: "pidY"), sampleType: createSampleType(name: "control"))
        SeqType seqType = createSeqType()
        createSeqTrack(sample: sample, seqType: seqType, sampleIdentifier: "sampleId2", workflowArtefact: artefact)
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: "${AlignmentWorkflow.INPUT_FASTQ}_${artefact.id}",
                workflowArtefact: artefact,
        )

        when:
        List<SampleNotificationRow> rows = workflowNotificationContentService.fetchInputSampleRows([run], AlignmentWorkflow.INPUT_FASTQ)

        then:
        rows.size() == 1
        rows.first().pid == "pidY"
        rows.first().sampleTypeName == "control"
        rows.first().seqTypeDisplayName == seqType.displayNameWithLibraryLayout
        rows.first().sampleIdentifier == "sampleId2"
        rows.first().workflowRunId == run.id
    }

    void "fetchSamplePairRows, projects the scalar sample pair data of the analysis instances of the given runs"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowArtefact artefact = createWorkflowArtefact(producedBy: run, outputRole: AbstractAnalysisWorkflow.ANALYSIS_OUTPUT)
        SnvCallingInstance instance = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(workflowArtefact: artefact)
        SamplePair samplePair = instance.samplePair

        when:
        List<SamplePairNotificationRow> rows = workflowNotificationContentService.fetchSamplePairRows([run], AbstractAnalysisWorkflow.ANALYSIS_OUTPUT)

        then:
        rows.size() == 1
        rows.first().pid == samplePair.individual.pid
        rows.first().sampleType1Name == samplePair.sampleType1.name
        rows.first().sampleType2Name == samplePair.sampleType2.name
        rows.first().seqTypeDisplayName == samplePair.seqType.displayNameWithLibraryLayout
        rows.first().projectId == samplePair.project.id
        rows.first().projectName == samplePair.project.name
        rows.first().workflowRunId == run.id
    }

    void "fetchOutputBamRows, projects the scalar project and seq type data of the output bam files of the given runs"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowArtefact artefact = createWorkflowArtefact(producedBy: run, outputRole: AlignmentWorkflow.OUTPUT_BAM)
        RoddyBamFile bamFile = createBamFile(workflowArtefact: artefact)

        when:
        List<AlignmentBamNotificationRow> rows = workflowNotificationContentService.fetchOutputBamRows([run], AlignmentWorkflow.OUTPUT_BAM)

        then:
        rows.size() == 1
        rows.first().projectId == bamFile.project.id
        rows.first().projectName == bamFile.project.name
        rows.first().seqTypeId == bamFile.seqType.id
        rows.first().seqTypeDirName == bamFile.seqType.dirName
        rows.first().hasAntibodyTarget == bamFile.seqType.hasAntibodyTarget
        rows.first().libraryLayoutDirName == bamFile.seqType.libraryLayoutDirName
    }

    void "fetchBamImportNotificationRows, projects the scalar data of externally processed bam files of the given runs"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowArtefact artefact = createWorkflowArtefact(producedBy: run, outputRole: BamImportWorkflow.OUTPUT_BAM)
        ExternallyProcessedBamFile bamFile = DomainFactory.createExternallyProcessedBamFile(workflowArtefact: artefact)

        when:
        List<BamImportNotificationRow> rows = workflowNotificationContentService.fetchBamImportNotificationRows([run], BamImportWorkflow.OUTPUT_BAM)

        then:
        rows.size() == 1
        rows.first().pid == bamFile.individual.pid
        rows.first().sampleTypeName == bamFile.sampleType.name
        rows.first().seqTypeDisplayName == bamFile.seqType.displayNameWithLibraryLayout
        rows.first().seqTypeDirName == bamFile.seqType.dirName
        rows.first().hasAntibodyTarget == bamFile.seqType.hasAntibodyTarget
        rows.first().libraryLayoutDirName == bamFile.seqType.libraryLayoutDirName
        rows.first().projectId == bamFile.project.id
        rows.first().workflowRunId == run.id
    }

    void "fetch methods, when no workflow runs are given, return an empty list"() {
        expect:
        workflowNotificationContentService.fetchOutputSampleRows([], DataInstallationWorkflow.OUTPUT_FASTQ) == []
        workflowNotificationContentService.fetchInputSampleRows([], AlignmentWorkflow.INPUT_FASTQ) == []
        workflowNotificationContentService.fetchSamplePairRows([], AbstractAnalysisWorkflow.ANALYSIS_OUTPUT) == []
        workflowNotificationContentService.fetchOutputBamRows([], AlignmentWorkflow.OUTPUT_BAM) == []
        workflowNotificationContentService.fetchBamImportNotificationRows([], BamImportWorkflow.OUTPUT_BAM) == []
    }

    void "fetchIlseNumbers, projects the distinct ILSe numbers of the ticket ascending, ignoring seq tracks without one"() {
        given:
        Ticket ticket = createTicket()
        IlseSubmission sharedSubmission = createIlseSubmission(ilseNumber: 100)
        createSeqTrackOfTicket(ticket, createIlseSubmission(ilseNumber: 300))
        createSeqTrackOfTicket(ticket, sharedSubmission)
        createSeqTrackOfTicket(ticket, sharedSubmission)
        createSeqTrackOfTicket(ticket, null)
        createSeqTrackOfTicket(createTicket(), createIlseSubmission(ilseNumber: 999))

        expect:
        workflowNotificationContentService.fetchIlseNumbers(ticket) == [100, 300]
    }

    void "fetchIlseNumbers, when no seq track of the ticket has an ILSe number, returns an empty list"() {
        given:
        Ticket ticket = createTicket()
        createSeqTrackOfTicket(ticket, null)

        expect:
        workflowNotificationContentService.fetchIlseNumbers(ticket) == []
    }

    void "loadProjectsById, batch loads the unique projects by id ignoring null ids"() {
        given:
        Project project1 = createProject()
        Project project2 = createProject()

        when:
        Map<Long, Project> projects = workflowNotificationContentService.loadProjectsById([project1.id, project2.id, project1.id, null])

        then:
        projects == [(project1.id): project1, (project2.id): project2]
    }

    private SeqTrack createSeqTrackOfTicket(Ticket ticket, IlseSubmission ilseSubmission) {
        SeqTrack seqTrack = createSeqTrack(ilseSubmission: ilseSubmission)
        FastqFile fastqFile = createFastqFile(seqTrack: seqTrack)
        fastqFile.fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [fastqFile])
        fastqFile.save(flush: true)
        return seqTrack
    }
}
