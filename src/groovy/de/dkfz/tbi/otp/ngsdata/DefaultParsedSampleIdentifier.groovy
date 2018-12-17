package de.dkfz.tbi.otp.ngsdata

import groovy.transform.Immutable

@Immutable
class DefaultParsedSampleIdentifier implements ParsedSampleIdentifier {

    String projectName
    String pid
    String sampleTypeDbName
    String fullSampleName

}
