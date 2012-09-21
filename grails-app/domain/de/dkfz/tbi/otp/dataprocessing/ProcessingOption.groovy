package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Project

class ProcessingOption {

    String name
    String type
    String value
    Project project
    Date dateCreated = new Date()
    Date dateObsoleted
    String comment

    static constraints = {
        type(nullable: true)
        project(nullable: true)
        dateObsoleted(nullable: true)
    }
}
