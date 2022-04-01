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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldDefinition
import de.dkfz.tbi.otp.project.additionalField.TextFieldValue
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Name of the file to import. The name must be absolute.
 */
String fileName = ''
assert fileName: 'No file name given, but this is required'
assert !fileName.contains(' '): 'File name contains spaces, which is not allowed'

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
Path path = fileSystem.getPath(fileName)

Spreadsheet s = new Spreadsheet(path.text)
Project.withTransaction {
    s.dataRows.each { Row row -> //get the values from the spreadsheet for example: project id, organizationalUnit & costCenter
        String projectId = row.getCellByColumnTitle('projectId').text
        String organizationalUnitValue = row.getCellByColumnTitle('organizationalUnit').text
        String costCenterValue = row.getCellByColumnTitle('costCenter').text

        Project.createCriteria().list {
            eq('id', projectId as Long)
        }.each { Project project ->
            AbstractFieldDefinition afdOrganizationalUnit = CollectionUtils.atMostOneElement(AbstractFieldDefinition.findAllByName('Organizational Unit'))
            AbstractFieldDefinition afdCostCenter = CollectionUtils.atMostOneElement(AbstractFieldDefinition.findAllByName('Cost Center'))

            //import values for Organizational Unit
            TextFieldValue tfvOrganizationUnit = new TextFieldValue()
            tfvOrganizationUnit.definition = afdOrganizationalUnit
            tfvOrganizationUnit.textValue = organizationalUnitValue
            tfvOrganizationUnit.save(flush: true)
            project.projectFields.add(tfvOrganizationUnit)

            //import values for Cost Center
            TextFieldValue tfvCostCenter = new TextFieldValue()
            tfvCostCenter.definition = afdCostCenter
            tfvCostCenter.textValue = costCenterValue
            tfvCostCenter.save(flush: true)
            project.projectFields.add(tfvCostCenter)

            project.save(flush: true)
        }
    }
    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}

''
