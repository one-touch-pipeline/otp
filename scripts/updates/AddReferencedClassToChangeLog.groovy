import de.dkfz.tbi.otp.ngsdata.ChangeLog
import de.dkfz.tbi.otp.utils.ReferencedClass
/**
 * This update script migrates the ChangeLog from using a tableName to using a more specific ReferencedClass
 * for indicating which domain class had changed.
 *
 * The script first migrates legacy "MetaDataEntry" names to the full qualified class name "de.dkfz.tbi.otp.ngsdata.MetaDataEntry"
 * and then updates all ChangeLog entries which do not yet have the new scheme.
 *
 * For each entry the ReferencedClass is created if it does not yet exist, the reference is set and the tableName is set to blank.
 */

ChangeLog.executeUpdate("UPDATE ChangeLog c SET c.tableName = 'de.dkfz.tbi.otp.ngsdata.MetaDataEntry' WHERE c.tableName = 'MetaDataEntry'")

ChangeLog.findAllByTableNameNotEqual("").each { ChangeLog log ->
    log.referencedClass = ReferencedClass.findOrSaveByClassName(log.tableName)
    log.tableName = ""
    log.save(flush: true)
}
