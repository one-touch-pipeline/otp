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

import grails.compiler.GrailsCompileStatic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.administration.DocumentService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

/*
 * show information about OTP
 */
@PreAuthorize('permitAll')
class InfoController {
    static allowedMethods = [
            about               : "GET",
            numbers             : "GET",
            contact             : "GET",
            imprint             : "GET",
            partners            : "GET",
            templates           : "GET",
            faq                 : "GET",
            newsBanner          : "GET",
    ]

    DocumentService documentService
    ProcessingOptionService processingOptionService

    @GrailsCompileStatic
    def about() {
        String aboutOtp = processingOptionService.findOptionAsString(OptionName.GUI_ABOUT_OTP)
        return [
                aboutOtp: aboutOtp,
        ]
    }

    @GrailsCompileStatic
    def numbers() { }

    @GrailsCompileStatic
    def contact() { }

    @GrailsCompileStatic
    def imprint() {
        String imprint = processingOptionService.findOptionAsString(OptionName.GUI_IMPRINT)
        return [
                imprint: imprint,
        ]
    }

    @GrailsCompileStatic
    def partners() { }

    @PreAuthorize('isFullyAuthenticated()')
    def templates() {
        return [
                availableTemplates: documentService.listDocuments().collect {
                    [
                            document    : it,
                            fullFileName: documentService.getDocumentFileNameWithExtension(it),
                    ]
                },
        ]
    }

    @GrailsCompileStatic
    def faq() { }

    String newsBanner() {
        return render(processingOptionService.findOptionAsString(ProcessingOption.OptionName.NEWS_BANNER))
    }
}
