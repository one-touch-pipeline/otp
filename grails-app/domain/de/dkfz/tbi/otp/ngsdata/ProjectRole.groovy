package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectRole implements Entity {

    String name
    boolean accessToOtp
    boolean accessToFiles
    boolean allowedToAddNewProjectUser

    static constraints = {
        name(blank: false, unique: true)
    }
}
