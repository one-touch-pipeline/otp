package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class FileType implements Entity {

    enum Type {
        SOURCE, MERGED, RESULT, SEQUENCE, ALIGNMENT, UNKNOWN
    }
    Type type

    String subType = ""
    String vbpPath = ""         // where to link the file
    String signature = ""       // how to recognize the file

    static constraints = {
        type(blank: false)
        subType()
    }

    @Override
    String toString() {
        "${type}-${subType}"
    }
}
