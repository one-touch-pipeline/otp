package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.stereotype.*

import java.util.regex.*

@Component
class Hipo2SampleIdentifierParser implements SampleIdentifierParser {

    private final static String PID = "(?<pid>(?<project>[KST][0-9]{2}[A-Z])-[A-Z0-9]{4}([A-Z0-9]{2})?)"
    private final static String TISSUE = "(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9]{1,2})"
    private final static String ANALYTE = "(?<analyte>[DRPAWYTBMLSE][0-9]|[0-9]*[CGHJ][0-9]{1,2})"

    static String REGEX = /^${PID}-${TISSUE}-${ANALYTE}$/

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + PID + /$/
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
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

    @Override
    String tryParseCellPosition(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ /^.*-${ANALYTE}$/
        if (matcher.matches()) {
            return matcher.group('analyte')
        }
        return null
    }
}
