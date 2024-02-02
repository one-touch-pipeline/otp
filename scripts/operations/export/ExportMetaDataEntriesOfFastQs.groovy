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

import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * This script exports the Fastq metadata entries for a selection of fastq files.
 * output is written to ${ScriptOutputPath}/export/
 *
 * WARNING: the MetaDataEntries represent the raw, unedited data that we received during data installation workflow.
 * They are NOT updated when sampleswaps or data corrections are done.
 * Thus, you are likely to get outdated data with this export script.
 */

////////////////////////////////////////////////////////////
// CONFIG

// output label (no extension, is used to make a .csv and a .done file)
String output_name = "ticketnumber-something-metadata"

// query for which fastq files to export the metadata
List<RawSequenceFile> fastq_to_export = RawSequenceFile.createCriteria().listDistinct {
    seqTrack {
        or {
            sample {
                individual {
                    or {
                        'in'('pid', [
                                '',
                        ])
                        project {
                            'in'('name', [
                                    '',
                            ])
                        }
                    }
                }
            }
            ilseSubmission(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                'in'('ilseNumber', [
                        -1
                ])
            }
        }
    }
}

// END CONFIG
////////////////////////////////////////////////////////////
// (but see DEBUG limit below)

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

FileSystem fileSystem = fileSystemService.remoteFileSystem

// where to put output
Path output_dir = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve("export")
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(output_dir)
Path output =    output_dir.resolve("${output_name}.csv")
Path done_flag = output_dir.resolve("${output_name}.done")

println("going to write output to ${output}")

// metadata columns of interest: everything normally in a metadata file we receive.
List<MetaDataColumn> wanted_columns = MetaDataColumn.values()
List<MetaDataKey> keys = MetaDataKey.findAllByNameInList(wanted_columns*.name())

// get metadata columns for our datafiles
List<MetaDataEntry> metaDataEntries = MetaDataEntry.createCriteria().list {
    'in'('sequenceFile', fastq_to_export)
    'in'('key', keys)
    order('sequenceFile') // Caveat: ordering clause is vitally important for output-logic below to be correct
}

// output our data ============================================================
// Note: the OTP Console will probably time out with a proxy/gateway error while doing this, but the script remains
// running. Look for the ".done" file to see when we are truly finished.
output.withPrintWriter { w ->
    // before we forget: the header
    w.write(wanted_columns.join("\t"))
    w.write("\n")

    // combine entries per datafile into a single line.
    Map<String, String> line_buffer = new HashMap()
    RawSequenceFile previous_fastq

    for (MetaDataEntry mde : metaDataEntries) {
        current_fastq = mde.sequenceFile
        // They're sorted by datafile, so if it "changes", we've just iterated into a new data-file
        // and our line_buffer should hold the complete selection for the "old" one
        if (current_fastq != previous_fastq && previous_fastq != null) { // "!= null" skip outputting almost-empty ...
            // .. record on first line, which ruins the alignment otherwise.

            // write everything for the old one to file.
            writeln(line_buffer, previous_fastq, w, wanted_columns)
            // don't keep any values, in case they are not overwritten by the next fastq
            line_buffer.clear()
        }

        // store this metadata property for this file in our buffer
        line_buffer[mde.key.name] = mde.value
        previous_fastq = current_fastq
    }

    // write the final buffer. The final file doesn't have a "different" one following it to trigger it otherwise.
    //   (This line was absolutely, honestly part of the first iteration of this script, and wasn't accidentally
    //   forgotten before someone else pointed it out! Honest!)
    writeln(line_buffer, previous_fastq, w, wanted_columns)
} // END printwriter

private void writeln(Map<String, String> line_buffer, RawSequenceFile the_file, PrintWriter w, List<MetaDataColumn> wanted_columns) {
    line = []

    // emit the column values in header-order
    wanted_columns.each { MetaDataColumn header ->
        // full path instead of filename for FastQ; other properties as-is
        if (header == MetaDataColumn.FASTQ_FILE) {
            // view-by-pid path probably won't exist anymore if data was withdrawn. This is good because you'll notice it missing.
            line << ctx.lsdfFilesService.getFileViewByPidPath(the_file)
        } else {
            line << line_buffer.getOrDefault(header.toString(), "")
        }
    }

    w.write(line.join("\t"))
    w.write("\n")
}

// joyously announce our success!
String done_message = "Done, at ${new Date()}\n"
println(done_message)
// In case the console timed out, so we can't see other messages: write a marker file to filesystem
done_flag.withPrintWriter { Writer w ->
    w.write(done_message)
}

null // suppress (potentially huge) console result output.
