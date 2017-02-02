package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.stereotype.*

import java.util.regex.*

@Component
class Hipo2SampleIdentifierParser implements SampleIdentifierParser {

    static String REGEX = /^(?<pid>(?<project>K[0-9]{2}[A-Z])-[A-Z0-9]{6})-(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9])-(?<analyte>[DRPAWYLTME][0-9]|[0-9][CGH][0-9]{2})$/

    public DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches()) {
            String sampleTypeDbName = "${HipoTissueType.fromKey(matcher.group('tissueType'))}${matcher.group('tissueNumber')}"
            String analyte = matcher.group('analyte')
            if (!'DRPAWYEMT'.toCharArray().contains(analyte.charAt(0))) {
                sampleTypeDbName += "-${analyte}"
            }
            return new DefaultParsedSampleIdentifier(
                    "hipo_${matcher.group('project')}",
                    matcher.group('pid'),
                    sampleTypeDbName,
                    sampleIdentifier,
            )
        }
        return null
    }
}
