/*
 * Copyright 2011-2021 The OTP authors
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
package operations.dataCorrection

import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Row

import java.nio.file.*

/**
 * Script to check and update metadata entries for one or multiple ilse.
 *
 * The script takes one or more ilse and check the database content against the metadatafile on the midterm.
 *
 * It will find:
 * - values changed in the file
 * - values of deleted columns
 *
 * The changes is divided to columns OTP is:
 * - using. They can not be deleted/updated
 * - not using (trash columns): This values can be deleted/updated.
 *
 * OTP do not check for new columns. They are ignored.
 *
 * About the flag 'tryRun' the scripts change between only listing and also updating.
 */

//--------------------------
//input

/**
 * The base directory of the midterm.
 */
String midTermDirectory = ""

/**
 * The ilse input. One value per line. All lines are rimmed, empty lines and lines starting with '#' are skipped.
 * The ilse must be known to OTP.
 */
String ilseInput = """
#ilse1
#ilse2

"""

/**
 * flag to switch between test (true) and the changes (false)
 */
boolean testRun = true

//--------------------------
//work

FileSystemService fileSystemService = ctx.fileSystemService
FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

Path midTermPath = fileSystem.getPath(midTermDirectory)

IlseSubmission.withTransaction { def transaction ->

    List<MetaDataEntry> toDelete = []
    List<MetaDataEntry> ignoreForDelete = []
    Map<MetaDataEntry, String> toUpdate = [:]
    Map<MetaDataEntry, String> ignoreForUpdate = [:]

    List<String> columnsUsedByOTP = [
            MetaDataColumn.FASTQ_FILE,
            MetaDataColumn.READ,
            MetaDataColumn.MD5,
            MetaDataColumn.CENTER_NAME,
            MetaDataColumn.RUN_ID,
            MetaDataColumn.RUN_DATE,
            MetaDataColumn.LANE_NO,
            MetaDataColumn.SAMPLE_NAME,
            MetaDataColumn.INDEX,
            MetaDataColumn.SEQUENCING_TYPE,
            MetaDataColumn.INSTRUMENT_MODEL,
            MetaDataColumn.INSTRUMENT_PLATFORM,
            MetaDataColumn.FASTQ_GENERATOR,
            MetaDataColumn.SEQUENCING_READ_TYPE,
            MetaDataColumn.ILSE_NO,
            MetaDataColumn.LIB_PREP_KIT,
            MetaDataColumn.ANTIBODY_TARGET,
            MetaDataColumn.ANTIBODY,
            MetaDataColumn.SEQUENCING_KIT,
            MetaDataColumn.PROJECT,
            MetaDataColumn.BASE_MATERIAL,
            MetaDataColumn.FRAGMENT_SIZE,
            MetaDataColumn.SAMPLE_SUBMISSION_TYPE,
            MetaDataColumn.SINGLE_CELL_WELL_LABEL,
    ]*.name()

    ilseInput.split('\n')*.trim().findAll { String line ->
        line && !line.startsWith('#')
    }.collect {
        CollectionUtils.exactlyOneElement(IlseSubmission.findAllByIlseNumber(it as Integer), "Could not find ilse $it")
    }.each { IlseSubmission ilseSubmission ->
        Path metadataFile = midTermPath.resolve(ilseSubmission.ilseNumber.toString().padLeft(6, '0')).resolve('data').resolve("${ilseSubmission.ilseNumber}_meta.tsv")
        assert Files.exists(metadataFile): "Could not find file ${metadataFile}. Does it exist?"
        assert Files.isReadable(metadataFile): "Could not read file ${metadataFile}. Is it readable for icgcdata?"

        MetadataValidationContext context = MetadataValidationContext.createFromFile(
                metadataFile,
                ctx.dataFilesInGpcfSpecificStructure,
                DirectoryStructureBeanName.GPCF_SPECIFIC.name(),
        )
        Map<String, List<MetaDataEntry>> entriesByFastqFileName = MetaDataEntry.withCriteria {
            dataFile {
                seqTrack {
                    eq('ilseSubmission', ilseSubmission)
                }
            }
        }.groupBy { MetaDataEntry entry ->
            entry.dataFile.fileName
        }

        context.spreadsheet.dataRows.each { Row row ->
            String fastqName = row.getCellByColumnTitle(MetaDataColumn.FASTQ_FILE.name()).text
            List<MetaDataEntry> entries = entriesByFastqFileName[fastqName]

            entries.each { MetaDataEntry entry ->
                String columnName = entry.key.name
                boolean usedByOTP = columnsUsedByOTP.contains(columnName)
                Cell cell = row.getCellByColumnTitle(columnName)
                if (cell) {
                    String valueInDB = entry.value
                    String valueInFile = cell.text
                    if (valueInDB != valueInFile) {
                        if (usedByOTP) {
                            ignoreForUpdate[entry] = valueInFile
                        } else {
                            toUpdate[entry] = valueInFile
                        }
                    }
                } else {
                    if (usedByOTP) {
                        ignoreForDelete << entry
                    } else {
                        toDelete << entry
                    }
                }
            }
        }
    }

    if (ignoreForDelete) {
        println "\n\nFollowing entries are missed in the input of columns OTP is using:"
        println ignoreForDelete.collect { MetaDataEntry entry ->
            [
                    entry.dataFile.seqTrack.ilseSubmission.ilseNumber,
                    entry.dataFile.fileName,
                    entry.key.name,
                    entry.value,
            ].join(' \t')
        }.join('\n')
        println "OTP will keep the values"
    }

    if (toDelete) {
        println "\n\nFollowing entries are missed in the input of columns OTP is not using:"
        println toDelete.collect { MetaDataEntry entry ->
            [
                    entry.dataFile.seqTrack.ilseSubmission.ilseNumber,
                    entry.dataFile.fileName,
                    entry.key.name,
                    entry.value,
            ].join(' \t')
        }.join('\n')
        println "OTP will delete this values"
    }

    if (ignoreForUpdate) {
        println "\n\nFollowing entries are changed of columns OTP is using:"
        println ignoreForUpdate.collect { MetaDataEntry entry, String value ->
            [
                    entry.dataFile.seqTrack.ilseSubmission.ilseNumber,
                    "\t ",
                    entry.dataFile.fileName,
                    "\t ",
                    entry.key.name,
                    ': "',
                    entry.value,
                    '" --> "',
                    value,
                    '"',
            ].join('')
        }.join('\n')
        println "OTP will keep the old values"
    }

    if (toUpdate) {
        println "\n\nFollowing entries are changed of columns OTP is not using:"
        println toUpdate.collect { MetaDataEntry entry, String value ->
            [
                    entry.dataFile.seqTrack.ilseSubmission.ilseNumber,
                    "\t ",
                    entry.dataFile.fileName,
                    "\t ",
                    entry.key.name,
                    ': "',
                    entry.value,
                    '" --> "',
                    value,
                    '"',
            ].join('')
        }.join('\n')
        println "OTP will change them"
    }

    if (toDelete || toUpdate) {
        println "\n\nThere are ${toDelete.size()} objects to delete and ${toUpdate.size()} objects to update"
    } else if (ignoreForDelete || ignoreForUpdate) {
        println "\n\nThere are no changes to do, the only found will be skipped."
        return
    } else {
        println "\n\nno changes found"
        return
    }

    println '\n'
    assert !testRun: "only test, change testRun to false to do the changes"

    toDelete*.delete(flush: false)
    toUpdate.each { MetaDataEntry entry, String value ->
        entry.value = value
        entry.save(flush: false)
    }
    transaction.flush()
    println 'Values delete and updated as announced'
}
