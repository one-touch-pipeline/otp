package de.dkfz.tbi.otp.ngsdata

class ToolName {

    String name

    static constraints = {
        name unique: true
    }
}
