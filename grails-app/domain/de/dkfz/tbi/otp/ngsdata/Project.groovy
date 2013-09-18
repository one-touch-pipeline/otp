package de.dkfz.tbi.otp.ngsdata

class Project {
    String name
    String dirName
    String realmName

   static belongsTo = [
                   projectGroup: ProjectGroup
               ]

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false)
        realmName(blank: false)
        projectGroup(nullable: true)
    }

    String toString() {
        name
    }
}
