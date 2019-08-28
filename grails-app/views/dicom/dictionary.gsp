%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<html>
    <head>
        <meta name="layout" content="main"/>
        <title><g:message code="dicom.info.title" /></title>
    </head>

    <body>
        <div class="body">
            <h2><g:message code="dicom.info.dict.title" /></h2>
            <table>
                <tr>
                    <th><g:message code="dicom.info.dict.header.codeSystemName"/></th>
                    <th><g:message code="dicom.info.dict.header.codeValue"/></th>
                    <th><g:message code="dicom.info.dict.header.codeMeaning"/></th>
                    <th><g:message code="dicom.info.dict.header.usage"/></th>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>0</td>
                    <td><g:message code="dicom.info.dict.event.switch"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.event.switch.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>1</td>
                    <td><g:message code="dicom.info.dict.event.activated"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.event.activated.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>2</td>
                    <td><g:message code="dicom.info.dict.event.deactivated"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.event.deactivated.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>3</td>
                    <td><g:message code="dicom.info.dict.event.granted"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.event.granted.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>4</td>
                    <td><g:message code="dicom.info.dict.event.revoked"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.event.revoked.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>100</td>
                    <td><g:message code="dicom.info.dict.role.admin"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.role.admin.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>101</td>
                    <td><g:message code="dicom.info.dict.role.operator"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.role.operator.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>102</td>
                    <td><g:message code="dicom.info.dict.role.switch"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.role.switch.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>1000</td>
                    <td><g:message code="dicom.info.dict.permission.file"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.permission.file.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>1001</td>
                    <td><g:message code="dicom.info.dict.permission.manage"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.permission.manage.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>1002</td>
                    <td><g:message code="dicom.info.dict.permission.delegate"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.permission.delegate.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>1003</td>
                    <td><g:message code="dicom.info.dict.permission.access"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.permission.access.usage" /></td>
                </tr>
                <tr>
                    <td>99DCMOTP</td>
                    <td>2000</td>
                    <td><g:message code="dicom.info.dict.object.permission"/></td>
                    <td class="keep-whitespace"><g:message code="dicom.info.dict.object.permission.usage" /></td>
                </tr>
            </table>
        </div>
    </body>
</html>
