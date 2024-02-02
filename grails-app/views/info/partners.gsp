%{--
  - Copyright 2011-2024 The OTP authors
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
    <meta name="layout" content="info"/>
    <title><g:message code="info.partners.title" /></title>
</head>

<body>
    <h1><g:message code="info.partners.title" /></h1>
        <table style="margin-left: 50px">
            <tr>
                <td style="padding:15px">
                    <a href="https://www.dkfz.de/en/dktk/" target="_blank">
                        <img src="${assetPath(src: 'non-free/dktk.jpg')}" alt="Deutsches Konsortium für Translationale Krebsforschung" width="190" height="200">
                    </a>
                </td>
                <td style="padding:15px">
                    <a href="http://www.hipo-heidelberg.org/" target="_blank" >
                        <img src="${assetPath(src: 'non-free/hipo.png')}" alt="HIPO" width="170" height="170">
                    </a>
                </td>
                <td style="padding:15px">
                    <a href="https://www.denbi.de/" target="_blank" >
                        <img src="${assetPath(src: 'non-free/denbi.png')}" alt="deNBI" width="240" height="70">
                    </a>
                </td>
            </tr>
            <tr style="background: white">
                <td colspan="2" style="padding:15px">
                    <a href="https://www.nct-heidelberg.de/" target="_blank" >
                        <img src="${assetPath(src: 'non-free/nct.jpg')}" alt="NCT POP" width="480" height="90">
                    </a>
                </td>
                <td style="padding:15px">
                    <a href="https://www.kitz-heidelberg.de/" target="_blank" >
                        <img src="${assetPath(src: 'non-free/kitz.png')}" alt="KiTZ" height="150">
                    </a>
                </td>
            </tr>
            <tr>
                <td style="padding:15px">
                    <a href="http://www.deutsches-epigenom-programm.de/" target="_blank" >
                        <img src="${assetPath(src: 'non-free/logoDeep.png')}" alt="DEEP" width="150" height="150">
                    </a>
                </td>

                <td style="padding:15px">
                    <a href="http://ihec-epigenomes.org/" target="_blank" >
                        <img src="${assetPath(src: 'non-free/ihec.png')}" alt="IHEC" width="200" height="70">
                    </a>
                </td>
                <td style="padding:15px">
                    <a href="http://icgc.org/" target="_blank" >
                        ICGC (International Cancer Genome Consortium)
                    </a>
                </td>
            </tr>
        </table>
</body>
</html>
