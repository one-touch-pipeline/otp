package de.dkfz.tbi.otp.ngsdata

interface SampleIdentifierParser {

    ParsedSampleIdentifier tryParse(String sampleIdentifier)

    boolean tryParsePid(String pid)

    String tryParseCellPosition(String sampleIdentifier)
}
