/*
 * Copyright 2011-2019 The OTP authors
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

package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PostAuthorize

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.utils.CollectionUtils

import javax.sql.DataSource
import java.nio.file.Paths
import java.text.MessageFormat

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog
import static org.springframework.util.Assert.notNull

@Transactional
class SeqTrackService {

    FileTypeService fileTypeService

    /**
     * Dependency Injection of Project Service.
     *
     * Needed for access control on data protected by Projects.
     */
    ProjectService projectService
    /**
     * Dependency Injection of LSDF File Service.
     *
     * Required for exporting the view by pid path of a datafile
     */
    LsdfFilesService lsdfFilesService
    /**
     * Dependency Injection of Spring Security Service.
     */
    SpringSecurityService springSecurityService

    AlignmentDeciderService alignmentDeciderService

    @Autowired
    DataSource dataSource


    @Autowired
    ApplicationContext applicationContext

    /**
     * Retrieves the Sequences matching the given filtering the user has access to.
     * The access restriction is done through the Projects the user has access to.
     * @param offset Offset in data
     * @param max Maximum number of elements, capped at 100
     * @param sortOrder true for ascending, false for descending
     * @param column The column to perform the sorting on
     * @param filtering Filtering restrictions
     * @return List of matching Sequences
     */
    List<Sequence> listSequences(int offset, int max, boolean sortOrder, SequenceSortColumn column, SequenceFiltering filtering) {
        if (filtering.enabled) {
            def c = Sequence.createCriteria()
            return c.list {
                filteringClosure.delegate = delegate
                filteringClosure(filtering)
                if (max != -1) { //-1 indicate in jquery datatable, that no paging is used. Therefore in that case no maxResult are set
                    maxResults(max)
                }
                firstResult(offset)
                order(column.columnName, sortOrder ? "asc" : "desc")
            }
        } else {
            List<Project> projects = projectService.getAllProjects()
            return projects ? Sequence.findAllByProjectIdInList(projects*.id, [
                    offset: offset,
                    max: max,
                    sort: column.columnName,
                    order: sortOrder ? "asc" : "desc",
            ]) : []
        }
    }

    /**
     * Counts the Sequences the User has access to by applying the provided filtering.
     * @param filtering The filters to apply on the data
     * @return Number of Sequences matching the filtering
     */
    int countSequences(SequenceFiltering filtering) {
        if (filtering.enabled) {
            def c = Sequence.createCriteria()
            return c.get {
                filteringClosure.delegate = delegate
                filteringClosure(filtering)
                projections { count('mockPid') }
            }
        } else {
            // shortcut for unfiltered results
            List<Project> projects = projectService.getAllProjects()
            return projects ? Sequence.countByProjectIdInList(projects*.id) : 0
        }
    }

    Closure filteringClosure = { SequenceFiltering filtering ->
        'in'('projectId', projectService.getAllProjects().collect { it.id })
        if (filtering.project) {
            'in'('projectId', filtering.project)
        }
        if (filtering.individual) {
            or {
                filtering.individual.each {
                    ilike('mockPid', "%${it}%")
                }
            }
        }
        if (filtering.sampleType) {
            'in'('sampleTypeId', filtering.sampleType)
        }
        if (filtering.seqType) {
            'in'('seqTypeDisplayName', filtering.seqType)
        }
        if (filtering.libraryLayout) {
            'in'('libraryLayout', filtering.libraryLayout)
        }
        if (filtering.singleCell) {
            'in'('singleCell', filtering.singleCell)
        }
        if (filtering.seqCenter) {
            'in'('seqCenterId', filtering.seqCenter)
        }
        if (filtering.libraryPreparationKit) {
            'in'('libraryPreparationKit', filtering.libraryPreparationKit)
        }
        if (filtering.ilseId) {
            'in'('ilseId', filtering.ilseId)
        }
        if (filtering.run) {
            or {
                filtering.run.each {
                    ilike('name', "%${it}%")
                }
            }
        }
    }

    /**
     * Calls the {@link AlignmentDecider#decideAndPrepareForAlignment(SeqTrack, boolean)} method of the
     * {@link AlignmentDecider} specified by the {@link Project#alignmentDeciderBeanName} property of the specified
     * {@link SeqTrack}'s {@link Project}.
     */
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign = false) {
        AlignmentDecider decider = alignmentDeciderService.getAlignmentDecider(seqTrack.project)
        return decider.decideAndPrepareForAlignment(seqTrack, forceRealign)
    }

    static boolean mayAlign(SeqTrack seqTrack, boolean log = true) {

        def notAligning = { String reason ->
            if (log) {
                AbstractAlignmentDecider.logNotAligning(seqTrack, reason)
            }
        }

        if (seqTrack.withdrawn) {
            notAligning('it is withdrawn')
            return false
        }

        if (!DataFile.withCriteria {
            eq 'seqTrack', seqTrack
            fileType {
                eq 'type', FileType.Type.SEQUENCE
            }
            eq 'fileWithdrawn', false
        }) {
            notAligning('it has no sequence files')
            return false
        }

        if (DataFile.withCriteria {
            eq 'seqTrack', seqTrack
            runSegment {
                eq 'align', false
            }
        }) {
            notAligning('alignment is disabled for the RunSegment')
            return false
        }

        if (seqTrack instanceof ExomeSeqTrack &&
                seqTrack.libraryPreparationKit == null &&
                seqTrack.kitInfoReliability == InformationReliability.UNKNOWN_VERIFIED) {
            notAligning('kitInfoReliability is UNKNOWN_VERIFIED')
            return false
        }

        if (seqTrack.seqPlatformGroup == null) {
            notAligning("seqPlatformGroup is null for ${seqTrack.seqPlatform}")
            return false
        }

        return true
    }

    /**
     * returns the most high prioritized, oldest alignable {@link SeqTrack} waiting for fastqc if possible,
     * otherwise the most high prioritized, oldest {@link SeqTrack} waiting for fastqc.
     *
     * @return a seqTrack without fastqc
     */
    SeqTrack getSeqTrackReadyForFastqcProcessing(ProcessingPriority minPriority) {
        List<SeqType> seqTypes = SeqTypeService.getAllAlignableSeqTypes()
        List args = [SeqTrack.DataProcessingState.NOT_STARTED.toString(),
                     minPriority.priority,
        ] + seqTypes*.id


        // this workaround is used because
        // HQL would support IN but doesn't support expressions in ORDER BY clauses,
        // JDBC doesn't support IN directly,
        // and the H2 driver doesn't support PreparedStatement.setArray()
        String questionMarksSeparatedByCommas = (["?"]* seqTypes.size()).join(",")

        String query = """\
SELECT st.id
FROM seq_track AS st
JOIN sample ON st.sample_id = sample.id
JOIN individual ON sample.individual_id = individual.id
JOIN project ON individual.project_id = project.id

WHERE st.fastqc_state = ?
AND project.processing_priority >= ?
AND NOT EXISTS (SELECT seq_track_id FROM data_file WHERE file_withdrawn = true AND seq_track_id = st.id)

ORDER BY project.processing_priority DESC, (st.seq_type_id IN (${questionMarksSeparatedByCommas})) DESC, st.id ASC
LIMIT 1
;
"""

        GroovyRowResult seqTrack = new Sql(dataSource).firstRow(query, args)
        return SeqTrack.get(seqTrack?.id)
    }

    static void setFastqcInProgress(SeqTrack seqTrack) {
        seqTrack.fastqcState = SeqTrack.DataProcessingState.IN_PROGRESS
        assert(seqTrack.save(flush: true))
    }

    void setFastqcFinished(SeqTrack seqTrack) {
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert(seqTrack.save(flush: true))
    }

    List<DataFile> getSequenceFilesForSeqTrack(SeqTrack seqTrack) {
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        List<DataFile> filteredFiles = []
        files.each {
            if (fileTypeService.isGoodSequenceDataFile(it)) {
                filteredFiles.add(it)
            }
        }
        return filteredFiles
    }

    // TODO OTP-2040: Do we still need this method?
    private Sample getSample(DataFile file) {
        String sampleName = metaDataValue(file, MetaDataColumn.SAMPLE_ID.name())
        SampleIdentifier idx = SampleIdentifier.findByName(sampleName)
        return idx.sample
    }

    // TODO OTP-2040: Do we still need this method?
    private void assertConsistentSample(Sample sample, List<DataFile> files) {
        for (DataFile file in files) {
            Sample fileSample = getSample(file)
            if (!sample.equals(fileSample)) {
                throw new SampleInconsistentException(files, sample, fileSample)
            }
        }
    }

    void fillBaseCount(SeqTrack seqTrack) {
        long basePairs = 0
        seqTrack.dataFilesWhereIndexFileIsFalse.each { DataFile file ->
            assert (file.sequenceLength && file.nReads): "The sequence length or nReads for datafile ${file} are not provided."
            basePairs += file.meanSequenceLength * file.nReads
        }
        seqTrack.nBasePairs = basePairs
        assert seqTrack.save(flush: true)
    }

    // TODO OTP-2040: Do we still need this method?
    /**
     * Attach alignment files to a given seq track
     * @param seqTrack
     */
    private void appendAlignmentToSeqTrack(SeqTrack seqTrack) {
        // attach alignment to seqTrack
        List<DataFile> alignFiles =
                        getRunFilesWithTypeAndLane(seqTrack.run, FileType.Type.ALIGNMENT, seqTrack.laneId)
        if (alignFiles.size() == 0) {
            return
        }
        Set<SoftwareTool> pipelines = getAlignmentPipelineSet(alignFiles)

        Sample sample = getSample(alignFiles.get(0))
        assertConsistentSample(sample, alignFiles)


        for (SoftwareTool pipeline in pipelines) {

            AlignmentParams alignParams = getAlignmentParams(pipeline)
            AlignmentLog alignLog = new AlignmentLog(
                            alignmentParams : alignParams,
                            seqTrack : seqTrack,
                            executedBy : AlignmentLog.Execution.INITIAL
                            )
            alignLog.save(flush: true)
            consumeAlignmentFiles(alignLog, alignFiles, pipeline)
            seqTrack.hasOriginalBam = true
            alignLog.save(flush: true)
            alignParams.save(flush: true)
        }
        seqTrack.save(flush: true)
    }

    // TODO OTP-2040: Do we still need this method?
    Set<SoftwareTool> getAlignmentPipelineSet(List<DataFile> alignFiles) {
        Set<SoftwareTool> set = new HashSet<SoftwareTool>()
        for (DataFile file in alignFiles) {
            SoftwareTool pipeline = getAlignmentPipeline(file)
            set << pipeline
        }
        return set
    }

    // TODO OTP-2040: Do we still need this method?
    SoftwareTool getAlignmentPipeline(DataFile file) {
        String name = metaDataValue(file, MetaDataColumn.ALIGN_TOOL.name())
        List<SoftwareToolIdentifier> idx = SoftwareToolIdentifier.findAllByName(name)
        for (SoftwareToolIdentifier si in idx) {
            if (si.softwareTool.type == SoftwareTool.Type.ALIGNMENT) {
                return si.softwareTool
            }
        }
        return null
    }

    // TODO OTP-2040: Do we still need this method?
    private AlignmentParams getAlignmentParams(SoftwareTool pipeline) {
        AlignmentParams alignParams = AlignmentParams.findByPipeline(pipeline)
        if (!alignParams) {
            alignParams = new AlignmentParams(pipeline: pipeline)
            alignParams.save(flush: true)
        }
        return alignParams
    }

    // TODO OTP-2040: Do we still need this method?
    private void consumeAlignmentFiles(AlignmentLog alignLog, List<DataFile> files, SoftwareTool pipeline) {
        for (DataFile file in files) {
            SoftwareTool filePipeline = getAlignmentPipeline(file)
            if (pipeline.equals(filePipeline)) {
                file.project = alignLog.seqTrack.project
                file.alignmentLog = alignLog
                file.used = true
                file.save(flush: true)
            }
        }
    }

    // TODO OTP-2040: Do we still need this method?
    /**
     * Return all dataFiles for a given run, type and lane
     * Only dataFiles which are not used are returned
     *
     * @param run The Run
     * @param type The Type
     * @param lane The lane
     */
    private def getRunFilesWithTypeAndLane(Run run, FileType.Type type, String lane) {
        MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.LANE_NO.name())
        def dataFiles = DataFile.executeQuery("""
SELECT dataFile FROM MetaDataEntry as entry
INNER JOIN entry.dataFile as dataFile
WHERE
dataFile.run = :run
AND dataFile.fileWithdrawn = false
AND dataFile.fileType.type = :type
AND dataFile.used = false
AND entry.key = :key
AND entry.value = :value
""".toString(),
                        [run: run, type: type, key: key, value: lane])
        return dataFiles
    }

    // TODO OTP-2040: Do we still need this method?
    private MetaDataEntry metaDataEntry(DataFile file, String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(file, key)
        if (!entry) {
            throw new ProcessingException("no entry for key: ${keyName}")
        }
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }

    // TODO OTP-2040: Do we still need this method?
    private String metaDataValue(DataFile file, String keyName) {
        MetaDataEntry entry = metaDataEntry(file, keyName)
        return entry.value
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.sample.individual.project, 'OTP_READ_ACCESS')")
    SeqTrack getSeqTrack(String identifier) {
        if (!identifier) {
            return null
        }
        SeqTrack seqTrack = null
        if (identifier.isLong()) {
            seqTrack = SeqTrack.get(identifier as Long)
        }
        return seqTrack
    }


    /**
     * This method determines if a fastq file has to be linked or copied to the project folder and stores the information in the seqTrack.
     * If a fastq file fulfills the following constraints it has to be linked:
     * - provided via storage that allows linking
     * - will be aligned
     * - the project allows linking
     * - the sequencing type allows linking (depends on whether the corresponding alignment workflow allows incremental merging)
     * - doesn't need adapter trimming
     */
    void determineAndStoreIfFastqFilesHaveToBeLinked(SeqTrack seqTrack, boolean willBeAligned) {
        assert seqTrack : "The input seqTrack for determineAndStoreIfFastqFilesHaveToBeLinked must not be null"
        boolean importDirAllowsLinking = doesImportDirAllowLinking(seqTrack)
        boolean projectAllowsLinking = !seqTrack.project.forceCopyFiles
        boolean seqTypeAllowsLinking = seqTrack.seqType.seqTypeAllowsLinking()
        boolean hasIndexFiles = hasIndexFiles(seqTrack)
        boolean adapterTrimming = RoddyWorkflowConfig.getLatestForIndividual(seqTrack.individual, seqTrack.seqType,
                Pipeline.findByName(seqTrack.seqType.isRna() ?
                        Pipeline.Name.RODDY_RNA_ALIGNMENT : Pipeline.Name.PANCAN_ALIGNMENT))?.adapterTrimmingNeeded ?: false
        boolean link = willBeAligned  && importDirAllowsLinking && projectAllowsLinking && seqTypeAllowsLinking && !hasIndexFiles && !adapterTrimming
        seqTrack.log("Fastq files{0} will be ${link ? "linked" : "copied"}, because " +
                "willBeAligned=${willBeAligned}, importDirAllowsLinking=${importDirAllowsLinking}, projectAllowsLinking=${projectAllowsLinking}, " +
                "seqTypeAllowsLinking=${seqTypeAllowsLinking}, needs adapter trimming=${adapterTrimming}, hasIndexFiles=${hasIndexFiles}")
        if (link) {
            seqTrack.linkedExternally = true
            assert seqTrack.save(flush: true)
        }
    }

    private boolean hasIndexFiles(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack for doesImportDirAllowLinking must not be null"
        return seqTrack.dataFiles.find {
            it.indexFile
        }
    }

    private boolean doesImportDirAllowLinking(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack for doesImportDirAllowLinking must not be null"
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        return files.every { DataFile dataFile ->
            seqTrack.seqCenter.importDirsAllowLinking.any {
                Paths.get(dataFile.initialDirectory).startsWith(it)
            }
        }
    }

    List<ExternallyProcessedMergedBamFile> returnExternallyProcessedMergedBamFiles(List<SeqTrack> seqTracks) {
        notNull(seqTracks, "The input of returnExternallyProcessedMergedBamFiles is null")
        assert !seqTracks.empty: "The input list of returnExternallyProcessedMergedBamFiles is empty"

        return seqTracks.collect { val ->
            CollectionUtils.atMostOneElement(
                    ExternallyProcessedMergedBamFile.createCriteria().list {
                        workPackage {
                            eq('sample', val.sample)
                            eq('seqType', val.seqType)
                        }
                    }
            )
        }.unique().findAll()
    }

    SeqTrack seqTrackReadyToInstall(ProcessingPriority minPriority) {
        return SeqTrack.createCriteria().get {
            eq('dataInstallationState', SeqTrack.DataProcessingState.NOT_STARTED)
            sample {
                individual {
                    project {
                        ge('processingPriority', minPriority.priority)
                        order('processingPriority', 'desc')
                    }
                }
            }
            order('id')
            maxResults(1)
        }
    }

    static void logToSeqTrack(SeqTrack seqTrack, String message, boolean saveInSeqTrack = true) {
        threadLog?.info(MessageFormat.format(message, " " + seqTrack))
        if (saveInSeqTrack) {
            seqTrack.save(flush: true)
            SeqTrack.withTransaction {
                LogMessage logMessage = new LogMessage(message: MessageFormat.format(message, ""))
                logMessage.save(flush: true)
                seqTrack.logMessages.add(logMessage)
                seqTrack.save(flush: true)
            }
        }
    }
}
