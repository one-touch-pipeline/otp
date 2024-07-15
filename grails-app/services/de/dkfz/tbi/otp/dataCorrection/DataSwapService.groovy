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
package de.dkfz.tbi.otp.dataCorrection

import grails.gorm.transactions.Transactional
import groovy.transform.*
import org.apache.commons.io.FilenameUtils
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.dataswap.DataSwapValidationException
import de.dkfz.tbi.otp.dataswap.DataSwapColumn
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.utils.spreadsheet.Spreadsheet
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValidationContext

@Transactional
class DataSwapService {

    MessageSourceService messageSourceService

    @TupleConstructor
    enum HeaderType {
        NEW(" New"),
        OLD(" Old"),
        RAW("")

        final String headerText
    }

    ValidationContext validate(MultipartFile dataSwapFile, Delimiter delimiter) {
        checkFileType(dataSwapFile)
        Spreadsheet dataSwapSpreadsheet = new Spreadsheet(new String(dataSwapFile.bytes), delimiter)
        ValidationContext context = new ValidationContext(dataSwapSpreadsheet)
        Collection<DataSwapValidator> validators = dataSwapValidators

        Long startTimeAll = System.currentTimeMillis()
        int dataCount = context.spreadsheet.dataRows.size()
        log.debug("Start validation of ${dataCount} lines of file, validation started : ${startTimeAll}")
        validators.each {
            Long startTime = System.currentTimeMillis()
            it.validate(context)
            log.debug("Finished ${it.class} took ${System.currentTimeMillis() - startTime}ms for ${dataCount} lines, validation started : ${startTimeAll}")
        }
        log.debug("Finished all ${validators.size()} validators for ${dataCount} lines took " +
                "${System.currentTimeMillis() - startTimeAll}ms, validation started : ${startTimeAll}")
        return context
    }

    protected Collection<DataSwapValidator> getDataSwapValidators() {
        return applicationContext.getBeansOfType(DataSwapValidator).values().sort { it.class.name }
    }

    private void checkFileType(MultipartFile dataSwapFile) throws DataSwapValidationException {
        List<String> allowedFileEndings = ['tsv', 'csv', 'txt']
        if (!allowedFileEndings.contains(FilenameUtils.getExtension(dataSwapFile.originalFilename))) {
            throw new DataSwapValidationException("The file has to have one of the following file endings: ${allowedFileEndings.join(', ')}.")
        }
    }

    List<String> getDataSwapHeaders() {
        return DataSwapColumn.values().collectMany {
            // adds suffix " Old" and a duplicated column with the suffix " New" for the sample swap template
            return (it.duplicateColumnForSampleSwapTemplate) ?
                    [getHeaderName(it, HeaderType.OLD), getHeaderName(it, HeaderType.NEW)] :
                    [getHeaderName(it, HeaderType.RAW)]
        }
    }

    String getHeaderName(DataSwapColumn columnName, HeaderType headerType) {
        return "${messageSourceService.getMessage(columnName.message)}${headerType.headerText}"
    }
}
