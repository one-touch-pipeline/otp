package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ContactPersonRole implements Entity{

    String name

    static constraints = {
        name(blank: false, unique: true)
    }
}
