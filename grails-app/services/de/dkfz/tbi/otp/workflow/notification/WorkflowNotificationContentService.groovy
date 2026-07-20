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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

/**
 * Provides the reusable content helpers needed to build the workflow notification mails.
 *
 * Each fetch method resolves the data of all given {@link WorkflowRun}s in a single projection query keyed on the
 * workflow run ids. Only the scalar fields needed for the mail are selected, so no lazy domain graph is walked and
 * the query count does not scale with the number of workflow runs. The returned rows are the single data source
 * from which a {@link WorkflowNotification} derives all content sections.
 */
@Transactional
class WorkflowNotificationContentService {

    ProjectService projectService

    /**
     * Fetches the sample rows of the seq tracks produced by the given workflow runs under the given output role.
     * Used by the data installation notification.
     */
    @CompileDynamic
    List<SampleNotificationRow> fetchOutputSampleRows(Collection<WorkflowRun> workflowRuns, String outputRole) {
        if (!workflowRuns) {
            return []
        }
        return SeqTrack.executeQuery('''
                select individual.pid, sampleType.name, seqType.displayName, seqType.libraryLayout, seqType.singleCell,
                       seqTrack.sampleIdentifier, project.id, project.name
                from SeqTrack seqTrack
                join seqTrack.workflowArtefact workflowArtefact
                join seqTrack.sample sample
                join sample.individual individual
                join individual.project project
                join sample.sampleType sampleType
                join seqTrack.seqType seqType
                where workflowArtefact.producedBy.id in (:workflowRunIds)
                  and workflowArtefact.outputRole = :outputRole
                ''', [
                workflowRunIds: workflowRuns*.id,
                outputRole    : outputRole,
        ]).collect { Object[] values -> toSampleRow(values) }
    }

    /**
     * Fetches the sample rows of the seq tracks used as input of the given workflow runs under the given input role.
     * Used by the alignment notification, where the notification text is based on the input seq tracks.
     *
     * The input role is stored per artefact as {@code <role>_<artefactId>} (see the alignment decider), therefore
     * both the plain role and the numbered variants are matched.
     */
    @CompileDynamic
    List<SampleNotificationRow> fetchInputSampleRows(Collection<WorkflowRun> workflowRuns, String inputRole) {
        if (!workflowRuns) {
            return []
        }
        return SeqTrack.executeQuery('''
                select individual.pid, sampleType.name, seqType.displayName, seqType.libraryLayout, seqType.singleCell,
                       seqTrack.sampleIdentifier, project.id, project.name
                from SeqTrack seqTrack
                join seqTrack.sample sample
                join sample.individual individual
                join individual.project project
                join sample.sampleType sampleType
                join seqTrack.seqType seqType,
                WorkflowRunInputArtefact workflowRunInputArtefact
                where workflowRunInputArtefact.workflowArtefact = seqTrack.workflowArtefact
                  and workflowRunInputArtefact.workflowRun.id in (:workflowRunIds)
                  and (workflowRunInputArtefact.role = :inputRole or workflowRunInputArtefact.role like :inputRolePattern)
                ''', [
                workflowRunIds  : workflowRuns*.id,
                inputRole       : inputRole,
                inputRolePattern: "${inputRole}_%".toString(),
        ]).collect { Object[] values -> toSampleRow(values) }
    }

    /**
     * Fetches the sample pair rows of the analysis instances produced by the given workflow runs under the given
     * output role. Used by the analysis notification.
     */
    @CompileDynamic
    List<SamplePairNotificationRow> fetchSamplePairRows(Collection<WorkflowRun> workflowRuns, String outputRole) {
        if (!workflowRuns) {
            return []
        }
        return BamFilePairAnalysis.executeQuery('''
                select individual.pid, sampleType1.name, sampleType2.name,
                       seqType.displayName, seqType.libraryLayout, seqType.singleCell,
                       project.id, project.name
                from BamFilePairAnalysis analysis
                join analysis.workflowArtefact workflowArtefact
                join analysis.samplePair samplePair
                join samplePair.mergingWorkPackage1 mergingWorkPackage1
                join samplePair.mergingWorkPackage2 mergingWorkPackage2
                join mergingWorkPackage1.sample sample1
                join mergingWorkPackage2.sample sample2
                join sample1.individual individual
                join individual.project project
                join sample1.sampleType sampleType1
                join sample2.sampleType sampleType2
                join mergingWorkPackage1.seqType seqType
                where workflowArtefact.producedBy.id in (:workflowRunIds)
                  and workflowArtefact.outputRole = :outputRole
                ''', [
                workflowRunIds: workflowRuns*.id,
                outputRole    : outputRole,
        ]).collect { Object[] values ->
            new SamplePairNotificationRow(
                    values[0] as String, // individual.pid
                    values[1] as String, // sampleType1.name
                    values[2] as String, // sampleType2.name
                    seqTypeDisplayName(values[3] as String, values[4] as SequencingReadType, values[5] as boolean), // seqType
                    values[6] as Long,   // project.id
                    values[7] as String, // project.name
            )
        }
    }

    /**
     * Fetches the BAM rows of the alignment output BAM files produced by the given workflow runs under the given
     * output role. Used by the alignment notification for the GUI URLs and file patterns.
     */
    @CompileDynamic
    List<AlignmentBamNotificationRow> fetchOutputBamRows(Collection<WorkflowRun> workflowRuns, String outputRole) {
        if (!workflowRuns) {
            return []
        }
        return AbstractBamFile.executeQuery('''
                select project.id, project.name, seqType.id, seqType.dirName, seqType.hasAntibodyTarget, seqType.libraryLayout
                from AbstractBamFile bamFile
                join bamFile.workflowArtefact workflowArtefact
                join bamFile.workPackage workPackage
                join workPackage.sample sample
                join sample.individual individual
                join individual.project project
                join workPackage.seqType seqType
                where workflowArtefact.producedBy.id in (:workflowRunIds)
                  and workflowArtefact.outputRole = :outputRole
                ''', [
                workflowRunIds: workflowRuns*.id,
                outputRole    : outputRole,
        ]).collect { Object[] values ->
            new AlignmentBamNotificationRow(
                    values[0] as Long,   // project.id
                    values[1] as String, // project.name
                    values[2] as Long,   // seqType.id
                    values[3] as String, // seqType.dirName
                    values[4] as boolean, // seqType.hasAntibodyTarget
                    (values[5] as SequencingReadType).name().toLowerCase(), // seqType.libraryLayout -> libraryLayoutDirName
            )
        }
    }

    /**
     * Batch loads the projects of the given IDs in a single query, so the {@link de.dkfz.tbi.otp.project.ProjectService}
     * can resolve the sequencing directory from a real project entity without a per-row lookup.
     */
    @CompileDynamic
    Map<Long, Project> loadProjectsById(Collection<Long> projectIds) {
        List<Long> uniqueIds = projectIds.unique(false)
        if (!uniqueIds) {
            return [:]
        }
        return Project.findAllByIdInList(uniqueIds).collectEntries { Project project ->
            [(project.id): project]
        }
    }

    /**
     * Creates the display text of the given sample rows grouped by pid, sample type and seqType.
     *
     * The format per group is: {@code "${pid} ${sampleType} ${seqType} (${sampleNames})"} where the sample names
     * are the unique and sorted {@code sampleIdentifier}s, for example:
     * <pre>
     * PID_A tumor1 WGS PAIRED bulk (sampleName1, sampleName2, sampleName3)
     * </pre>
     */
    Set<String> buildSampleNotificationText(Collection<SampleNotificationRow> rows) {
        if (!rows) {
            return [] as Set
        }
        return rows.groupBy { SampleNotificationRow row ->
            "${row.pid} ${row.sampleTypeName} ${row.seqTypeDisplayName}"
        }.sort { it.key }.collect { String header, List<SampleNotificationRow> groupRows ->
            String sampleNames = groupRows*.sampleIdentifier.unique().sort().join(", ")
            "${header} (${sampleNames})".toString()
        } as Set
    }

    /**
     * Creates the display text of the given sample pair rows.
     *
     * The format per sample pair is: {@code "${pid} ${sampleType1} ${sampleType2} ${seqType}"}.
     */
    Set<String> buildSamplePairNotificationText(Collection<SamplePairNotificationRow> rows) {
        if (!rows) {
            return [] as Set
        }
        return rows.collect { SamplePairNotificationRow row ->
            "${row.pid} ${row.sampleType1Name} ${row.sampleType2Name} ${row.seqTypeDisplayName}".toString()
        }.unique().sort() as Set
    }

    /**
     * Creates the file patterns of the merged alignment directories of the given BAM files.
     *
     * PID, sample type and (if applicable) antibody target are kept as variables in the returned patterns.
     *
     * @deprecated bridge for the old {@link de.dkfz.tbi.otp.notification.CreateNotificationTextService};
     * remove together with the old workflow system.
     */
    @Deprecated
    @SuppressWarnings('GStringExpressionWithinString')
    Set<String> getMergingDirectories(Collection<AbstractBamFile> bamFiles) {
        if (!bamFiles) {
            return [] as Set
        }

        String pid = '${PID}'
        String sampleType = '${SAMPLE_TYPE}'

        return bamFiles.collect { AbstractBamFile bamFile ->
            SeqType seqType = bamFile.seqType
            String antiBodyTarget = seqType.hasAntibodyTarget ? '-${ANTI_BODY_TARGET}' : ''
            projectService.getSequencingDirectory(bamFile.project)
                    .resolve(seqType.dirName)
                    .resolve("view-by-pid")
                    .resolve(pid)
                    .resolve("${sampleType}${antiBodyTarget}")
                    .resolve(seqType.libraryLayoutDirName)
                    .resolve("merged-alignment")
                    .toString()
        }.unique().sort() as Set
    }

    private static SampleNotificationRow toSampleRow(Object[] values) {
        return new SampleNotificationRow(
                values[0] as String, // individual.pid
                values[1] as String, // sampleType.name
                seqTypeDisplayName(values[2] as String, values[3] as SequencingReadType, values[4] as boolean), // seqType
                values[5] as String, // seqTrack.sampleIdentifier
                values[6] as Long,   // project.id
                values[7] as String, // project.name
        )
    }

    /**
     * Rebuilds {@link SeqType#getDisplayNameWithLibraryLayout} from the projected scalar fields.
     */
    private static String seqTypeDisplayName(String displayName, SequencingReadType libraryLayout, boolean singleCell) {
        String singleCellDisplayName = singleCell ? SeqType.SINGLE_CELL_TRUE : SeqType.SINGLE_CELL_FALSE
        return "${displayName} ${libraryLayout} ${singleCellDisplayName}".toString()
    }
}
