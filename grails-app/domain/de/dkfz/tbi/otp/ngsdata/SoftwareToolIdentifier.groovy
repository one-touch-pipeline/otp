package de.dkfz.tbi.otp.ngsdata

class SoftwareToolIdentifier {

    String name
    static belongsTo = [softwareTool : SoftwareTool]
    static constraints = {
        name()
    }
}
