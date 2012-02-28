package de.dkfz.tbi.otp.ngsdata

class FileType {

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

    String toString() {
        "${type}-${subType}"
    }
}
