package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Project

/**
 * ProcessingOption stores options for external programs processing data
 * depending on project and additional string parameters. The Options are
 * stored in a database and can be modified at runtime.
 *
 */

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
