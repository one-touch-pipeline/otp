package de.dkfz.tbi.otp.ngsdata

class IgvSessionFileController {

    def file = {
        println params.id
        String name = "${params.id}.xml"
        IgvSessionFile file = IgvSessionFile.findByName(name)
        render (file.content)
    }
}