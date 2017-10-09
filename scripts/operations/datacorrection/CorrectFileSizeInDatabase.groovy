import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Correct the fastq file size for all datafiles of an run.
 *
 * It check the size in the file system with the one saved in the database. If they differ,
 * the value in the database are updated and the change is logged in the comment.
 *
 */

String runname = ''

//---------------------------------
assert runname : 'please provide a run name'

Run run = CollectionUtils.exactlyOneElement(Run.findAllByName(runname))

LsdfFilesService lsdfFilesService = ctx.lsdfFilesService
CommentService commentService = ctx.commentService

DataFile.withTransaction {
    List<DataFile> dataFiles = DataFile.findAllByRun(run)
    dataFiles.each {
        long sizeInDB = it.fileSize
        long sizeOnFilesystem = new File(lsdfFilesService.getFileFinalPath(it)).length()
        println "${it}: ${sizeInDB} ${sizeOnFilesystem} ${sizeInDB == sizeOnFilesystem}"
        if (sizeInDB != sizeOnFilesystem) {
            it.fileSize = sizeOnFilesystem
            it.save()
            String newMsg = "Correct file size from ${sizeInDB} to ${sizeOnFilesystem}"
            String oldMsg = it.comment?.comment
            String msg = oldMsg ? "${oldMsg}\n${newMsg}" : newMsg

            commentService.saveComment(it, msg)
        }
    }
}
