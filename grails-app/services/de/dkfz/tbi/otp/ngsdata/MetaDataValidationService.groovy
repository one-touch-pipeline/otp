package de.dkfz.tbi.otp.ngsdata
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class MetaDataValidationService {

    private final Lock validateMetaDataLock = new ReentrantLock()

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
                SeqType seqType = SeqType.findByName(entry.value)
                entry.status = (seqType != null) ? valid : invalid
                break
            case "LIBRARY_LAYOUT":
                SeqType seqType = SeqType.findByLibraryLayout(entry.value)
                entry.status = (seqType != null) ? valid : invalid
                break
            case "INSERT_SIZE":
                entry.status = (checkAndCorrectInsertSize(entry)) ? valid : invalid
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
        ChangeLog changeLog = new ChangeLog(
            rowId : entry.id,
            tableName : "MetaDataEntry",
            columnName : "value",
            fromValue : entry.value,
            toValue : substring,
            comment : "removing trailing 'bp'",
            source : ChangeLog.Source.SYSTEM
        )
        changeLog.save(flush: true)
        entry.value = substring
        entry.source = MetaDataEntry.Source.SYSTEM
    }
}
