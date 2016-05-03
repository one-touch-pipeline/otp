package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class Mutation implements Entity {

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

    static mapping = {
        individual index: "mutation_individual_idx"
        resultsDataFile index: "mutation_results_data_file_idx"
    }
}
