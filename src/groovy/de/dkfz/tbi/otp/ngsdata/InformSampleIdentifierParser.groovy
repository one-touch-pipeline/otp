package de.dkfz.tbi.otp.ngsdata

import java.util.regex.Matcher
import org.springframework.stereotype.Component

@Component
class InformSampleIdentifierParser implements SampleIdentifierParser {

    public static final String REGEX = createRegex()

    public DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher && checkSampleIdentifierConsistency(findSampleIdentifiersByPidAndTissueTypeKey(matcher.group('pid'), matcher.group('tissueTypeKey')))) {
            return new DefaultParsedSampleIdentifier(
                    'INFORM',
                    matcher.group('pid'),
                    buildSampleTypeDbName(matcher),
                    sampleIdentifier,
            )
        }
        return null
    }

    private Collection<SampleIdentifier> findSampleIdentifiersByPidAndTissueTypeKey(String pid, String tissueTypeKey) {
        Collection<SampleIdentifier> result = SampleIdentifier.createCriteria().list {
            sample {
                individual {
                    eq ("pid", pid)
                }
            }
        }.findAll {
            Matcher matcher = it.name =~ REGEX
            return matcher.matches() && (matcher.group('tissueTypeKey') == tissueTypeKey)
        }
        return result
    }

    private boolean checkSampleIdentifierConsistency(Collection<SampleIdentifier> sampleIdentifiers) {
        return sampleIdentifiers.every {
            return buildSampleTypeDbName(it.name =~ REGEX) == it.sampleType.name
        }
    }

    private String buildSampleTypeDbName(Matcher matcher) {
        assert matcher.matches()
        return "${InformTissueType.fromKey(matcher.group('tissueTypeKey'))}0${matcher.group('sampleTypeNumber')}"
    }

    private static String createRegex() {
        String treatingCenterId = "([0-9]{3})"
        String patientId =  "([0-9]{3})"
        String sampleTypeNumber = "(?<sampleTypeNumber>([0-9]))"
        String tissueTypeKey = "(?<tissueTypeKey>([TMCFL]))"
        String sampleId = "(${sampleTypeNumber}${tissueTypeKey}[0-9]_[DRPI][0-9])"
        return "^"+
                "(?<pid>(I${treatingCenterId}_${patientId}))_" +
                "${sampleId}" +
                /$/
    }

}