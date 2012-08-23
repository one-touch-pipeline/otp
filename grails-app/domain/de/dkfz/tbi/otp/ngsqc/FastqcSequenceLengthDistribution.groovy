package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.DataFile

/**
 * Domain class to store the 'Sequence Length Distribution' module data from fastqc files
 * 
 */
class FastqcSequenceLengthDistribution {

    /**
     * Length of the sequences (Column Label in fastqc file is 'length')
     */
    int length

    /**
     * Number of sequences with same length (Column label in fastqc file is 'Count' which is a reserved word)
     */
    double countSequences

    static belongsTo = [
        dataFile : DataFile
    ]
}
