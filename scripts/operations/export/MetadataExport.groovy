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

import de.dkfz.tbi.otp.ngsdata.*

import java.nio.charset.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

/**
 * Creates a TSV file containing the metadata of the specified {@linkplain DataFile}s.
 * The output file has a format which is processable by the {@linkplain MetadataImportService}.
 */
static void writeMetadata(Collection<DataFile> dataFiles, File metadataOutputFile) {
    metadataOutputFile.bytes = getMetadata(dataFiles).getBytes(StandardCharsets.UTF_8)
}

static String getMetadata(Collection<DataFile> dataFiles) {
    Collection<Map<String, String>> allProperties = dataFiles.collect { getMetadata(it) }
    List<String> headers = allProperties*.keySet().sum() as List<String>
    StringBuilder s = new StringBuilder(headers.join('\t')).append('\n')
    for (Map<String, String> properties : allProperties) {
        for (String header : headers) {
            s.append(properties.get(header) ?: '').append('\t')
        }
        s.append('\n')
    }
    return s.toString()
}

static Map<String, String> getMetadata(DataFile dataFile) {
    Map<String, String> properties = [:]

    MetaDataEntry.findAllByDataFile(dataFile).each {
        properties.put(it.key.name, it.value)
    }

    Closure put = { MetaDataColumn column, String value ->
        if (value != null) {
            properties.put(column.toString(), value)
        }
    }

    put(FASTQ_FILE, dataFile.fileName)
    put(MD5, dataFile.md5sum)
    put(MATE, dataFile.mateNumber?.toString())
    put(WITHDRAWN, dataFile.fileWithdrawn ? '1' : null)

    Run run = dataFile.run
    put(RUN_ID, run.name)
    put(RUN_DATE, run.dateExecuted?.format("yyyy-MM-dd"))
    put(CENTER_NAME, run.seqCenter.name)
    put(INSTRUMENT_PLATFORM, run.seqPlatform.name)
    put(INSTRUMENT_MODEL, run.seqPlatform.seqPlatformModelLabel?.name)
    put(SEQUENCING_KIT, run.seqPlatform.sequencingKitLabel?.name)

    SeqTrack seqTrack = dataFile.seqTrack
    String[] laneId = seqTrack.laneId.split('_', 2)
    put(LANE_NO, laneId[0])
    put(BARCODE, laneId.length > 1 ? laneId[1] : null)
    String seqType = seqTrack.seqType.name
    if (seqType.endsWith(SeqType.TAGMENTATION_SUFFIX)) {
        put(SEQUENCING_TYPE, seqType.substring(0, seqType.length() - SeqType.TAGMENTATION_SUFFIX.length()))
        put(TAGMENTATION_BASED_LIBRARY, '1')
    } else {
        put(SEQUENCING_TYPE, seqType)
    }
    put(LIBRARY_LAYOUT, seqTrack.seqType.libraryLayout)
    properties.put('OTP_PID', seqTrack.individual.pid)
    properties.put('OTP_SAMPLE_TYPE', seqTrack.sampleType.name)
    put(SAMPLE_ID, preferredOrLongest(
            properties.get(SAMPLE_ID.toString()), SampleIdentifier.findAllBySample(seqTrack.sample)*.name))
    put(PIPELINE_VERSION, preferredOrLongest(
            properties.get(PIPELINE_VERSION.toString()), SoftwareToolIdentifier.findAllBySoftwareTool(seqTrack.pipelineVersion)*.name))
    put(INSERT_SIZE, String.valueOf(seqTrack.insertSize))
    put(LIB_PREP_KIT, seqTrack.libraryPreparationKit?.name)
    put(ILSE_NO, seqTrack.ilseSubmission?.ilseNumber?.toString())
    put(PROJECT, seqTrack.project.name)
    put(CUSTOMER_LIBRARY, seqTrack.libraryName)

    if (seqTrack instanceof ChipSeqSeqTrack) {
        put(ANTIBODY_TARGET, ((ChipSeqSeqTrack) seqTrack).antibodyTarget.name)
        put(ANTIBODY, ((ChipSeqSeqTrack) seqTrack).antibody)
    }

    return properties
}

static String preferredOrLongest(String preferred, Collection<String> all) {
    return all.contains(preferred) ? preferred : all.max { it.length() }
}
