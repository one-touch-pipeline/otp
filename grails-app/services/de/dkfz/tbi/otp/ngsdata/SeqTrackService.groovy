package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AbstractAlignmentDecider
import de.dkfz.tbi.otp.dataprocessing.AlignmentDecider
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.FastqSet
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails

import static org.springframework.util.Assert.*

class SeqTrackService {

    def fileTypeService
    /**
     * Dependency Injection of Project Service.
     *
     * Needed for access control on data protected by Projects.
     */
    def projectService
    /**
     * Dependency Injection of LSDF File Service.
     *
     * Required for exporting the view by pid path of a datafile
     */
    def lsdfFilesService
    /**
     * Dependency Injection of Spring Security Service.
     */
    def springSecurityService

    @Autowired
    ApplicationContext applicationContext

    MultiplexingService multiplexingService

    LibraryPreparationKitService libraryPreparationKitService

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
    public List<Sequence> listSequences(int offset, int max, boolean sortOrder, SequenceSortColumn column, SequenceFiltering filtering) {
        String columnName = "projectId"
        if (filtering.enabled) {
            def c = Sequence.createCriteria()
            return c.list {
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
                    'in'('seqTypeAliasOrName', filtering.seqType)
                }
                if (filtering.libraryLayout) {
                    'in'('libraryLayout', filtering.libraryLayout)
                }
                if (filtering.seqCenter) {
                    'in'('seqCenterId', filtering.seqCenter)
                }
                if (filtering.run) {
                    or {
                        filtering.run.each {
                            ilike('name', "%${it}%")
                        }
                    }
                }
                if (max != -1) { //-1 indicate in jquery datatable, that no paging is used. Therefore in that case no maxResult are set
                    maxResults(max)
                }
                firstResult(offset)
                order(column.columnName, sortOrder ? "asc" : "desc")
            }
        } else {
            return Sequence.findAllByProjectIdInList(projectService.getAllProjects().collect { it.id }, [offset: offset, max: max, sort: column.columnName, order: sortOrder ? "asc" : "desc"])
        }
    }

    /**
     * Counts the Sequences the User has access to by applying the provided filtering.
     * @param filtering The filters to apply on the data
     * @return Number of Sequences matching the filtering
     */
    public int countSequences(SequenceFiltering filtering) {
        if (filtering.enabled) {
            def c = Sequence.createCriteria()
            return c.get {
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
                    'in'('seqTypeAliasOrName', filtering.seqType)
                }
                if (filtering.libraryLayout) {
                    'in'('libraryLayout', filtering.libraryLayout)
                }
                if (filtering.seqCenter) {
                    'in'('seqCenterId', filtering.seqCenter)
                }
                if (filtering.run) {
                    or {
                        filtering.run.each {
                            ilike('name', "%${it}%")
                        }
                    }
                }
                projections { count('mockPid') }
            }
        } else {
            // shortcut for unfiltered results
            return Sequence.countByProjectIdInList(projectService.getAllProjects().collect { it.id })
        }
    }



    /**
     * Calls the {@link AlignmentDecider#decideAndPrepareForAlignment(SeqTrack, boolean)} method of the
     * {@link AlignmentDecider} specified by the {@link Project#alignmentDeciderBeanName} property of the specified
     * {@link SeqTrack}'s {@link Project}.
     */
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign = false) {
        AlignmentDecider decider = getAlignmentDecider(seqTrack.project)
        return decider.decideAndPrepareForAlignment(seqTrack, forceRealign)
    }

    AlignmentDecider getAlignmentDecider(Project project) {
        String alignmentDeciderBeanName = project.alignmentDeciderBeanName
        if (!alignmentDeciderBeanName) {
            // The validator should prevent this, but there are ways to circumvent the validator.
            throw new RuntimeException("alignmentDeciderBeanName is not set for project ${project}. (In case no alignment shall be done for that project, set the alignmentDeciderBeanName to noAlignmentDecider, which is an AlignmentDecider which decides not to align.)")
        }
        return applicationContext.getBean(alignmentDeciderBeanName, AlignmentDecider)
    }

    static boolean mayAlign(SeqTrack seqTrack) {

        def notAligning = { String reason -> AbstractAlignmentDecider.logNotAligning(seqTrack, reason) }

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

        if (seqTrack.seqPlatform.seqPlatformGroup == null) {
            notAligning("seqPlatformGroup is null for ${seqTrack.seqPlatform}")
            return false
        }

        return true
    }

    public void setRunReadyForFastqc(Run run) {
        def unknown = SeqTrack.DataProcessingState.UNKNOWN
        SeqTrack.findAllByRunAndFastqcState(run, unknown).each { SeqTrack seqTrack ->
            if (fastqcReady(seqTrack)) {
                seqTrack.fastqcState = SeqTrack.DataProcessingState.NOT_STARTED
                assert(seqTrack.save(flush: true))
            }
        }
    }

    private boolean fastqcReady(SeqTrack track) {
        List<DataFile> files = DataFile.findAllBySeqTrack(track)
        for (DataFile file in files) {
            if (!fileTypeService.fastqcReady(file)) {
                return false
            }
        }
        return true
    }

    /**
     * returns the oldest alignable {@link SeqTrack} waiting for fastqc if possible,
     * otherwise the oldest {@link SeqTrack} waiting waiting for fastqc.
     *
     * @return a seqTrack without fastqc
     * @see SeqTypeService#alignableSeqTypes
     */
    public SeqTrack getSeqTrackReadyForFastqcProcessingPreferAlignable() {
        return getSeqTrackReadyForFastqcProcessing(true) ?: getSeqTrackReadyForFastqcProcessing()
    }

    /**
     * returns the oldest {@link SeqTrack} waiting for fastqc.
     *
     * @param onlyAlignable if true, only alignable {@link SeqTrack}s are searched, else all {@link SeqTrack}s.
     * @return a seqTrack without fastqc
     * @see SeqTypeService#alignableSeqTypes
     */
    public SeqTrack getSeqTrackReadyForFastqcProcessing(boolean onlyAlignable = false) {
        return SeqTrack.createCriteria().get {
            eq('fastqcState', SeqTrack.DataProcessingState.NOT_STARTED)
            if (onlyAlignable) {
                seqType {
                    'in'('id', SeqTypeService.alignableSeqTypes()*.id)
                }
            }
            order('id', 'asc')
            maxResults(1)
        }
    }

    public void setFastqcInProgress(SeqTrack seqTrack) {
        seqTrack.fastqcState = SeqTrack.DataProcessingState.IN_PROGRESS
        assert(seqTrack.save(flush: true))
    }

    public void setFastqcFinished(SeqTrack seqTrack) {
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert(seqTrack.save(flush: true))
    }

    public List<DataFile> getSequenceFilesForSeqTrack(SeqTrack seqTrack) {
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        List<DataFile> filteredFiles = []
        files.each {
            if (fileTypeService.isGoodSequenceDataFile(it)) {
                filteredFiles.add(it)
            }
        }
        return filteredFiles
    }

    public Run getRunReadyForFastqcSummary() {
        List<Run> runsInProgress = getCandidatesForFastqcSummary()
        for (Run run in runsInProgress) {
            if (isRunReadyForFastqcSummary(run)) {
                return run
            }
        }
        return null
    }

    private List<Run> getCandidatesForFastqcSummary() {
        List<SeqTrack> tracks = SeqTrack.withCriteria {
            and {
                eq("fastqcState", SeqTrack.DataProcessingState.FINISHED)
                run { eq("qualityEvaluated", false) }
            }
        }
        return tracks*.run
    }

    private boolean isRunReadyForFastqcSummary(Run run) {
        int nFinished = SeqTrack.countByRunAndFastqcState(run, SeqTrack.DataProcessingState.FINISHED)
        int nTotal = SeqTrack.countByRun(run)
        return (nTotal == nFinished)
    }

    /**
     *
     * A sequence track corresponds to one lane in Illumina
     * and one slide in Solid.
     *
     * This method build sequence tracks for a given run.
     * To each sequence track there are raw data and alignment
     * data attached
     *
     * @param runId
     */
    public void buildSequenceTracks(long runId) {
        Run run = Run.get(runId)
        Set<String> lanes = getSetOfLanes(run)
        lanes.each{String laneId ->
            buildOneSequenceTrack(run, laneId)
        }
    }

    private Set<String> getSetOfLanes(Run run) {
        MetaDataKey key = MetaDataKey.findByName("LANE_NO")
        Set<String> lanes = new HashSet<String>()
        DataFile.findAllByRun(run).each { DataFile dataFile ->
            if (!fileTypeService.isGoodSequenceDataFile(dataFile)) {
                return
            }
            MetaDataEntry laneValue = MetaDataEntry.findByDataFileAndKey(dataFile, key)
            if (!laneValue) {
                throw new LaneNotDefinedException(run.name, dataFile.fileName)
            }
            lanes << laneValue.value
        }
        return lanes
    }

    /**
     * builds one sequence track (SeqTrack).
     * If the seqTrack for a given run and given lane already exists
     * it is used and only alignment files could be attached.
     * Once the seqTrack is created it is not possible to add sequence files to it.
     *
     * @param run Run for which sequence track is build
     * @param lane Lane identifier
     */
    private void buildOneSequenceTrack(Run run, String lane) {
        SeqTrack seqTrack = SeqTrack.findByRunAndLaneId(run, lane)
        if (!seqTrack) {
            seqTrack = buildFastqSeqTrack(run, lane)
        }
        appendAlignmentToSeqTrack(seqTrack)
    }

    private SeqTrack buildFastqSeqTrack(Run run, String lane) {
        // find sequence files
        List<DataFile> dataFiles =
                        getRunFilesWithTypeAndLane(run, FileType.Type.SEQUENCE, lane)
        if (!dataFiles) {
            throw new ProcessingException("No laneDataFiles found.")
        }
        Sample sample = getSample(dataFiles.get(0))
        assertConsistentSample(sample, dataFiles)

        SeqType seqType = getSeqType(dataFiles.get(0))
        assertConsistentSeqType(seqType, dataFiles)

        assertConsistentLibraryPreparationKit(dataFiles)

        SoftwareTool pipeline = getPipeline(dataFiles.get(0))
        assertConsistentPipeline(pipeline, dataFiles)

        String ilseId = assertAndReturnConcistentIlseId(dataFiles)

        SeqTrack seqTrack = createSeqTrack(dataFiles.get(0), run, sample, seqType, lane, pipeline, ilseId)
        consumeDataFiles(dataFiles, seqTrack)
        fillReadsForSeqTrack(seqTrack)
        assert seqTrack.save(failOnError: true, flush: true)

        boolean willBeAligned = decideAndPrepareForAlignment(seqTrack)
        determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, willBeAligned)

        return seqTrack
    }

    private SeqTrack createSeqTrack(DataFile dataFile, Run run, Sample sample, SeqType seqType, String lane, SoftwareTool pipeline, String ilseId = null) {
        SeqTrackBuilder builder = new SeqTrackBuilder(lane, run, sample, seqType, run.seqPlatform, pipeline, ilseId)
        builder.setHasFinalBam(false).setHasOriginalBam(false).setUsingOriginalBam(false)

        extractAndSetLibraryPreparationKit(dataFile, builder, run, sample)

        /*
         * There is one special case which needs a specific treatment.
         * For all other cases the default suffices.
         */
        if (seqType.name == SeqTypeNames.CHIP_SEQ.seqTypeName) {
            annotateSeqTrackForChipSeq(dataFile, builder)
        }

        SeqTrack seqTrack = builder.create()
        seqTrack.save(flush: true)
        return seqTrack
    }


    private void extractAndSetLibraryPreparationKit(DataFile dataFile, SeqTrackBuilder builder, Run run, Sample sample) {
        notNull(dataFile, "The input dataFile of the method annotateSeqTrackForExome is null")
        notNull(builder, "The input builder of the method annotateSeqTrackForExome is null")
        notNull(run, "The input run of the method annotateSeqTrackForExome is null")
        notNull(sample, "The input sample of the method annotateSeqTrackForExome is null")

        MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.LIB_PREP_KIT.name())
        MetaDataEntry metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
        if (metaDataEntry == null) {
            builder.setInformationReliability(InformationReliability.UNKNOWN_UNVERIFIED)
        } else if (!metaDataEntry.value) {
            assert builder.seqType.name != SeqTypeNames.EXOME.seqTypeName
            builder.setInformationReliability(InformationReliability.UNKNOWN_UNVERIFIED)
        } else if (metaDataEntry.value == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
            assert builder.seqType.name == SeqTypeNames.EXOME.seqTypeName
            builder.setInformationReliability(InformationReliability.UNKNOWN_VERIFIED)
        } else {
            LibraryPreparationKit libraryPreparationKit = libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(metaDataEntry.value)
            notNull(libraryPreparationKit, "There is no LibraryPreparationKit in the DB for the metaDataEntry ${metaDataEntry.value} of run ${run}")
            builder.setLibraryPreparationKit(libraryPreparationKit)
        }
    }


    private void annotateSeqTrackForChipSeq(DataFile dataFile, SeqTrackBuilder builder) {
        notNull(dataFile, "The input dataFile of method annotateSeqTrackForChipSeq is null")
        notNull(builder, "The input builder of the method annotateSeqTrackForChipSeq is null")
        MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.ANTIBODY_TARGET.name())
        MetaDataEntry metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
        notNull(metaDataEntry, "There is no metaDataEntry for the key " + key + " and the file " + dataFile)
        builder.setAntibodyTarget(AntibodyTarget.findByName(metaDataEntry.value))
        key = MetaDataKey.findByName(MetaDataColumn.ANTIBODY.name())
        metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
        if (metaDataEntry) {
            builder.setAntibody(metaDataEntry.value)
        }
    }

    private Sample getSample(DataFile file) {
        String sampleName = metaDataValue(file, "SAMPLE_ID")
        SampleIdentifier idx = SampleIdentifier.findByName(sampleName)
        return idx.sample
    }

    private void assertConsistentSample(Sample sample, List<DataFile> files) {
        for(DataFile file in files) {
            Sample fileSample = getSample(file)
            if (!sample.equals(fileSample)) {
                throw new SampleInconsistentException(files, sample, fileSample)
            }
        }
    }

    private SeqType getSeqType(DataFile file) {
        String type = metaDataValue(file, "SEQUENCING_TYPE")
        String layout = metaDataValue(file, "LIBRARY_LAYOUT")
        SeqType seqType = SeqType.findByNameAndLibraryLayout(type, layout)
        if (!seqType) {
            throw new SeqTypeNotDefinedException(type, layout)
        }
        return seqType
    }

    private void assertConsistentSeqType(SeqType seqType, List<DataFile> files) {
        for(DataFile file in files) {
            SeqType fileSeqType = getSeqType(file)
            if (!seqType.equals(fileSeqType)) {
                throw new MetaDataInconsistentException(files, seqType.name, fileSeqType.name)
            }
        }
    }

    private void assertConsistentLibraryPreparationKit(List<DataFile> files) {
        assertConsistentWithinSeqTrack(files, MetaDataColumn.LIB_PREP_KIT)
    }

    private String assertAndReturnConcistentIlseId(List<DataFile> files) {
        return assertConsistentWithinSeqTrack(files, MetaDataColumn.ILSE_NO)
    }

    private String assertConsistentWithinSeqTrack(List<DataFile> files, MetaDataColumn metaDataColumn) {
        List<String> values = files.collect { DataFile dataFile ->
            MetaDataKey key = MetaDataKey.findByName(metaDataColumn.name())
            MetaDataEntry metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
            return metaDataEntry?.value
        }
        String value = values.first()
        if (!values.every { it == value }) {
            throw new ProcessingException("Not using the same ${metaDataColumn.name()} (files: ${files*.fileName})")
        }
        return value
    }



    private SoftwareTool getPipeline(DataFile file) {
        String name = metaDataValue(file, "PIPELINE_VERSION")
        List<SoftwareToolIdentifier> idx = SoftwareToolIdentifier.findAllByName(name)
        for(SoftwareToolIdentifier si in idx) {
            if (si.softwareTool.type == SoftwareTool.Type.BASECALLING) {
                return si.softwareTool
            }
        }
        return null
    }

    private void assertConsistentPipeline(SoftwareTool pipeline, List<DataFile> files) {
        for (DataFile file in files) {
            SoftwareTool filePipeline = getPipeline(file)
            if (!pipeline.equals(filePipeline)) {
                throw new MetaDataInconsistentException(files, pipeline.programName, filePipeline.programName)
            }
        }
    }

    private void consumeDataFiles(List<DataFile> files, SeqTrack seqTrack) {
        files.each {DataFile dataFile ->
            dataFile.seqTrack = seqTrack
            dataFile.project = seqTrack.project
            dataFile.used = true
            dataFile.save(flush: true)
        }
    }

    /**
     *
     * Fills the numbers in the SeqTrack object using MetaDataEntry
     * objects from the DataFile objects belonging to this SeqTrack.
     *
     * @param seqTrack
     * @return manipulated seqTrack
     */
    private void fillReadsForSeqTrack(SeqTrack seqTrack) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        fillInsertSize(seqTrack, dataFiles)
        fillReadCount(seqTrack, dataFiles)
        fillBaseCount(seqTrack, dataFiles)
    }

    private void fillInsertSize(SeqTrack seqTrack, List<DataFile> files) {
        seqTrack.insertSize = metaDataLongValue(files.get(0), "INSERT_SIZE")
    }

    private void fillReadCount(SeqTrack seqTrack, List<DataFile> files) {
        seqTrack.nReads = 0
        for(DataFile file in files) {
            seqTrack.nReads += metaDataLongValue(file, "READ_COUNT")
        }
    }

    private void fillBaseCount(SeqTrack seqTrack, List<DataFile> files) {
        seqTrack.nBasePairs = 0
        for(DataFile file in files) {
            seqTrack.nBasePairs += metaDataLongValue(file, "BASE_COUNT")
        }
    }

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


        for(SoftwareTool pipeline in pipelines) {

            AlignmentParams alignParams = getAlignmentParams(pipeline)
            AlignmentLog alignLog = new AlignmentLog(
                            alignmentParams : alignParams,
                            seqTrack : seqTrack,
                            executedBy : AlignmentLog.Execution.INITIAL
                            )
            alignLog.save(flush: true)
            consumeAlignmentFiles(alignLog, alignFiles, pipeline)
            seqTrack.hasOriginalBam = true
            alignLog.save()
            alignParams.save()
        }
        seqTrack.save(flush: true)
    }

    Set<SoftwareTool> getAlignmentPipelineSet(List<DataFile> alignFiles) {
        Set<SoftwareTool> set = new HashSet<SoftwareTool>()
        for(DataFile file in alignFiles) {
            SoftwareTool pipeline = getAlignmentPipeline(file)
            set << pipeline
        }
        return set
    }

    SoftwareTool getAlignmentPipeline(DataFile file) {
        String name = metaDataValue(file, "ALIGN_TOOL")
        List<SoftwareToolIdentifier> idx = SoftwareToolIdentifier.findAllByName(name)
        for(SoftwareToolIdentifier si in idx) {
            if (si.softwareTool.type == SoftwareTool.Type.ALIGNMENT) {
                return si.softwareTool
            }
        }
        return null
    }

    private AlignmentParams getAlignmentParams(SoftwareTool pipeline) {
        AlignmentParams alignParams = AlignmentParams.findByPipeline(pipeline)
        if (!alignParams) {
            alignParams = new AlignmentParams(pipeline: pipeline)
            alignParams.save(flush: true)
        }
        return alignParams
    }

    private void consumeAlignmentFiles(AlignmentLog alignLog, List<DataFile> files, SoftwareTool pipeline) {
        for(DataFile file in files) {
            SoftwareTool filePipeline = getAlignmentPipeline(file)
            if (pipeline.equals(filePipeline)) {
                file.project = alignLog.seqTrack.project
                file.alignmentLog = alignLog
                file.used = true
                file.save(flush: true)
            }
        }
    }

    /**
     * Return all dataFiles for a given run, type and lane
     * Only dataFiles which are not used are returned
     *
     * @param run The Run
     * @param type The Type
     * @param lane The lane
     * @return
     */
    private def getRunFilesWithTypeAndLane(Run run, FileType.Type type, String lane) {
        MetaDataKey key = MetaDataKey.findByName("LANE_NO")
        def dataFiles = DataFile.executeQuery('''
SELECT dataFile FROM MetaDataEntry as entry
INNER JOIN entry.dataFile as dataFile
WHERE
dataFile.run = :run
AND dataFile.fileWithdrawn = false
AND dataFile.fileType.type = :type
AND dataFile.used = false
AND entry.key = :key
AND entry.value = :value
''',
                        [run: run, type: type, key: key, value: lane])
        return dataFiles
    }


    /**
     *
     * checks if all sequence (raw and aligned) data files are attached
     * to SeqTrack objects. If yes the field DataFile.used field is set to true.
     * if data file is a sequence or alignment type and is
     * not used it contains errors in meta-data
     *
     * @param runId
     */
    public boolean checkSequenceTracks(long runId) {
        Run run = Run.get(runId)
        List<RunSegment> segments = RunSegment.findAllByRun(run)
        boolean allUsed = true
        for (RunSegment segment in segments) {
            segment.allFilesUsed = true
            List<DataFile> files = DataFile.findAllByRunSegment(segment)
            for (DataFile file in files) {
                if (fileTypeService.isRawDataFile(file)) {
                    if (!file.used) {
                        segment.allFilesUsed = false
                        allUsed = false
                        LogThreadLocal.getThreadLog()?.error("Datafile " + file + " is not used in a seq track" +
                                        (file.fileWithdrawn ? " (reason: withdrawn)": ""))
                    }
                }
            }
            segment.save(flush: true)
        }
        return allUsed
    }

    private MetaDataEntry metaDataEntry(DataFile file, String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(file, key)
        if (!entry) {
            throw new ProcessingException("no entry for key: ${keyName}")
        }
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }

    private String metaDataValue(DataFile file, String keyName) {
        MetaDataEntry entry = metaDataEntry(file, keyName)
        return entry.value
    }

    private long metaDataLongValue(DataFile file, String keyName) {
        MetaDataEntry entry = metaDataEntry(file, keyName)
        if (entry.value.isLong()) {
            return entry.value as long
        }
        return 0
    }

    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.sample.individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
    public SeqTrack getSeqTrack(String identifier) {
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
     * Retrieves the previous SeqTrack by database id if present.
     * @param seqTrack The SeqTrack for which the predecessor has to be retrieved
     * @return Previous SeqTrack if present, otherwise null
     **/
    @PreAuthorize("(returnObject == null) or hasPermission(#seqTrack.sample.individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
    SeqTrack previousSeqTrack(SeqTrack seqTrack) {
        if (!seqTrack) {
            return null
        }
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
            // shortcut for operator
            return SeqTrack.findByIdLessThan(seqTrack.id, [sort: "id", order: "desc"])
        }
        // for normal users
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }
        String query = '''
SELECT MAX(i.id) FROM SeqTrack AS st, AclEntry AS ace
JOIN st.sample AS s
JOIN s.individual AS i
JOIN i.project AS p
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = p.id
AND ac.className = :className
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
AND i.id < :seqTrackId
'''
        Map params = [
            className: Project.class.getName(),
            permissions: [
                BasePermission.READ.getMask(),
                BasePermission.ADMINISTRATION.getMask()
            ],
            roles: roles,
            seqTrackId: seqTrack.id
        ]
        List result = Individual.executeQuery(query, params)
        if (!result) {
            return null
        }
        return SeqTrack.get(result[0] as Long)
    }

    /**
     * Retrieves the next SeqTrack by database id if present.
     * @param seqTrack The SeqTrack for which the successor has to be retrieved
     * @return Next SeqTrack if present, otherwise null
     **/
    @PreAuthorize("(returnObject == null) or hasPermission(#seqTrack.sample.individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
    SeqTrack nextSeqTrack(SeqTrack seqTrack) {
        if (!seqTrack) {
            return null
        }
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
            // shortcut for operator
            return SeqTrack.findByIdGreaterThan(seqTrack.id, [sort: "id", order: "asc"])
        }
        // for normal users
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }
        String query = '''
SELECT MIN(i.id) FROM SeqTrack AS st, AclEntry AS ace
JOIN st.sample AS s
JOIN s.individual AS i
JOIN i.project AS p
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = p.id
AND ac.className = :className
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
AND i.id > :seqTrackId
'''
        Map params = [
            className: Project.class.getName(),
            permissions: [
                BasePermission.READ.getMask(),
                BasePermission.ADMINISTRATION.getMask()
            ],
            roles: roles,
            seqTrackId: seqTrack.id
        ]
        List result = SeqTrack.executeQuery(query, params)
        if (!result) {
            return null
        }
        return SeqTrack.get(result[0] as Long)
    }

    /**
     * This method determines if a fastq file has to be linked or copied to the project folder and stores the information in the seqTrack.
     * If a fastq file fulfills the following constraints it has to be linked:
     * - provided by GPCF
     * - provided via the midterm storage
     * - will be aligned
     * - hasToBeCopied flag is set to false
     */
    void determineAndStoreIfFastqFilesHaveToBeLinked(SeqTrack seqTrack, boolean willBeAligned) {
        assert seqTrack : "The input seqTrack for determineAndStoreIfFastqFilesHaveToBeLinked must not be null"
        SeqCenter core = CollectionUtils.exactlyOneElement(SeqCenter.findAllByName("DKFZ"))
        if ( willBeAligned &&
                seqTrack.run.seqCenter == core &&
                !seqTrack.project.hasToBeCopied &&
                areFilesLocatedOnMidTermStorage(seqTrack)) {
            seqTrack.linkedExternally = true
            assert seqTrack.save(flush: true)
        }
    }

    private boolean areFilesLocatedOnMidTermStorage(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack for areFilesLocatedOnMidTermStorage must not be null"
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        RunSegment runSegment = CollectionUtils.exactlyOneElement( files*.runSegment.unique() )
        return LsdfFilesService.midtermStorageMountPoint.any{runSegment.dataPath.startsWith(it)}
    }



    List<ExternallyProcessedMergedBamFile> returnExternallyProcessedMergedBamFiles(List<SeqTrack> seqTracks) {
        notNull(seqTracks, "The input of returnExternallyProcessedMergedBamFiles is null")
        assert !seqTracks.empty : "The input list of returnExternallyProcessedMergedBamFiles is empty"

        return ExternallyProcessedMergedBamFile.executeQuery(
"""
select bamFile
from ExternallyProcessedMergedBamFile bamFile
join bamFile.fastqSet fastqSet
join fastqSet.seqTracks seqTracks
where seqTracks in (:seqTrackList)
""", ["seqTrackList": seqTracks])

    }

}
