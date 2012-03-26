package de.dkfz.tbi.otp.ngsdata

class IgvSessionFileController {

    def file = {
        println params.id
        String name = "${params.id}.xml"
        IgvSessionFile file = IgvSessionFile.findByName(name)
        render (file.content)
    }

    def mutFile = {
        println params.id
        int indId = params.id.substring(0, params.id.indexOf(".")) as int
        Individual ind = Individual.get(indId)
        List<Mutation> muts = Mutation.findAllByIndividual(ind)

        println ind
        println muts

        String text = "chr\tstart\tend\tsample\ttype\n"
        for(Mutation mut in muts) {
            text += mut.chromosome + "\t"
            text += mut.startPosition + "\t"
            text += mut.endPosition + "\t"
            text += mut.individual.mockFullName + "\t"
            text += mut.type + "\n"
        }
        println text
        render(text)
    }
}