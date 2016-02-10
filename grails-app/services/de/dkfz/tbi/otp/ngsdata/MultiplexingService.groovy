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


    /**
     * find and extract barcodes of the file. It uses the following patterns in the given order:
     * <ul>
     * <li><code>/_([GATC]{6,8})_/</code></li>
     * <li><code>/_[0-9]_([0-9]{3})[\._]/</code></li>
     * </ul>
     * The first matching pattern is used and the group 1 is returned as barcode.
     *
     * @param fileName the name of the file to check for
     * @return the extracted barcode or null
     */
    public static String barcode(String fileName) {
        final List<String> regExpression = [
            /_([GATC]{6,8})_/ ,
            /_[0-9]_([0-9]{3})[\._]/ ,
            ]
        for (String muxRegexp: regExpression) {
            Matcher matcher = (fileName =~ muxRegexp)
            if (matcher) {
                return matcher.group(1)
            }
        }
        return null
    }


    private void appendBarcode(DataFile file, String barcode) {
        MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.LANE_NO.name())
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
