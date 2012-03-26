package de.dkfz.tbi.otp.ngsdata

class ResultsParserService {

    void parse(ResultsDataFile results) {
        results.text.eachLine{String line, int no ->
            if (no > 1) {
                Mutation mut = processLine(line)
                if (mut) {
                    mut.resultsDataFile = results
                    mut.save(flush: true)
                }
            }
        }
    }

    private Mutation processLine(String line) {
        List<String> tokens = line.tokenize("\t")
        Individual ind = getIndividual(tokens.get(0))
        if (!ind) {
            println "Unknown sample ${tokens.get(0)}"
            return null
        }
        Mutation mutation = new Mutation (
            chromosome: tokens.get(1),
            startPosition: tokens.get(2) as long,
            endPosition: tokens.get(2) as long,
            type: tokens.get(9),
            gene: tokens.get(7),
            individual: ind
        )
        return mutation
    }

    private Individual getIndividual(String name) {
        SampleIdentifier sampleId = SampleIdentifier.findByName(name)
        if (sampleId) {
            return sampleId.sample.individual
        }
        return null
    }
}
