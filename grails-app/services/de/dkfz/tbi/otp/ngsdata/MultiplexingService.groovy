package de.dkfz.tbi.otp.ngsdata

import java.util.regex.Matcher
import de.dkfz.tbi.otp.utils.ReferencedClass

class MultiplexingService {

    def runProcessingService

    public boolean needsMultiplexingHandling(Run run) {
        List<DataFile> files = runProcessingService.dataFilesWithMetaDataInProcessing(run)
        for (DataFile file in files) {
            if (barcode(file.fileName)) {
                return true
            }
        }
        return false
    }

    public void executeMultiplexing(Run run) {
        List<DataFile> files = runProcessingService.dataFilesWithMetaDataInProcessing(run)
        for (DataFile file in files) {
            String barcode = barcode(file.fileName)
            if (barcode) {
                appendBarcode(file, barcode)
            }
        }
    }

    public String barcode(String fileName) {
        final String muxRegexp = /_[GATC]{6,8}_/
        Matcher m = (fileName =~ muxRegexp)
        if (!m) {
            return null
        }
        return m[0].replace("_", "")
    }

    private void appendBarcode(DataFile file, String barcode) {
        MetaDataKey key = MetaDataKey.findByName("LANE_NO")
        MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(file, key)
        if (entry.value.contains(barcode)) {
            return
        }
        String newValue = "${entry.value}_${barcode}"
        buildChangeLog(entry, newValue)
        entry.value = newValue
        entry.save(flush: true)
    }

    private void buildChangeLog(MetaDataEntry entry, String to) {
        ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(MetaDataEntry.class.getName())
        ChangeLog changeLog = new ChangeLog(
            rowId: entry.id,
            ReferencedClass: clazz,
            columnName: "value",
            fromValue: entry.value,
            toValue: to,
            comment: "appending BARCODE to lane number",
            source: ChangeLog.Source.SYSTEM
        )
        changeLog.save(flush: true)
    }
}
