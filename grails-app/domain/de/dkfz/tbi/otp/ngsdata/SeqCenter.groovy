package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

class SeqCenter implements Entity {

    String name

    /** where fastqc files are written to */
    String dirName

    /** where fastq files are imported from */
    String autoImportDir
    boolean autoImportable = false

    /** files in this directories may be linked */
    static hasMany = [
            importDirsAllowLinking: String,
    ]

    /** should meta data files be copied to the seq center inbox */
    boolean copyMetadataFile = false

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false, unique: true, validator: { OtpPath.isValidPathComponent(it) })
        autoImportDir nullable: true, blank: false, unique: true,  validator: { val, obj ->
            if (obj.autoImportable && !val) {
                return "auto import dir must be set if auto import enabled"
            }
            if (val != null && !OtpPath.isValidAbsolutePath(val)) {
                return "'${val}' is not a valid absolute path"
            }
        }
        importDirsAllowLinking nullable: true, validator: { val ->
            if (val != null) {
                Set<String> invalidPaths = val.findAll {
                    !OtpPath.isValidAbsolutePath(it)
                }
                if (invalidPaths) {
                    return "'${invalidPaths.join(", ")}' not valid absolute path(s)"
                }
            }
        }
    }

    @Override
    String toString() {
        name
    }
}
