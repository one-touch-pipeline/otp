package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import org.springframework.context.annotation.Scope
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.ReferencedClass
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Scope("prototype")
class SeqPlatformService {

    FileTypeService fileTypeService

    SeqPlatformModelLabelService seqPlatformModelLabelService

    SequencingKitLabelService sequencingKitLabelService


    @SuppressWarnings("GrailsStatelessService")
    MetaDataKey platformKey
    @SuppressWarnings("GrailsStatelessService")
    MetaDataKey modelKey
    @SuppressWarnings("GrailsStatelessService")
    MetaDataKey sequencingKey

    @SuppressWarnings("GrailsStatelessService")
    MetaDataEntry platformEntry
    @SuppressWarnings("GrailsStatelessService")
    MetaDataEntry modelEntry
    @SuppressWarnings("GrailsStatelessService")
    MetaDataEntry sequencingEntry

    @SuppressWarnings("GrailsStatelessService")
    SeqPlatform seqPlatform = null



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
        platformKey = MetaDataKey.findByName(MetaDataColumn.INSTRUMENT_PLATFORM.name())
        modelKey = MetaDataKey.findByName(MetaDataColumn.INSTRUMENT_MODEL.name())
        sequencingKey = MetaDataKey.findByName(MetaDataColumn.SEQUENCING_KIT.name())
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
            LogThreadLocal.getThreadLog()?.error("Platform or model are not given for '${file}")
            return false
        }
        return validateWithEntries(platformEntry, modelEntry, sequencingEntry, file)
    }

    private void setupEntries(DataFile file) {
        platformEntry = MetaDataEntry.findByDataFileAndKey(file, platformKey)
        modelEntry = MetaDataEntry.findByDataFileAndKey(file, modelKey)
        sequencingEntry = MetaDataEntry.findByDataFileAndKey(file, sequencingKey)
    }

    private boolean validateWithEntries(MetaDataEntry pEntry, MetaDataEntry mEntry, MetaDataEntry sEntry, DataFile file) {
        SeqPlatform platform = null
        SeqPlatformModelLabel seqPlatformModelLabel = null
        SequencingKitLabel sequencingKitLabel = null

        if (mEntry?.value) {
            seqPlatformModelLabel = seqPlatformModelLabelService.findSeqPlatformModelLabelByNameOrAlias(mEntry.value)
            if (!seqPlatformModelLabel) {
                LogThreadLocal.getThreadLog()?.error("Could not find a SeqPlatformModelLabel for '${mEntry.value}' (requested by file ${file})")
                return false
            }
        }
        if (sEntry?.value) {
            sequencingKitLabel = sequencingKitLabelService.findSequencingKitLabelByNameOrAlias(sEntry.value)
            if (!sequencingKitLabel) {
                LogThreadLocal.getThreadLog()?.error("Could not find a SequencingKitLabel for '${sEntry.value}' (requested by file ${file})")
                return false
            }
        }
        platform = findForNameAndModelAndSequencingKit(pEntry.value, seqPlatformModelLabel, sequencingKitLabel)
        if (!platform) {
            LogThreadLocal.getThreadLog()?.error("Could not find a SeqPlatform for '${pEntry}' and '${mEntry}' and '${sEntry}' (requested by file ${file})")
            return false
        }

        if (seqPlatform == null) {
            seqPlatform = platform
            return true
        } else if (!seqPlatform.equals(platform)) {
            LogThreadLocal.getThreadLog()?.error("The found seqPlatform '${platform}' differs from the one of the run '${seqPlatform}'")
            return false
        }
        return true
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
        correctMD(modelEntry, seqPlatform.seqPlatformModelLabel.name)
        if (seqPlatform.sequencingKitLabel) {
            correctMD(sequencingEntry, seqPlatform.sequencingKitLabel.name)
        }
    }

    private boolean correctMD(MetaDataEntry entry, String value) {
        if (entry.value != value) {
            ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(MetaDataEntry.class.getName())
            ChangeLog changeLog = new ChangeLog(
                            rowId: entry.id,
                            referencedClass: clazz,
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

    SeqPlatform findForNameAndModelAndSequencingKit(String platformName, SeqPlatformModelLabel seqPlatformModelLabel, SequencingKitLabel sequencingKitLabel) {
        assert platformName
        return SeqPlatform.findByNameIlikeAndSeqPlatformModelLabelAndSequencingKitLabel(platformName, seqPlatformModelLabel, sequencingKitLabel)
    }
}
