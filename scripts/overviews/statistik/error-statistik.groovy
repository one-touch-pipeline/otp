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

/**
 * The script search in the given time area for all failed jobs of specific type and show the count for
 * selectable groups.
 *
 * The script support the following groups:
 * - date (with selectable format)
 * - project
 * - seq type
 * - workflow
 * - job (OTP job)
 *
 * It is possible to select which to use and in which order.
 *
 *
 * The script finds errors of workflows working on:
 * - seqType (data installation (current version), fastqc)
 * - bamFiles (Roddy alignment, Cell ranger Alignment)
 * - bamFilePairAnalysis (Snv, Indel, Sophia, Aceseq, runYapsa)
 *
 * The script do not find errors for:
 * - ImportExternalBamFile
 * - OTP alignment
 * - old data installation
 *
 */

import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.util.TimeFormats
import java.text.SimpleDateFormat

//---------------------------
//input

//the input is in the format yyyy-MM-dd
String startDateString = '2019-01-01'
String endDateString = '2019-02-01'

//the format to use for grouping (if date is selected)
String groupFormat = TimeFormats.DATE.format

/*
    The following list defines the grouping.
    Comment/uncomment a line to remove/add a group level
    change order to change hierarchy level
 */
List<Closure> grouping = [
        { it[0].format(groupFormat) }, //Date, using provided format
        { it[3] },// project name
        { it[1] },// Workflow name
        //{ it[2] },// OTP job name
        //{ it[4] },// seq type
]

//---------------------
//work

SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TimeFormats.DATE.format, Locale.ENGLISH)

Date startDate = simpleDateFormat.parse(startDateString)
Date endDate = simpleDateFormat.parse(endDateString) + 1

println "start date: ${startDate}"
println "end date: ${endDate}"

String seqTrackBasedErrors = createErrorQuery('SeqTrack', '')
String bamFileBasedErrors = createErrorQuery('AbstractBamFile', 'workPackage.')
String analysisBasedErrors = createErrorQuery('BamFilePairAnalysis', 'samplePair.mergingWorkPackage1.')

List<List> result = [
        seqTrackBasedErrors,
        bamFileBasedErrors,
        analysisBasedErrors,
].collectMany {
    ProcessParameter.executeQuery(it, [
            startDate: startDate,
            endDate  : endDate,
    ])
}

Map groupData = result.groupBy(grouping)
println printGrouping(groupData)

//---------------------
//functions

static String createErrorQuery(String className, String propertyName) {
    return """
select
    psu.date,
    psu.processingStep.jobDefinition.plan.name,
    psu.processingStep.jobDefinition.name,
    c.${propertyName}sample.individual.project.name,
    c.${propertyName}seqType
from
    ProcessingStepUpdate psu,
    ProcessParameter pp,
    ${className} c
where
    psu.error is not null
    and psu.date >= :startDate
    and psu.date <= :endDate
    and pp.process = psu.processingStep.process
    and cast(pp.value as long) = c.id
""".toString()
}

static String printGrouping(Object o) {
    if (o instanceof Map) {
        Map map = (Map) o
        return '\n' + map.sort {
            it.key
        }.collect { key, value ->
            '' + key + printGrouping(value).replace('\n', '\n    ')
        }.join('\n')
    } else if (o instanceof List) {
        List list = (List) o
        return ": ${list.size()}"
    } else {
        assert "unexpected class: ${o.getClass}"
    }
}

''
