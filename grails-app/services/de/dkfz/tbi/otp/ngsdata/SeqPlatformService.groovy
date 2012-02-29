package de.dkfz.tbi.otp.ngsdata

class SeqPlatformService {

    def fileTypeService

    final String platformKeyName = "INSTRUMENT_PLATFORM"
    final String modelKeyName = "INSTRUMENT_MODEL"
    @SuppressWarnings("GrailsStatelessService")
    MetaDataKey platformKey
    @SuppressWarnings("GrailsStatelessService")
    MetaDataKey modelKey
    @SuppressWarnings("GrailsStatelessService")
    MetaDataEntry platformEntry
    @SuppressWarnings("GrailsStatelessService")
    MetaDataEntry modelEntry
    @SuppressWarnings("GrailsStatelessService")
    SeqPlatform seqPlatform = null

    //@Scope("Prototype")
    public boolean validateSeqPlatform(long runId) {
        Run run = Run.get(runId)
        seqPlatform = null
        setupKeys()
        if (!checkAllFiles(run)) {
            return false
        }
        correctMetaData(run)
        run.seqPlatform = seqPlatform
        run.save(flush: true)
        return true
    }

    private void setupKeys() {
        platformKey = MetaDataKey.findByName(platformKeyName)
        modelKey = MetaDataKey.findByName(modelKeyName)
    }

    private boolean checkAllFiles(Run run) {
        boolean isCorrect = true
        List<DataFile> files = DataFile.findAllByRun(run)
        for(DataFile file in files) {
            if (fileTypeService.isSequenceDataFile(file)) {
                if (!validateForFile(file)) {
                    isCorrect = false
                }
            }
        }
        return isCorrect
    }

    private boolean validateForFile(DataFile file) {
        setupEntries(file)
        if (platformEntry == null || modelEntry == null) {
            return false
        }
        if (validateWithEntries(platformEntry, modelEntry)) {
            return true
        }
        // known bug of swapped meta data entries
        return validateWithEntries(modelEntry, platformEntry)
    }

    private void setupEntries(DataFile file) {
        platformEntry = MetaDataEntry.findByDataFileAndKey(file, platformKey)
        modelEntry = MetaDataEntry.findByDataFileAndKey(file, modelKey)
    }

    private boolean validateWithEntries(MetaDataEntry pEntry, MetaDataEntry mEntry) {
        SeqPlatform platform = null
        SeqPlatformModelIdentifier identifier = SeqPlatformModelIdentifier.findByName(mEntry.value)
        if (identifier) {
            platform = identifier.seqPlatform
            if (platform.name.toLowerCase() != pEntry.value.toLowerCase()) {
                return false
            }
        } else {
            platform = SeqPlatform.findByNameAndModel(pEntry.value, null)
        }

        if (seqPlatform == null) {
            seqPlatform = platform
            return true
        } else {
            return seqPlatform.equals(platform)
        }
    }

    private boolean correctMetaData(Run run) {
        List<DataFile> files = DataFile.findAllByRun(run)
        for(DataFile file in files) {
            if (fileTypeService.isSequenceDataFile(file)) {
                correctMetaDataIfNeeded(file)
            }
        }
    }

    private boolean correctMetaDataIfNeeded(DataFile file) {
        setupEntries(file)
        correctMD(platformEntry, seqPlatform.name)
        correctMD(modelEntry, seqPlatform.model)
    }

    private boolean correctMD(MetaDataEntry entry, String value) {
        if (entry.value != value) {
            ChangeLog changeLog = new ChangeLog(
                rowId: entry.id,
                tableName: "MetaDataEntry",
                columnName: "value",
                fromValue: entry.value,
                toValue: value,
                source: ChangeLog.Source.SYSTEM,
                comment: "harmonizing sequencing platform name"
            )
            changeLog.save(flush: true)
            entry.value = value
            entry.source = MetaDataEntry.Source.SYSTEM
        }
        entry.status =  MetaDataEntry.Status.VALID
        entry.save(flush: true)
    }
}
