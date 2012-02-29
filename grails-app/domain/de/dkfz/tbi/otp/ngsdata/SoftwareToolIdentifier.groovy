package de.dkfz.tbi.otp.ngsdata

class SoftwareToolIdentifier {

    String name
    static belongsTo = [SoftwareTool]
    static constraints = {
        name()
    }
}
