package de.dkfz.tbi.otp.hipo

import groovy.transform.TupleConstructor
import java.util.regex.Matcher

@TupleConstructor  // see http://jeffastorey.blogspot.de/2011/10/final-variables-in-groovy-with-dynamic.html
class HipoSampleIdentifier {

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
    final int sampleNumber

    /**
     * Which analyte type was applied to the sample, [DRPAC] (DNA, RNA, Protein, miRNA or ChiPSeq).
     * Always followed by the order number.
     * Example: D1 (DNA, attempt 1)
     */
    final String analyteTypeAndNumber

    private final static String REGEX =/^(([A-Z])(\d\d\w)-(?:\w\w)?\w\w\w(\w))-([${HipoTissueType.values()*.key.join("")}])(\d{1,2})-(([DRPAC])(\d{1,2}))$/

    /**
     * Tries to parse a HIPO sample name.
     * @return A {@link HipoSampleIdentifier} if the supplied string is a valid HIPO sample name,
     * otherwise <code>null</code>.
     */
    public static HipoSampleIdentifier tryParse(String sampleName) {
        Matcher matcher = sampleName =~ REGEX
        if (!matcher.matches()) {
            return null
        }
        String projectLetter = matcher.group(2)
        String projectNumber = matcher.group(3)

        if(!(projectLetter ==~ /[HP]/)) {
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
                /* sampleNumber: */ Integer.parseInt(matcher.group(6)),
                /* experiment: */ matcher.group(7),
                )
    }

    /**
     * The 'original' name, concatenated from the parsed subparts.
     * Example: H456-ABCD-T3-D1
     */
    public String getFullSampleName() {
        return "${pid}-${tissueType.key}${sampleNumber}-${analyteTypeAndNumber}"
    }

    /**
     * The sample type name as it appears in the database, i.e. the tissue type name, followed by
     * the {@link #sampleNumber} if different from 1 or for project 35.
     * Example: TUMOR03
     */
    public String getSampleTypeDbName() {
        String dbName = tissueType.name()
        // 'default' sample number is 1, only make explicit when not 1
        // except project 35, which always makes it explicit, see sampleType JavaDoc
        if (sampleNumber != 1 || projectNumber == "035") {
            dbName += String.format('%02d', sampleNumber)
        }
        return dbName
    }

    @Override
    public String toString() {
        return fullSampleName
    }
}
