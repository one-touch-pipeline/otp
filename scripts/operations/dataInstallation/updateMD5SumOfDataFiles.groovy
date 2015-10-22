package operations.dataInstallation

import de.dkfz.tbi.otp.ngsdata.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * script to change md5Sums of Datafiles
 * runName = Name of {@link Run}, the {@link DataFile} belongs to as String
 * dataFileMap = Map of {@link DataFile} fileName as String and its new md5Sum as String
 *               dataFileMap = [fileName: md5Sum, fileName: md5Sum]
 */

String runName = ""
Map dataFileMap = [:]

Run run = exactlyOneElement(Run.findAllByName(runName))

DataFile.withTransaction {
    dataFileMap.each { fileName, md5Sum ->
        DataFile dataFile = exactlyOneElement(DataFile.findAllByRunAndFileName(run, fileName))

        dataFile.md5sum = md5Sum
        assert dataFile.save(flush: true, failOnError: true)

        final MetaDataEntry metaDataEntry = exactlyOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.MD5.toString()))))

        ctx.metaDataService.updateMetaDataEntry(metaDataEntry, md5Sum)
    }
}
