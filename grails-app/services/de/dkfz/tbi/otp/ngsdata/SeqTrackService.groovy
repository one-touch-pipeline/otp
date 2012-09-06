package de.dkfz.tbi.otp.ngsdata


import de.dkfz.tbi.otp.job.processing.ProcessingException

class SeqTrackService {

    def fileTypeService

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
                run {
                    eq("qualityEvaluated", false)
                }
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

        SoftwareTool pipeline = getPipeline(dataFiles.get(0))
        assertConsistentPipeline(pipeline, dataFiles)

        SeqTrack seqTrack = new SeqTrack(
            run : run,
            sample : sample,
            seqType : seqType,
            seqPlatform : run.seqPlatform,
            laneId : lane,
            hasFinalBam : false,
            hasOriginalBam : false,
            usingOriginalBam : false,
            pipelineVersion: pipeline
        )
        if (!seqTrack.validate()) {
            println seqTrack.errors
            throw new ProcessingException("seqTrack could not be validated.")
        }
        seqTrack.save(flush: true)
        consumeDataFiles(dataFiles, seqTrack)
        fillReadsForSeqTrack(seqTrack)
        seqTrack.save(flush: true)
        return seqTrack
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
    public void checkSequenceTracks(long runId) {
        Run run = Run.get(runId)
        List<RunSegment> segments = RunSegment.findAllByRun(run)
        for(RunSegment segment in segments) {
            segment.allFilesUsed = true
            List<DataFile> files = DataFile.findAllByRunSegment(segment)
            for(DataFile file in files) {
                if (fileTypeService.isRawDataFile(file)) {
                    if (!file.used) {
                        segment.allFilesUsed = false
                    }
                }
            }
            segment.save(flush: true)
        }
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
}
