package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.stereotype.*

import java.util.regex.*

@Component
class Hipo2SampleIdentifierParser implements SampleIdentifierParser {

    static String REGEX = /^(?<pid>(?<project>K[0-9]{2}[A-Z])-[A-Z0-9]{4}([A-Z0-9]{2})?)-(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9]{1,2})-(?<analyte>[DRPAWYLTME][0-9]|[0-9][CGH][0-9]{2})$/

    public DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches()) {
            String projectNumber = matcher.group('project')

            String sampleTypeDbName = "${HipoTissueType.fromKey(matcher.group('tissueType'))}${matcher.group('tissueNumber')}"
            String analyte = matcher.group('analyte')
            if (!'DRPAWYEMT'.toCharArray().contains(analyte.charAt(0))) {
                sampleTypeDbName += "-${analyte}"
            }
            if (projectNumber != "K26K" && matcher.group('tissueNumber').length() != 1) {
                return null
            }
            return new DefaultParsedSampleIdentifier(
                    "hipo_${projectNumber}",
                    matcher.group('pid'),
                    sampleTypeDbName,
                    sampleIdentifier,
            )
        }
        return null
    }
}
