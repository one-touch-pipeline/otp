package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ContactPerson implements Entity {

    String fullName
    String email
    String aspera

    static belongsTo = Project

    static hasMany =  [
            projects: Project
    ]

    static constraints = {
        fullName(blank: false, unqiue: true)
        email(email:true)
        aspera(blank: true, nullable: true)
    }
}
