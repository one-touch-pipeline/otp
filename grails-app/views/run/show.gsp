<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="run.show.title"/></title>
</head>
<body>
  <div class="body_grow">
    <h1><g:message code="run.show.title"/></h1>

    <h1><g:message code="run.show.general"/></h1>

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
                <g:if test="${run.dateExecuted != null}">
                    ${(new Date(run.dateExecuted.getTime())).format("yyyy-MM-dd")}
                </g:if>
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

    <h1><g:message code="run.show.processing"/></h1>
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
        <h1><g:message code="run.show.dataFiles"/></h1>
        <table>
            <g:each var="file" in="${metaDataFiles}">
            <tbody>
                <tr>
                    <td><g:link controller="metadataImport" action="details" id="${file.runSegmentId}">${file.filePath}/${file.fileName}</g:link></td>
                    <td>${(new Date(file.dateCreated.getTime())).format("yyyy-MM-dd")}</td>
                </tr>
            </tbody>
            </g:each>
        </table>
        <g:each var="track" in="${seqTracks}">
        <table>
            <thead>
                <tr>
                    <th colspan="3"><g:link controller="seqTrack" action="show" id="${track.key.id}">${track.key.laneId} ${track.key.sample}<br/>${track.key.seqType}</g:link></th>
                    <th colspan="3">insert size: ${track.key.insertSize}</th>
                    <th colspan="3">number of base pairs: ${track.key.nBaseString()}</th>
                </tr>
            </thead>
            <tbody>
            <g:each  var="file" in="${track.value.files}">
                <tr>
                    <td>-</td>
                    <td>s</td>
                    <td><g:link controller="dataFile" action="showDetails" id="${file.id}">${file.fileName}</g:link></td>
                    <td><b><g:link controller="projectOverview" action="index" params="[project: file.project]">${file.project}</g:link></b></td>
                    <td class="true">metadata</td>
                    <td class="${file.fileExists}">lsdf</td>
                    <td class="${file.fileLinked}">view-by-pid</td>
                    <td>${String.format("%.1f GB", file.fileSize/1e9)}</td>
                    <td>${file.dateFileSystem ? (new Date(file.dateFileSystem.getTime())).format("yyyy-MM-dd") : '&nbsp;' }</td>
                    <td>
                        <g:if test="${fastqcLinks.get(file.id)}">
                            <g:link controller="fastqcResults" action="show" id="${file.id}"><g:message code="run.show.fastqc"/></g:link>
                        </g:if>
                    </td>
                </tr>
            </g:each>
            <g:each var="alignment" in="${track.value.alignments}">
                <g:each var="file" in="${alignment.value}">
                    <tr>
                        <td>-</td>
                        <td>a</td>
                        <td><g:link controller="dataFile" action="showDetails" id="${file.id}">${file.fileName}</g:link></td>
                        <td>${alignment.key.alignmentParams.pipeline}</td>
                        <td>${alignment.key.executedBy}</td>
                        <td class="${file.fileExists}">lsdf</td>
                        <td class="${file.fileLinked}">view-by-pid</td>
                        <td>${String.format("%.1f GB", file.fileSize/1e9)}</td>
                        <td>${file.dateFileSystem ? (new Date(file.dateFileSystem.getTime())).format("yyyy-MM-dd") : '&nbsp;' }</td>
                        <td>&nbsp;</td>
                    </tr>
                </g:each>
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
                    <td><g:link controller="dataFile" action="showDetails" id="${file.id}">${file.fileName}</g:link></td>
                    <td>${de.dkfz.tbi.otp.ngsdata.MetaDataEntry.findByDataFileAndKey(file, keys[0])?.value}</td>
                    <td>${de.dkfz.tbi.otp.ngsdata.MetaDataEntry.findAllByDataFileAndKey(file, keys[1])}</td>
                    <td class="true"><g:message code="run.show.metaData"/></td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
  </div>
</body>
<asset:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</asset:script>
</html>
