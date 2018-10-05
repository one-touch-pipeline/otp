package de.dkfz.tbi.otp.hipo

import de.dkfz.tbi.otp.ngsdata.ParsedSampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleIdentifierParser
import groovy.transform.TupleConstructor
import java.util.regex.Matcher
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("singleton")
class HipoSampleIdentifierParser implements SampleIdentifierParser {

    private final static String REGEX = /^(${PIDREGEX})-([${HipoTissueType.values()*.key.join("")}])(\d{1,2})-(([DRPACWY])(\d{1,2}))$/
    private final static String PIDREGEX = "([A-JL-RU-Z])(\\d\\d\\w)-(?:\\w\\w)?\\w\\w\\w(\\w)"

    @Override
    boolean isForProject(String projectName) {
        return projectName.matches("hipo_[A-JL-RU-Z]")
    }

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + PIDREGEX + /$/
    }

    /**
     * Tries to parse a HIPO sample name.
     * @return A {@link HipoSampleIdentifier} if the supplied string is a valid HIPO sample name,
     * otherwise <code>null</code>.
     */
    @Override
    HipoSampleIdentifier tryParse(String sampleName) {
        Matcher matcher = sampleName =~ REGEX
        if (!matcher.matches()) {
            return null
        }
        String projectLetter = matcher.group(2)
        String projectNumber = matcher.group(3)

        if (!(projectLetter ==~ /[HP]/)) {
            projectNumber = projectLetter + projectNumber
        }
        HipoTissueType tissueType = HipoTissueType.fromKey(matcher.group(5))

        // Project 35 has even more specific rules.
        if (projectNumber == "035") {
            if (projectLetter != "H" || !(matcher.group(4) =~ /^[KM]$/)
                    || ![HipoTissueType.BLOOD, HipoTissueType.CELL].contains(tissueType)) {
                return null
            }
        }

        // all projects except 059 may have only one digit in the sampleNumber
        if (projectNumber != "059") {
            if (matcher.group(6).length() != 1) {
                return null
            }
        }

        // if the analyte type is ChiP (C) there shall be two digits behind. For all other cases only one digit is allowed.
        final int expectedAnalyteNumberDigits = (matcher.group(8) == "C") ? 2 : 1
        if (matcher.group(9).size() != expectedAnalyteNumberDigits) {
            return null
        }

        return new HipoSampleIdentifier(
                /* projectNumber: */ projectNumber,
                /* pid: */ matcher.group(1),
                /* tissueType: */ tissueType,
                /* sampleNumber: */ matcher.group(6),
                /* experiment: */ matcher.group(7),
        )
    }
}

@TupleConstructor  // see http://jeffastorey.blogspot.de/2011/10/final-variables-in-groovy-with-dynamic.html
class HipoSampleIdentifier implements ParsedSampleIdentifier {

    /**
     * The HIPO project number.
     * Example: 004
     */
    final String projectNumber

    /**
     * The patient ID.
     * Example: H456-ABCD
     */
    final String pid

    /**
     * The tissue type.
     * Example: TUMOR
     */
    final HipoTissueType tissueType

    /**
     * The sample number.
     * Example: 3
     */
    final String sampleNumber

    /**
     * Which analyte type was applied to the sample, [DRPAC] (DNA, RNA, Protein, miRNA or ChiPSeq).
     * Always followed by the order number.
     * Example: D1 (DNA, attempt 1)
     */
    final String analyteTypeAndNumber

    @Override
    String getProjectName() {
        return "hipo_${projectNumber}"
    }

    /**
     * The 'original' name, concatenated from the parsed subparts.
     * Example: H456-ABCD-T3-D1
     */
    @Override
    String getFullSampleName() {
        return "${pid}-${tissueType.key}${sampleNumber}-${analyteTypeAndNumber}"
    }

    /**
     * The sample type name as it appears in the database, i.e. the tissue type name, followed by
     * the {@link #sampleNumber} if different from 1 or for project 35.
     * Example: TUMOR03
     */
    @Override
    String getSampleTypeDbName() {
        String dbName = tissueType.name()
        // In project 59 "1" and "01" have different meanings, so use
        // the sample number exactly as given in the sample identifier.
        if (projectNumber == '059') {
            dbName += sampleNumber
        } else {
            // 'default' sample number is 1, only make explicit when not 1
            // except project 35, which always makes it explicit, see sampleType JavaDoc
            int sampleNumberInt = Integer.parseInt(sampleNumber)
            if (sampleNumberInt != 1 || projectNumber == "035") {
                dbName += String.format('%02d', sampleNumberInt)
            }
        }
        return dbName
    }

    @Override
    String toString() {
        return fullSampleName
    }
}
