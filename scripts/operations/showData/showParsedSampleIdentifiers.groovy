/*
 * Copyright 2011-2024 The OTP authors
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

/**
 * The script executes a chosen SampleIdentifierParser for all given SampleIdentifiers and
 * returns the extracted values project, individual, sampleType and the identifier itself.
 */
import de.dkfz.tbi.otp.ngsdata.SampleIdentifierService
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypeService
import de.dkfz.tbi.otp.parser.*
import de.dkfz.tbi.otp.parser.covid19.Covid19SampleIdentifierParser
import de.dkfz.tbi.otp.parser.hipo.*
import de.dkfz.tbi.otp.parser.inform.InformSampleIdentifierParser
import de.dkfz.tbi.otp.parser.itccp4.ITCC_4P_Parser
import de.dkfz.tbi.otp.parser.pedion.PedionParser

// ----------------------------------------
// input area

// sample name
// lines are trimmed, lines starting with # are ignored
String input = """
#identifier1
#identifier2

"""

// uncomment the parser to use
SampleIdentifierParser parser
// parser = ctx.deepSampleIdentifierParser
// parser = ctx.informSampleIdentifierParser
// parser = ctx.hipoSampleIdentifierParser
// parser = ctx.hipo2SampleIdentifierParser
// parser = ctx.hipo2SamplePreparationLabSampleIdentifierParser
// parser = ctx.iTCC_4P_Parser
// parser = ctx.OE0290_EORTC_SampleIdentifierParser
// parser = ctx.simpleProjectIndividualSampleTypeParser
// parser = ctx.pedionParser
// parser = ctx.covid19SampleIdentifierParser

// ----------------------------------------
// work area
assert parser: 'Please select one parser'

List<String> output = []
List<String> table = []
List<String> notParsable = []

static String getSanitizedSampleTypeDbName(String sampleTypeName) {
    SampleType sampleType = SampleTypeService.findSampleTypeByName(sampleTypeName)
    if (sampleType) {
        return sampleType.name
    }
    String sanitizedSampleTypeDbName = new SampleIdentifierService().getSanitizedSampleTypeDbName(sampleTypeName)
    SampleType sanitizedSampleType = SampleTypeService.findSampleTypeByName(sanitizedSampleTypeDbName)
    if (sanitizedSampleType) {
        return sanitizedSampleType.name
    }
    return sanitizedSampleTypeDbName
}

try {
    SampleType.withTransaction {
        input.split('\n')*.trim().findAll { String line ->
            line && !line.startsWith('#')
        }.each { String line ->
            output << "parse: ${line}"
            ParsedSampleIdentifier identifier = parser.tryParse(line)
            if (identifier) {
                table << [
                        identifier.projectName,
                        identifier.pid,
                        getSanitizedSampleTypeDbName(identifier.sampleTypeDbName),
                        identifier.fullSampleName,
                ].join(', ')
            } else {
                notParsable << line
            }
        }
    }
} finally {
    println "parsed input (${output.size()}):"
    println output.join('\n')
    println '\n----------------------------------------------\n'
    println "parsed values (${table.size()}):"
    println "project, pid, sampleType, identifier"
    println table.join('\n')
    println '\n----------------------------------------------\n'
    println "not parsable identifiers (${notParsable.size()}):"
    println notParsable.join('\n')
}
''
