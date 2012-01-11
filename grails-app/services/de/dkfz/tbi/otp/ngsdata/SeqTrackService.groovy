package de.dkfz.tbi.otp.ngsdata

class SeqTrackService {
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
    void buildSequenceTracks(long runId) {
        Run run = Run.get(runId)
        // find out present lanes/slides
        // lines/ slides could by identifiers not only numbers
        MetaDataKey key = MetaDataKey.findByName("LANE_NO")
        Set<String> lanes = new HashSet<String>()
        // get the list of unique lanes identifiers
        DataFile.findAllByRun(run).each { DataFile dataFile ->
            // These returns are continues
            if (!dataFile.metaDataValid) {
                return
            }
            if (dataFile.fileWithdrawn) {
                return
            }
            if (dataFile.fileType.type != FileType.Type.SEQUENCE) {
                return
            }
            MetaDataEntry laneValue = MetaDataEntry.findByDataFileAnKey(dataFile, key)
            if (!laneValue) {
                throw new LaneNotDefinedException(run.name, dataFile.fileName)
            }
            lanes << laneValue.value

        }
        // run track creation for each lane
        lanes.each{String laneId ->
            println "processing ${laneId}"
            buildOneSequenceTrack(run, laneId)
        }
    }

    /**
     * Builds one sequence track identified by a lane id
     *
     * @param run - Run obejct
     * @param lane - lane identifier string
     */
    private void buildOneSequenceTrack(Run run, String lane) {
        SeqTrack seqTrack = buildFastqSeqTrack(run, lane)
        appendAlignmentToSeqTrack(seqTrack)
    }

    /**
     * Build one seqTrack from sequence (fastq) files
     * @param run
     * @param lane
     * @return
     */
    private SeqTrack buildFastqSeqTrack(Run run, String lane) {
        // find sequence files
        List<DataFile> laneDataFiles =
                getRunFilesWithTypeAndLane(run, FileType.Type.SEQUENCE, lane)
        // check if metadata consistent
        List<String> keyNames = [
            "SAMPLE_ID",
            "SEQUENCING_TYPE",
            "LIBRARY_LAYOUT",
            "PIPELINE_VERSION",
            "READ_COUNT"
        ]
        //List<MetaDataKey> keys = MetaDataKey.findAllByNameInList(keyNames)
        List<MetaDataEntry> metaDataEntries = getMetaDataValues(laneDataFiles.get(0), keyNames)
        println metaDataEntries
        boolean consistent = checkIfConsistent(laneDataFiles, keyNames, metaDataEntries)
        // error handling
        if (!consistent) {
            throw new MetaDataInconsistentException(laneDataFiles)
        }
        // check if complete
        // TODO to be implemented

        // build structure
        Sample sample = getSampleByString(metaDataEntries.get(0))
        if (!sample) {
            throw new SampleNotDefinedException(metaDataEntries.get(0))
        }
        SeqType seqType = SeqType.findByNameAndLibraryLayout(metaDataEntries.get(1), metaDataEntries.get(2))
        if (!seqType) {
            throw new SeqTypeNotDefinedException(metaDataEntries.get(1), metaDataEntries.get(2))
        }
        SeqTrack seqTrack = new SeqTrack(
            run : run,
            sample : sample,
            seqType : seqType,
            seqTech : run.seqTech,
            laneId : lane,
            hasFinalBam : false,
            hasOriginalBam : false,
            usingOriginalBam : false,
            pipelineVersion: metaDataEntries.get(3)
        )
        if (!seqTrack.validate()) {
            println seqTrack.errors
        }
        seqTrack.save(flush: true)
        laneDataFiles.each {DataFile dataFile ->
            dataFile.seqTrack = seqTrack
            dataFile.project = sample.individual.project
            dataFile.save(flush: true)
        }
        seqTrack = fillReadsForSeqTrack(seqTrack)
        seqTrack.save(flush: true)
        return seqTrack
    }

    /**
     * 
     */
    private Sample getSampleByString(String sampleString) {
        SampleIdentifier sampleId = SampleIdentifier.findByName(sampleString)
        if (!sampleId) {
            return null
        }
        return sampleId.sample
    }

    /**
     * Attach alignment files to a given seq track
     * @param seqTrack
     */
    private void appendAlignmentToSeqTrack(SeqTrack seqTrack) {
        // attach alignment to seqTrack
        List<DataFile> alignFiles =
            getRunFilesWithTypeAndLane(seqTrack.run, FileType.Type.ALIGNMENT, seqTrack.laneId)
        // no alignment files
        if (!alignFiles) {
            return
        }
        // find out if data complete
        List<String> alignKeyNames = ["SAMPLE_ID", "ALIGN_TOOL"]
        //List<MetaDataKey> alignKeys = MetaDataKey.findAllByName(alignKeyNames)
        List<MetaDataEntry> alignValues = getMetaDataValues(alignFiles.get(0), alignKeyNames)
        boolean consistent = checkIfConsistent(alignFiles, alignKeyNames, alignValues)
        if (!consistent) {
            println "inconsistent meta data to alignment file"
            return
        }
        Sample sample = getSampleByString(alignValues.get(0))
        if (sample != seqTrack.sample) {
            println "inconsistent sample between fastq and bam files"
            return
        }
        log.debug("alignment data found")
        // create or find alignment params object
        String alignProgram = alignValues.get(1) ?: seqTrack.pipelineVersion
        AlignmentParams alignParams = AlignmentParams.findByProgramName(alignProgram)
        if (!alignParams) {
            alignParams = new AlignmentParams(programName: alignProgram)
        }
        alignParams.save()
        // create alignment log
        AlignmentLog alignLog = new AlignmentLog(
                alignmentParams : alignParams,
                seqTrack : seqTrack,
                executedBy : AlignmentLog.Execution.INITIAL
                )
        // attach data files
        alignFiles.each {
            it.project = sample.individual.project
            it.alignmentLog = alignLog
            it.save()
        }
        seqTrack.hasOriginalBam = true
        // save
        alignLog.save()
        alignParams.save()
        seqTrack.save()
    }

    /**
     * Return all dataFiles for a given run, type and lane
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
AND entry.key = :key
AND entry.value = :value
''',
                [run: run, type: type, key: key, value: lane])
        println dataFiles
        return dataFiles
    }

    /**
     * The function returns the MetaDataEntry values associated with the keys
     *
     * @param dataFile The DataFile containing the values
     * @param metaDataKeys The MetaDataKeys for which the MetaDataEntrys are to be found
     * @return List containing MetaDataEntrys and the metaDataKeys associated
     */
    private List<MetaDataEntry> getMetaDataValues(DataFile dataFile, List<String>keyNames) {
        if (!dataFile) {
            return
        }
        List<MetaDataEntry> metaDataEntries = []
        for (int i = 0; i < keyNames.size(); i++) {
            metaDataEntries[i] = getMetaDataEntry(dataFile, keyNames[i])
        }
        return metaDataEntries
    }

    /**
     * Check if meta-data values for DataFile objects belonging
     * presumably to the same lane are consistent
     *
     * @param dataFiles - array of data files
     * @param keys - keys for which the consistency have to be checked
     * @param values - this array will be filled with values for given keys
     * @return consistency status
     */
    private boolean checkIfConsistent(List<DataFile> dataFiles, List<String>keyNames, List<MetaDataEntry> metaDataEntries) {
        if (dataFiles == null) {
            return false
        }
        for (int i=0; i<keyNames.size; i++) {
            MetaDataEntry reference = getMetaDataEntry(dataFiles.get(0), keyNames.get(i))
            metaDataEntries[i] = reference?.value
            for (int j = 1; j < dataFiles.size; j++) {
                MetaDataEntry entry = getMetaDataEntry(dataFiles.get(j), keyNames.get(i))
                if (entry?.value != reference?.value) {
                    log.debug(entry?.value)
                    log.debug(reference?.value)
                    return false
                }
            }
        }
        return true
    }

    /**
     *
     * Fills the numbers in the SeqTrack object using MetaDataEntry
     * objects from the DataFile objects belonging to this SeqTrack.
     *
     * @param seqTrack
     * @return manipulated seqTrack
     */
    private SeqTrack fillReadsForSeqTrack(SeqTrack seqTrack) {
        if (seqTrack.seqTech.name != "illumina") {
            return
        }
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        List<String> dbKeys = [
            "BASE_COUNT",
            "READ_COUNT",
            "INSERT_SIZE"
        ]
        List<String> dbFields = [
            "nBasePairs",
            "nReads",
            "insertSize"
        ]
        List<Boolean> add = [true, false, false]
        dataFiles.each { DataFile file ->
            if (file.fileType.type != FileType.Type.SEQUENCE) {
                return
            }
            MetaDataEntry.findAllByDataFile(file).each { MetaDataEntry entry ->
                for (int iKey=0; iKey < dbKeys.size(); iKey++) {
                    if (entry.key.name == dbKeys[iKey]) {
                        long value = 0
                        if (entry.value.isLong()) {
                            value = entry.value as long
                        }
                        if (add[iKey]) {
                            seqTrack."${dbFields[iKey]}" += value
                        } else {
                            seqTrack."${dbFields[iKey]}" = value
                        }
                    }
                }
            }
        }
        return seqTrack
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
    void checkSequenceTracks(long runId) {
        Run run = Run.get(runId)
        run.allFilesUsed = true
        DataFile.findAllByRun(run).each { DataFile dataFile ->
            if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
                dataFile.used = (dataFile.seqTrack != null)
                if (!dataFile.used) {
                    log.debug(dataFile)
                    run.allFilesUsed = false
                }
            }
            if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
                dataFile.used = (dataFile.alignmentLog != null)
                if (!dataFile.used) {
                    log.debug(dataFile)
                    run.allFilesUsed = false
                }
            }
        }
        log.debug("All files used: ${run.allFilesUsed}\n")
    }

    /**
     *
     * Returns a metat data entry belonging the a given data file
     * with a key specified by the input parameter
     *
     * @param file
     * @param key
     * @return
     */
    private MetaDataEntry getMetaDataEntry(DataFile file, MetaDataKey key) {
        getMetaDataEntry(file, key.name)
    }

    /**
     *
     * Returns a metat data entry belonging the a given data file
     * with a key specified by the input parameter
     *
     * @param file
     * @param key
     * @return
     */
    private MetaDataEntry getMetaDataEntry(DataFile file, String key) {
        MetaDataEntry entry = null
        // TODO: optimize
        MetaDataEntry.findAllByDataFile(file).each { MetaDataEntry iEntry ->
            if (iEntry.key.name == key) {
                entry = iEntry
            }
        }
        return entry
    }
}
