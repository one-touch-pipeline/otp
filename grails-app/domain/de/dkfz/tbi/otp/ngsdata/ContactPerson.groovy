package de.dkfz.tbi.otp.ngsdata

class ContactPerson {

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
