package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.stereotype.*

import java.util.regex.*

@Component
class Hipo2SampleIdentifierParser implements SampleIdentifierParser {

    static String REGEX = /^(?<pid>${PIDREGEX})-(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9]{1,2})-(?<analyte>[DRPAWYTBMLSE][0-9]|[0-9]*[CGH][0-9]{2})$/
    private final static String PIDREGEX = "(?<project>[KST][0-9]{2}[A-Z])-[A-Z0-9]{4}([A-Z0-9]{2})?"
    public boolean isForProject(String projectName) {
        return projectName.matches("hipo_[KST]")
    }

    public boolean tryParsePid(String pid) {
        return pid =~ "^"+PIDREGEX+/$/
    }

    public DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches()) {
            String projectNumber = matcher.group('project')

            String sampleTypeDbName = "${HipoTissueType.fromKey(matcher.group('tissueType'))}${matcher.group('tissueNumber')}"
            String analyte = matcher.group('analyte')
            if (!'DRPAWYEMT'.toCharArray().contains(analyte.charAt(0))) {
                sampleTypeDbName += "-${analyte}"
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
