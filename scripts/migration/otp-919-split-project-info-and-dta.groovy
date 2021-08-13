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
import de.dkfz.tbi.otp.project.dta.DataTransferAgreement
import de.dkfz.tbi.otp.project.dta.DataTransferAgreementService
import de.dkfz.tbi.otp.project.ProjectService

/**
 * Creates a bash script to move all dta documents from the project info folder into
 * the new structure: <project>/projectInfo/dta-<dta_id>/ which has been introduced within otp-919.
 */

List<DataTransferAgreement> dataTransferAgreements = DataTransferAgreement.findAll()

List<String> output = []
output << """\
#!/bin/bash

"""

DataTransferAgreementService dataTransferAgreementService = ctx.dataTransferAgreementService
ProjectService projectService = ctx.projectService
dataTransferAgreements.each { DataTransferAgreement dta ->
    String fileOnOldPath = "${projectService.getProjectDirectory(dta.project).toString()}/${ProjectService.PROJECT_INFO}/${dta.dataTransferAgreementDocuments.findAll().first().fileName}"
    String newPath = dataTransferAgreementService.getPathOnRemoteFileSystem(dta).toString()
    output << ("mkdir -m 750 ${newPath}" as String)
    output << ("mv ${fileOnOldPath} ${newPath}" as String)
}

println output.join("\n")

''
