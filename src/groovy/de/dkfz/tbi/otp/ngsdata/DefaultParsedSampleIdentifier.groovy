package de.dkfz.tbi.otp.ngsdata

import groovy.transform.TupleConstructor

@TupleConstructor
class DefaultParsedSampleIdentifier implements ParsedSampleIdentifier {

    final String projectName
    final String pid
    final String sampleTypeDbName
    final String fullSampleName

}
