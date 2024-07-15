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

import grails.validation.Validateable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.multipart.MultipartFile
import grails.converters.JSON

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.dataswap.DataSwapValidationException
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.utils.spreadsheet.SpreadsheetParseException
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValidationContext

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class DataSwapController implements CheckAndCall {
    DataSwapService dataSwapService

    static allowedMethods = [
            index   : "GET",
            swapData: "POST",
    ]

    def index() {
        return [
                delimiters: Delimiter.simpleValues(),
                delimiter : Delimiter.COMMA,
        ]
    }

    def swapData(DataSwapCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                ValidationContext context = dataSwapService.validate(cmd.dataSwapFile, cmd.delimiter)
                return render([problems: context.problems*.toProblemDTO()] as JSON)
            } catch (DataSwapValidationException | SpreadsheetParseException e) {
                return response.sendError(HttpStatus.BAD_REQUEST.value(), e.message)
            }
        }
    }
}

class DataSwapCommand implements Validateable {
    MultipartFile dataSwapFile
    boolean dryRun
    Delimiter delimiter

    static constraints = {
        dryRun(nullable: true)
    }
}
