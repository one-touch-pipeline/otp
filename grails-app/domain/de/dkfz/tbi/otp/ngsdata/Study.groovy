package de.dkfz.tbi.otp.ngsdata

class Study {
    String name
    String title
    String authors
    String description
    Project project
    static constraints = {
        name(unique: true)
        title(nullable: true)
        authors(nullable: true)
        description(nullable: true)
    }
}
