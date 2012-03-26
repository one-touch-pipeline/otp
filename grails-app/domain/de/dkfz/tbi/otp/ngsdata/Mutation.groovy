package de.dkfz.tbi.otp.ngsdata

class Mutation {

    String chromosome
    long startPosition
    long endPosition
    String type
    String gene

    static belongsTo = [
        individual: Individual,
        resultsDataFile : ResultsDataFile
    ]

    static constraints = {
    }
}
