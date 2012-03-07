package de.dkfz.tbi.otp.ngsdata
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class MetaDataValidationService {

    private final Lock validateMetaDataLock = new ReentrantLock()

    HashMap bugs = ["SBS": "WHOLE_GENOME_BISULFITE"]

    /**
    * Checks if values of MetaDataEntry table are correct
    *
    * the following keys are checked
    * RunID, sample identifier, sequencing center
    * sequencing type and library layout
    *
    * @param runId
    */
    boolean validateMetadata(long runId) {
        Run run = Run.get(runId)
        boolean allValid = true
        validateMetaDataLock.lock()
        try {
            DataFile.findAllByRun(run).each { DataFile dataFile ->
                dataFile.metaDataValid = true
                MetaDataEntry.findAllByDataFile(dataFile).each { MetaDataEntry entry ->
                    boolean isValid = validateMetaDataEntry(run, entry)
                    if (!isValid) {
                        dataFile.metaDataValid = false
                        allValid = false
                   }
                }
                addMissingEntries(dataFile)
                combineLaneAndIndex(dataFile)
                dataFile.save(flush: true)
            }
        } finally {
            validateMetaDataLock.unlock()
        }
        run.save(flush: true)
        return allValid
    }

    /**
     * Validate individual MetaDataEntry
     * @param run
     * @param entry
     * @return
     */
    private boolean validateMetaDataEntry(Run run, MetaDataEntry entry) {
        MetaDataEntry.Status valid = MetaDataEntry.Status.VALID
        MetaDataEntry.Status invalid = MetaDataEntry.Status.INVALID
        switch(entry.key.name) {
            case "RUN_ID":
                entry.status = (run.name == entry.value) ? valid : invalid
                break
            case "SAMPLE_ID":
                SampleIdentifier sample = SampleIdentifier.findByName(entry.value)
                entry.status = (sample != null) ? valid : invalid
                break
            case "CENTER_NAME":
                entry.status = invalid
                SeqCenter center = run.seqCenter
                if (center.dirName == entry.value.toLowerCase()) {
                    entry.status = valid
                } else if (center.name == entry.value) {
                    entry.status = valid
                }
                break
            case "SEQUENCING_TYPE":
                boolean status = checkAndCorrectSequencingType(entry)
                entry.status = (status) ? valid : invalid
                break
            case "LIBRARY_LAYOUT":
                SeqType seqType = SeqType.findByLibraryLayout(entry.value)
                entry.status = (seqType != null) ? valid : invalid
                break
            case "INSERT_SIZE":
                entry.status = (checkAndCorrectInsertSize(entry)) ? valid : invalid
                break
            case "PIPELINE_VERSION":
            case "ALIGN_TOOL":
                boolean status = checkSoftwareTool(entry)
                entry.status = (status) ? valid : invalid
                break
        }
        entry.save(flush: true)
        return (entry.status == invalid)? false : true
    }

    /**
     * Best effort to interpret insdert_size if it is incorrect try to recover
     * by removing trailing 'bp'
     * @param entry
     * @return
     */
    private boolean checkAndCorrectInsertSize(MetaDataEntry entry) {
        if (checkInsertSize(entry.value)) {
            return true
        }
        return tryToRecoverInsertSize(entry)
    }

    private boolean checkInsertSize(String value) {
        if (value.isAllWhitespace()) {
            return true
        }
        if (value.isInteger()) {
            return true
        }
        if (value.contains("n/a")) {
            return true // essentially shall be false
        }
        return false
    }

    private boolean tryToRecoverInsertSize(MetaDataEntry entry) {
        String substring = entry.value.substring(0, entry.value.indexOf("b"))
        if (substring.isInteger()) {
            changeInsertSizeValue(entry, substring)
            return true
        }
        return false
    }

    private void changeInsertSizeValue(MetaDataEntry entry, substring) {
        ChangeLog changeLog = buildChangeLog(
            entry.id, entry.value, substring, "removing trailing 'bp'",
        )
        changeLog.save(flush: true)
        entry.value = substring
        entry.source = MetaDataEntry.Source.SYSTEM
    }

    private boolean checkAndCorrectSequencingType(MetaDataEntry entry) {
        SeqType seqType = SeqType.findByName(entry.value)
        if (seqType != null) {
            return true
        }
        if (tryDirNameBug(entry)) {
            return true
        }
        if (tryHashMapBug(entry)) {
            return true
        }
        return false
    }

    private boolean tryDirNameBug(MetaDataEntry entry) {
        SeqType seqType = SeqType.findByDirName(entry.value)
        if(seqType != null) {
            ChangeLog changeLog = buildChangeLog(
                entry.id, entry.value, seqType.name, 
                "seqType recogniozed by directory name"
            )
            changeLog.save(flush: true)
            entry.value = seqType.name
            entry.source = MetaDataEntry.Source.SYSTEM
            entry.save()
            return true
        }
        return false
    }

    private boolean tryHashMapBug(MetaDataEntry entry) {
        String value = bugs[entry.value]
        if (value != null) {
            ChangeLog changeLog = buildChangeLog(
                entry.id, entry.value, value, "fixing known bug"
            )
            changeLog.save(flush: true)
            entry.value = value
            entry.source = MetaDataEntry.Source.SYSTEM
            entry.save()
            return true
        }
        return false
    }

    private boolean checkSoftwareTool(MetaDataEntry entry) {
        SoftwareToolIdentifier idx = SoftwareToolIdentifier.findByName(entry.value)
        if (idx) {
            return true
        }
        if (entry.value.isAllWhitespace()) {
            ChangeLog changeLog = buildChangeLog(
                entry.id, entry.value, "unknown", "changing blank to unknown"
            )
            changeLog.save(flush: true)
            entry.value = "unknown"
            entry.source = MetaDataEntry.Source.SYSTEM
            entry.save()
            return true
        }
        return false
    }

    private void addMissingEntries(DataFile dataFile) {
        addAlignmentToolIfNeeded(dataFile)
    }

    private void addAlignmentToolIfNeeded(DataFile file) {
        if (file.fileType.type != FileType.Type.ALIGNMENT) {
            return
        }
        MetaDataKey key = MetaDataKey.findByName("ALIGN_TOOL")
        MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(file, key)
        if (entry) {
            return
        }
        entry = new MetaDataEntry(
            dataFile: file,
            key: key,
            value: "unknown",
            source: MetaDataEntry.Source.SYSTEM,
            status: MetaDataEntry.Status.VALID
        )
        entry.save(flush: true)
        String cmnt = "adding missing entry ALIGN_TOOL"
        ChangeLog changeLog = buildChangeLog(entry.id, "", "unknown", cmnt)
        changeLog.save(flush: true)
    }

    private void combineLaneAndIndex(DataFile file) {
        MetaDataEntry index = metaDataEntry(file, "INDEX_NO")
        if (!index) {
            return
        }
        MetaDataEntry lane = metaDataEntry(file, "LANE_NO")
        String oldValue = lane.value
        String value = lane.value + "_" + index.value
        lane.value = value
        lane.source = MetaDataEntry.Source.SYSTEM
        lane.save(flush: true)

        String cmnt = "combinig lane_no and index_no"
        ChangeLog changeLog = buildChangeLog(lane.id, oldValue, value, cmnt)
        changeLog.save(flush: true)
    }

    private ChangeLog buildChangeLog(long rowId, String from, String to, String comment) {
        ChangeLog changeLog = new ChangeLog(
            rowId : rowId,
            tableName : "MetaDataEntry",
            columnName : "value",
            fromValue : from,
            toValue : to,
            comment : comment,
            source : ChangeLog.Source.SYSTEM
        )
        return changeLog
    }

    private MetaDataEntry metaDataEntry(DataFile file, String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }
}
