package de.dkfz.tbi.otp.ngsdata

interface SampleIdentifierParser {

    public ParsedSampleIdentifier tryParse(String sampleIdentifier)

    public boolean tryParsePid(String pid)

    public boolean isForProject(String projectName)

}
