/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.parser.hipo

import groovy.transform.TupleConstructor
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.util.regex.Matcher

@Component
class HipoSampleIdentifierParser implements SampleIdentifierParser {

    private final static String REGEX = /^(${PIDREGEX})-([${HipoTissueType.values()*.key.join("")}])(\d{1,2})-(([BDRPACWY])(\d{1,2}))$/
    private final static String PIDREGEX = "([A-Z])(\\d\\d\\w)-(?:\\w\\w)?\\w\\w\\w(\\w)"

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

        String sampleNumber = matcher.group(6)

        // all projects except 059 may not have leading zeros in the sampleNumber
        if (projectNumber != "059") {
            if (sampleNumber[0] == "0" && sampleNumber != "0") {
                return null
            }
        }

        // if the analyte type is ChiP (C) there shall be two digits behind. For all other cases only one digit is allowed.
        int expectedAnalyteNumberDigits = (matcher.group(8) == "C") ? 2 : 1
        if (matcher.group(9).size() != expectedAnalyteNumberDigits) {
            return null
        }

        return new HipoSampleIdentifier(
                /* projectNumber: */ projectNumber,
                /* pid: */ matcher.group(1),
                /* tissueType: */ tissueType,
                /* sampleNumber: */ sampleNumber,
                /* experiment: */ matcher.group(7),
                tissueType.specificReferenceGenome,
        )
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        return null
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

    final SpecificReferenceGenome useSpecificReferenceGenome

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
        String dbName = tissueType
        // In project 59 "1" and "01" have different meanings, so use
        // the sample number exactly as given in the sample name.
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
