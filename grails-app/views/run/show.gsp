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

<%@ page import="de.dkfz.tbi.otp.project.Project; de.dkfz.tbi.otp.utils.CollectionUtils; de.dkfz.tbi.otp.ngsdata.MetaDataEntry; de.dkfz.tbi.util.TimeFormats" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="run.show.title"/></title>
</head>

<body>
<div class="body">
    <h1><g:message code="run.show.title"/></h1>

    <h2><g:message code="run.show.general"/></h2>

    <table>
        <tr>
            <td class="myKey"><g:message code="run.show.general.name"/></td>
            <td class="myValue">${run.name}</td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="run.show.general.sequencingCenter"/></td>
            <td class="myValue">${run.seqCenter.name.toLowerCase()}</td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="run.show.general.sequencingTechnology"/></td>
            <td class="myValue">${run.seqPlatform}</td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="run.show.general.dateExecuted"/></td>
            <td class="myValue">
                ${TimeFormats.DATE.getFormattedDate(run?.dateExecuted)}
            </td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="run.show.general.dateCreated"/></td>
            <td class="myValue">${run.dateCreated}</td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="run.show.general.finalLocations"/></td>
            <td class="myValue">
                <g:each var="pathWithRunName" in="${finalPaths}">
                    ${pathWithRunName}<br/>
                </g:each>
            </td>
        </tr>
    </table>

    <h2><g:message code="run.show.processing"/></h2>
    <table>
        <g:each var="processParameter" in="${processParameters}">
            <tr>
                <td class="myKey">${processParameter.process.jobExecutionPlan.name}</td>
                <td><g:link controller="processes" action="process" id="${processParameter.process.id}">
                    <g:message code="run.show.processing.showDetails"/>
                </g:link></td>
            </tr>
        </g:each>
    </table>


    <div class="tableBlock">
        <h3><g:message code="run.show.metadataFiles"/></h3>
        <table>
            <g:each var="file" in="${metaDataFileWrapper}">
                <tbody>
                <tr>
                    <td class="metaDataDetails-link">
                        <g:link controller="metadataImport" action="details" id="${file.metaDataFile.fastqImportInstanceId}">
                            <g:message code="metadataImport.details.fullPathSource"/>: ${file.fullPathSource}<br>
                            <g:message code="metadataImport.details.fullPathTarget"/>: ${file.fullPathTarget}
                        </g:link>
                    </td>
                    <td>${TimeFormats.DATE.getFormattedDate(file?.metaDataFile?.dateCreated)}</td>
                </tr>
                </tbody>
            </g:each>
        </table>

        <h3><g:message code="run.show.rawSequenceFiles"/></h3>
        <g:each var="track" in="${seqTracks}">
            <table>
                <thead>
                <tr>
                    <th colspan="3">${track.key.laneId} ${track.key.sample}<br/>${track.key.seqType}</th>
                    <th colspan="3">insert size: ${track.key.insertSize}</th>
                    <th colspan="3">number of base pairs: ${track.key.nBaseString()}</th>
                </tr>
                </thead>
                <tbody>
                <g:set var="archived" value="${track.key.project.state == Project.State.ARCHIVED ? 'archived' : ''}"/>
                <g:set var="deleted" value="${track.key.project.state == Project.State.DELETED ? 'deleted' : ''}"/>
                <g:each var="file" in="${track.value.files}">
                    <tr>
                        <td>-</td>
                        <td>s</td>
                        <td><g:link controller="rawSequenceFile" action="showDetails" id="${file.id}" class="${archived} ${deleted}">
                            ${file.fileName}
                            <g:if test="${archived}">
                                <span title="${track.key.project} is archived">&#128451;</span>
                            </g:if>
                            <g:if test="${deleted}">
                                <span title="${track.key.project} is deleted">&#128465;</span>
                            </g:if>
                        </g:link></td>
                        <td><b><g:link controller="projectOverview" action="index" params="[(projectParameter): file.project.name]">${file.project}</g:link></b>
                        </td>
                        <td class="true">metadata</td>
                        <td class="${file.fileExists}">lsdf</td>
                        <td class="${file.fileLinked}">view-by-pid</td>
                        <td>${String.format("%.1f GB", file.fileSize / 1e9)}</td>
                        <td>${TimeFormats.DATE.getFormattedDate(file?.dateFileSystem)}</td>
                        <td>
                            <g:if test="${fastqcLinks.get(file.id)}">
                                <g:link controller="fastqcResults" action="show" id="${file.id}"><g:message code="run.show.fastqc"/></g:link>
                            </g:if>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </g:each>
        <table>
            <thead>
            <tr>
                <td colspan="4"><g:message code="run.show.filesNotUsed"/></td>
            </tr>
            </thead>
            <tbody>
            <g:each var="file" in="${errorFiles}">
                <tr>
                    %{--This code require metadata fields, since there is no connection to a SeqTrack--}%
                    <td><g:link controller="rawSequenceFile" action="showDetails" id="${file.id}">${file.fileName}</g:link></td>
                    <td>${CollectionUtils.atMostOneElement(MetaDataEntry.findAllBySequenceFileAndKey(file, keys[0]))?.value}</td>
                    <td>${MetaDataEntry.findAllBySequenceFileAndKey(file, keys[1])}</td>
                    <td class="true"><g:message code="run.show.metaData"/></td>
                </tr>
            </g:each>
            <g:if test="${!errorFiles}">
                <tr><td>None</td></tr>
            </g:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
