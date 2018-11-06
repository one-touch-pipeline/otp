package de.dkfz.tbi.otp.dataprocessing


import groovy.transform.*

@TupleConstructor
enum SampleIdentifierParserBeanName {
    NO_PARSER('', 'No Parser'),
    DEEP('deepSampleIdentifierParser', 'DEEP'),
    HIPO('hipoSampleIdentifierParser', 'HIPO'),
    HIPO2('hipo2SampleIdentifierParser', 'HIPO2'),
    INFORM('informSampleIdentifierParser', 'INFORM'),

    final String beanName
    final String displayName
}
