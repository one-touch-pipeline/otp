/*
 * If ProcessedAlignmentFileService.deleteOldAlignmentProcessingFiles has been executed on an OTP instance that uses the
 * production file system but a non-production database, the production database is inconsistent with the file system.
 * This script can be used to fix those inconsistencies.
 *
 * This script has to be run on the OTP instance using the non-production database. It outputs Groovy code, which then
 * has to be run on the production OTP.
 *
 * !!! This script assumes that every file that is marked as deleted in the non-production database is not marked as
 *     deleted in the production database. This is generally NOT true. (The first and only time this script was used so
 *     far, no files were marked as deleted in the production database.)
 */

import de.dkfz.tbi.otp.dataprocessing.*

final CharArrayWriter buffer = new CharArrayWriter()
final PrintWriter out = new PrintWriter(buffer)

//out.println ProcessedBamFile.countByDeletionDateIsNotNull()
//out.println ProcessedSaiFile.countByDeletionDateIsNotNull()
//out.println ProcessedBamFile.listOrderByDeletionDate()*.deletionDate
//out.println ProcessedSaiFile.listOrderByDeletionDate()*.deletionDate

doStuff = { dbFiles ->
    dbFiles*.each {
        assert it.fileExists == false
        assert it.deletionDate != null
        File alignmentDirectory = new File(ctx.processedAlignmentFileService.getDirectory(it.alignmentPass))
        assert alignmentDirectory.parentFile.exists()
        assert !alignmentDirectory.exists()
        out.println "alignmentDirectory = new File('${alignmentDirectory}')"
        out.println "assert alignmentDirectory.parentFile.exists()"
        out.println "assert !alignmentDirectory.exists()"
        out.println "dbFile = ${it.class.simpleName}.get(${it.id})"
        out.println "assert dbFile.fileExists"
        out.println "assert dbFile.deletionDate == null"
        out.println "dbFile.fileExists = false"
        out.println "dbFile.deletionDate = new Date(${it.deletionDate.time})"
        out.println "assert dbFile.save(flush: true)"
        out.println ""
    }
}

out.println "import de.dkfz.tbi.otp.dataprocessing.*"
out.println ""

doStuff ProcessedBamFile.findAllByDeletionDateIsNotNull()
doStuff ProcessedSaiFile.findAllByDeletionDateIsNotNull()

println buffer
