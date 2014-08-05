package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AlignmentPassService
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

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

    MultiplexingService multiplexingService

    ExomeEnrichmentKitService exomeEnrichmentKitService

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
                    'in'('seqTypeName', filtering.seqType)
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
                maxResults(max)
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
                    'in'('seqTypeName', filtering.seqType)
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
     * Performs an export to XML with the applied filtering.
     * @param filtering
     * @return
     */
    public String performXMLExport(SequenceFiltering filtering) {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)

        def c = Sequence.createCriteria()
        List<Sequence> sequences = c.list {
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
                'in'('seqTypeName', filtering.seqType)
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
        }
        String dataFileQuery = "SELECT df FROM DataFile AS df INNER JOIN df.seqTrack AS s WHERE s.id = :seqTrackId"

        builder.sequences() {
            sequences.each { Sequence p ->
                sequence() {
                    seqTrack(id: p.seqTrackId, finalBam: p.hasFinalBam, originalBam: p.hasOriginalBam, usingOriginalBam: p.usingOriginalBam) {
                        laneId(p.laneId)
                        numberBasePairs(p.nBasePairs)
                        numberReads(p.nReads)
                        insertSize(p.insertSize)
                        qualityEncoding(p.qualityEncoding)
                        alignmentState(p.alignmentState)
                        fastqcState(p.fastqcState)
                    }
                    run(id: p.runId, blacklisted: p.blacklisted, multipleSource: p.multipleSource, qualityEvaluated: p.qualityEvaluated) {
                        name(p.name)
                        dateExecuted(p.dateExecuted)
                        dateCreated(p.dateCreated)
                        storageRealm(p.storageRealm)
                        dataQuality(p.dataQuality)
                    }
                    seqPlatform(id: p.seqPlatformId) {
                        name(p.seqPlatformName)
                        model(p.model)
                    }
                    seqType(id: p.seqTypeId) {
                        name(p.seqTypeName)
                        libraryLayout(p.libraryLayout)
                        dirName(p.dirName)
                    }
                    individual(id: p.individualId) {
                        pid(p.pid)
                        mockPid(p.mockPid)
                        mockFullName(p.mockFullName)
                        type(p.type)
                        sample(id: p.sampleId) {
                            sampleType(id: p.sampleTypeId, p.sampleTypeName)
                        }
                    }
                    project(id: p.projectId) {
                        name(p.projectName)
                        dirName(p.projectDirName)
                        realm(p.realmName)
                    }
                    seqCenter(id: p.seqCenterId) {
                        name(p.seqCenterName)
                        dirName(p.seqCenterDirName)
                    }
                    dataFiles() {
                        DataFile.executeQuery(dataFileQuery, [seqTrackId: p.seqTrackId]).each { DataFile df ->
                            path(lsdfFilesService.getFileViewByPidPath(df, p))
                        }
                    }
                }
            }
        }
        return writer.toString()
    }

    /**
     * Sets all {@link SeqTrack}s fulfilling the criteria listed below to alignment state
     * {@link SeqTrack.DataProcessingState#NOT_STARTED}.
     *
     * <p>Critieria for the SeqTrack:</p>
     * <ul>
     *   <li>It has been done for a sample which has been sequenced in the specified run. (This also
     *       includes <code>SeqTrack</code>s from other runs, as long as they are done for such a
     *       sample.)</li>
     *   <li>It is alignable as defined here: {@link AlignmentPassService#ALIGNABLE_SEQTRACK_HQL}</li>
     *   <li>It is in alignment state {@link SeqTrack.DataProcessingState#UNKNOWN}.</li>
     *   <li>It has sequence type WHOLE_GENOME or EXOME and library layout PAIRED.</li>
     *   <li>It has a corresponding {@link RunSegment} where align is set to true .</li>
     *   <li>For {@link SeqType} Exome, the BED file must be available. </li>
     * </ul>
     *
     * @see AlignmentPassService#findAlignableSeqTrack()
     */
    public void setRunReadyForAlignment(Run run) {
        notNull(run, "The run argument must not be null.")

        // List of seqType names which have to be aligned
        List<String> seqTypesToAlign = [
            SeqTypeNames.WHOLE_GENOME.seqTypeName,
            SeqTypeNames.EXOME.seqTypeName
        ]

        // Find all samples in this run.
        List<SeqTrack> seqTracksPerRun = SeqTrack.withCriteria {
            eq("run", run)
            seqType {
                'in'("name", seqTypesToAlign)
                eq("libraryLayout", "PAIRED")
            }
        }

        seqTracksPerRun.each { SeqTrack seqTrack ->
            // Find all SeqTracks belonging to the same sample and seqType as the seqTrack in the run which can be aligned.
            List<SeqTrack> seqTracks = SeqTrack.findAll(AlignmentPassService.ALIGNABLE_SEQTRACK_HQL +
                            "AND seqType.name =:seqTypeOfSeqTrack AND seqType.libraryLayout = :seqTypeLibraryLayout " +
                            "AND sample =:sampleOfSeqTrack " +
                            "AND NOT EXISTS (FROM DataFile WHERE seqTrack = st AND runSegment.align = false)",
                            [
                                alignmentState: SeqTrack.DataProcessingState.UNKNOWN,
                                seqTypeOfSeqTrack: seqTrack.seqType.name,
                                seqTypeLibraryLayout: "PAIRED",
                                sampleOfSeqTrack: seqTrack.sample,
                            ] << AlignmentPassService.ALIGNABLE_SEQTRACK_QUERY_PARAMETERS)

            // ExomeSeqTracks with the kitInfoState "UNKNOWN_VERIFIED" and no kit must not be aligned.
            seqTracks = seqTracks.findAll {
                !(it instanceof ExomeSeqTrack) ||
                                it.exomeEnrichmentKit ||
                                it.kitInfoReliability == InformationReliability.UNKNOWN_UNVERIFIED
            }

            // Mark the SeqTracks as being ready for alignment.
            seqTracks.each {
                it.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
                it.save(flush: true)
            }
        }
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

    public SeqTrack getSeqTrackReadyForFastqcProcessing() {
        return SeqTrack.findByFastqcState(SeqTrack.DataProcessingState.NOT_STARTED)
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

        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            assertConsistentLibraryPreparationKit(dataFiles)
        }

        SoftwareTool pipeline = getPipeline(dataFiles.get(0))
        assertConsistentPipeline(pipeline, dataFiles)

        SeqTrack seqTrack = createSeqTrack(dataFiles.get(0), run, sample, seqType, lane, pipeline)
        consumeDataFiles(dataFiles, seqTrack)
        fillReadsForSeqTrack(seqTrack)
        seqTrack.save(flush: true)
        return seqTrack
    }

    private SeqTrack createSeqTrack(DataFile dataFile, Run run, Sample sample, SeqType seqType, String lane, SoftwareTool pipeline) {
        SeqTrackBuilder builder = new SeqTrackBuilder(lane, run, sample, seqType, run.seqPlatform, pipeline)
        builder.setHasFinalBam(false).setHasOriginalBam(false).setUsingOriginalBam(false)

        /*
         * These are two special cases, which need a specific treatment.
         * For all other cases the default suffices -> no else is needed.
         */
        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            annotateSeqTrackForExome(dataFile, builder, run, sample)
        } else if (seqType.name == SeqTypeNames.CHIP_SEQ.seqTypeName) {
            annotateSeqTrackForChipSeq(dataFile, builder)
        }

        SeqTrack seqTrack = builder.create()
        seqTrack.save(flush: true)
        return seqTrack
    }


    private void annotateSeqTrackForExome(DataFile dataFile, SeqTrackBuilder builder, Run run, Sample sample) {
        notNull(dataFile, "The input dataFile of the method annotateSeqTrackForExome is null")
        notNull(builder, "The input builder of the method annotateSeqTrackForExome is null")
        notNull(run, "The input run of the method annotateSeqTrackForExome is null")
        notNull(sample, "The input sample of the method annotateSeqTrackForExome is null")

        MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.LIB_PREP_KIT.name())
        MetaDataEntry metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
        if (metaDataEntry == null) {
            builder.setInformationReliability(InformationReliability.UNKNOWN_UNVERIFIED)
        } else if (metaDataEntry.value == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
            builder.setInformationReliability(InformationReliability.UNKNOWN_VERIFIED)
        } else {
            ExomeEnrichmentKit exomeEnrichmentKit = exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(metaDataEntry.value)
            notNull(exomeEnrichmentKit, "There is no EnrichmentKit in the DB for the metaDataEntry ${metaDataEntry.value} of run ${run}")
            exomeEnrichmentKitService.validateExomeEnrichmentKit(sample, exomeEnrichmentKit)
            builder.setExomeEnrichmentKit(exomeEnrichmentKit)
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
                throw new MetaDataInconsistentException(files, seqType, fileSeqType)
            }
        }
    }

    private void assertConsistentLibraryPreparationKit(List<DataFile> files) {
        List<String> libraryPreparationKits = files.collect { DataFile dataFile ->
            MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.LIB_PREP_KIT.name())
            MetaDataEntry metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
            return metaDataEntry?.value
        }
        String libraryPreparationKit = libraryPreparationKits.first()
        if (!libraryPreparationKits.every { it == libraryPreparationKit }) {
            throw new ProcessingException("Not using the same library preparation kit (files: ${files*.fileName})")
        }
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
        for(DataFile file in file) {
            SoftwareTool filePipeline = getPipeline(file)
            if (!pipeline.equals(filePipeline)) {
                throw new MetaDataInconsistentException(files, pipeline, filePipeline)
            }
        }
    }

    private void consumeDataFiles(List<DataFile> files, SeqTrack seqTrack) {
        files.each {DataFile dataFile ->
            dataFile.seqTrack = seqTrack
            dataFile.project = seqTrack.sample.individual.project
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
                file.project = alignLog.seqTrack.sample.individual.project
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
}
