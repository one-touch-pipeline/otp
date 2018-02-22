package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectRole implements Entity {

    String name
    boolean accessToOtp = false
    boolean accessToFiles = false
    boolean manageUsersAndDelegate = false

    static constraints = {
        name(blank: false, unique: true)
    }
}
