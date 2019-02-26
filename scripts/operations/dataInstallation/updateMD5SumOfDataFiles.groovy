package operations.dataInstallation

import de.dkfz.tbi.otp.ngsdata.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * script to change md5Sums of Datafiles
 * runName = Name of {@link Run}, the {@link DataFile} belongs to as String
 * dataFileMap = Map of {@link DataFile} fileName as String and its new md5Sum as String
 *               dataFileMap = [fileName: md5Sum, fileName: md5Sum]
 */

String executingUser = ""
List<String> runNames = [""]
Map<String, String> dataFileMap = [:]


assert executingUser: "Your username is used to set a comment that informs of the changes to the DataFile"
assert runNames: "Define the Runs"
runNames.each {
    assert it: "Invalid run in list of runs"
}

List<Run> runs = Run.findAllByNameInList(runNames)
assert runs.size() == runNames.size(): "Didn't find Runs for all run names"

DataFile.withTransaction {
    dataFileMap.each { String fileName, String newMd5sum ->
        DataFile dataFile = exactlyOneElement(DataFile.findAllByRunInListAndFileName(runs, fileName))

        String fromMd5 = dataFile.md5sum
        String toMd5 = newMd5sum
        Long fromFileSize = dataFile.fileSize
        Long toFileSize = new File(ctx.lsdfFilesService.getFileViewByPidPath(dataFile)).size()

        if (fromMd5 != toMd5) {
            dataFile.md5sum = newMd5sum
            dataFile.fileSize = toFileSize
            String newComment = """${new Date().format("yyyy-MM-dd HH:mm:ss")}
  changed md5sum   from: ${fromMd5} to: ${toMd5}
  changed fileSize from: ${fromFileSize} to: ${toFileSize}"""
            println(newComment)
            String previous = dataFile.getComment()?.comment
            ctx.commentService.createOrUpdateComment(
                    dataFile,
                    previous + (previous? "\n\n" : "") + newComment,
                    executingUser
            )
            assert dataFile.save(flush: true, failOnError: true)

            final MetaDataEntry metaDataEntry = exactlyOneElement(MetaDataEntry.findAllWhere(dataFile: dataFile, key: exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.MD5.toString()))))
            ctx.metaDataService.updateMetaDataEntry(metaDataEntry, newMd5sum)
        } else {
            println "Nothing done for ${fileName} with md5sum ${newMd5sum}"
        }
    }
    assert false: "Intentional failure for debugging"
}
