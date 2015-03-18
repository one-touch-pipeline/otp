package de.dkfz.tbi.otp.ngsdata
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.utils.ReferencedClass
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 */
class MetaDataValidationService {

    def hipoIndividualService

    LibraryPreparationKitService libraryPreparationKitService
    SequencingKitLabelService sequencingKitLabelService

    private final Lock validateMetaDataLock = new ReentrantLock()

    @SuppressWarnings("GrailsStatelessService")
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
                if (checkSampleIdentifier(entry.value)) {
                    hipoIndividualService.createHipoIndividual(entry.value)
                    SampleIdentifier sample = SampleIdentifier.findByName(entry.value)
                    entry.status = (sample != null) ? valid : invalid
                } else {
                    entry.status = invalid
                }
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
            case "LIB_PREP_KIT":
                entry.status = checkLibraryPreparationKit(entry) ? valid : invalid
                break
            case "SEQUENCING_KIT":
                entry.status = !entry.value || sequencingKitLabelService.findSequencingKitLabelByNameOrAlias(entry.value) ? valid : invalid
                break
            case "ANTIBODY_TARGET":
                MetaDataEntry metaDataEntry = metaDataEntry(entry.dataFile, "SEQUENCING_TYPE")
                boolean isSequenceOfTypeChipSeq = metaDataEntry?.value == SeqTypeNames.CHIP_SEQ.seqTypeName
                if (isSequenceOfTypeChipSeq) {
                    checkAndCorrectAntibodyTarget(entry)
                }
                break
        }
        entry.save(flush: true)
        return (entry.status == invalid)? false : true
    }


    private void checkAndCorrectAntibodyTarget(MetaDataEntry entry) {
        AntibodyTarget value = AntibodyTarget.createCriteria().get {
            eq("name", entry.value, [ignoreCase: true])
        }
        if(entry.value == value?.name) {
            entry.status = MetaDataEntry.Status.VALID
        } else if(value) {
            ChangeLog changeLog = buildChangeLog(
                    entry.id, entry.value, value.name, "fixed wrong upper/lower case",
            )
            changeLog.save(flush: true)
            entry.status = MetaDataEntry.Status.VALID
            entry.source = MetaDataEntry.Source.SYSTEM
            entry.value = value.name
        } else {
            entry.status = MetaDataEntry.Status.INVALID
        }
    }

    private boolean checkSampleIdentifier(String value) {
        //values are trimed during upload
        if (!value) {
            LogThreadLocal.getThreadLog()?.error('Sample Identifier may not be empty')
            return false
        } else if (value.size() < 3) {
            LogThreadLocal.getThreadLog()?.error('Sample Identifier should have at least three characters')
            return false
        } else {
            return true
        }
    }

    /**
     * validate the value for library preparation kit. The value is valid, if
     * <ul>
     * <li> the value is in {@link LibraryPreparationKit} </li>
     * <li> the value is in {@link LibraryPreparationKitSynonym} </li>
     * <li> the value has the special value {@link InformationReliability#UNKNOWN_VERIFIED} ("UNKNOWN) and is an exome seq track </li>
     * <li> the value is empty and belongs not to an exome seq track </li>
     * </ul>
     * All other values are invalid.
     */
    private boolean checkLibraryPreparationKit(MetaDataEntry entry) {
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(entry.value)
        if (libraryPreparationKit) {
            //Value could be found, so the value is valid
            return true
        }

        //check seq type
        MetaDataEntry metaDataEntry = metaDataEntry(entry.dataFile, "SEQUENCING_TYPE")
        if (metaDataEntry.value == SeqTypeNames.EXOME.seqTypeName) {
            /*
             * It is allowed to have the value "UNKNOWN" as library preparation kit info for exome seqTrack.
             * If this is the case return true.
             */
            return InformationReliability.UNKNOWN_VERIFIED.rawValue == entry.value
        } else {
            return !entry.value
        }

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
        List<String> allowedFillers = ["n/a", "N/A", "n.a.", "N.A.", "na", "NA"]
        if (allowedFillers.contains(value)) {
            return true // essentially shall be false
        }
        return false
    }


    /**
     * Recovers insert size number in a notation: 230bp
     *
     * @param entry meta data entry
     * @return true if recovery successful, false otherwise
     */
    private boolean tryToRecoverInsertSize(MetaDataEntry entry) {
        int idx = entry.value.indexOf("b")
        if (idx < 0) {
            return false
        }
        String substring = entry.value.substring(0, idx)
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
        entry.save(flush: true)
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
        MetaDataKey key = getKey("ALIGN_TOOL")
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
        //entry.validate()
        //println entry.errors
        entry.save(flush: true)
        String cmnt = "adding missing entry ALIGN_TOOL"
        ChangeLog changeLog = buildChangeLog(entry.id, "", "unknown", cmnt)
        changeLog.save(flush: true)
    }


    private MetaDataKey getKey(String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        if (!key) {
            key = new MetaDataKey(name: keyName)
            key.save(flush: true)
        }
        return key
    }


    private void combineLaneAndIndex(DataFile file) {
        MetaDataEntry index = metaDataEntryFromList(file, ["INDEX_NO", "BARCODE"])
        if (!index || index.value.isAllWhitespace()) {
            return
        }
        MetaDataEntry lane = metaDataEntry(file, "LANE_NO")
        String oldValue = lane.value
        if (!oldValue.endsWith(index.value)) {
            String value = lane.value + "_" + index.value
            lane.value = value
            lane.source = MetaDataEntry.Source.SYSTEM
            lane.save(flush: true)

            String cmnt = "combinig lane_no and index_no"
            ChangeLog changeLog = buildChangeLog(lane.id, oldValue, value, cmnt)
            changeLog.save(flush: true)
        }
    }


    private ChangeLog buildChangeLog(long rowId, String from, String to, String comment) {
        ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(MetaDataEntry.class.getName())
        ChangeLog changeLog = new ChangeLog(
                        rowId : rowId,
                        ReferencedClass: clazz,
                        columnName : "value",
                        fromValue : from,
                        toValue : to,
                        comment : comment,
                        source : ChangeLog.Source.SYSTEM
                        )
        return changeLog
    }


    /**
     * Returns first found meta data entry from the key names in a list.
     * If no entry was found returns null
     */
    private MetaDataEntry metaDataEntryFromList(DataFile file, List<String> keyNames) {
        for(String keyName in keyNames) {
            MetaDataEntry entry = metaDataEntry(file, keyName)
            if (entry) {
                return entry
            }
        }
        return null
    }


    private MetaDataEntry metaDataEntry(DataFile file, String keyName) {
        MetaDataKey key = MetaDataKey.findByName(keyName)
        return MetaDataEntry.findByDataFileAndKey(file, key)
    }
}
