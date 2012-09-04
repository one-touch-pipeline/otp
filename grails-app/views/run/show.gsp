<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Run details</title>
</head>
<body>
  <div class="body">
    <ul>
        <g:if test="${nextRun}">
            <li class="button"><g:link action="show" id="${nextRun.id}">next run</g:link></li>
        </g:if>
        <g:if test="${previousRun}">
            <li class="button"><g:link action="show" id="${previousRun.id}">previous run</g:link></li>
        </g:if>
    </ul>


    <h1>General</h1>

    <table>
       <tr>
            <td class="myKey">name</td>
            <td class="myValue">${run.name}</td>
       </tr>
       <tr>
            <td class="myKey">sequencing center</td>
            <td class="myValue">${run.seqCenter.name.toLowerCase()}</td>
       </tr>
       <tr>
            <td class="myKey">sequencing technology</td>
            <td class="myValue">${run.seqPlatform}</td>
       </tr>
       <tr>
            <td class="myKey">date executed</td>
            <td class="myValue">
                <g:if test="${run.dateExecuted != null}">
                    ${(new Date(run.dateExecuted.getTime())).format("yyyy-MM-dd")}
                </g:if>
            </td>
       </tr>
       <tr>
            <td class="myKey">date created</td>
            <td class="myValue">${run.dateCreated}</td>
       </tr>
       <tr>
            <td class="myKey">meta data path</td>
            <td class="myValue">${run.initialMDPaths()}</td>
       </tr>
       <tr>
            <td class="myKey">final locations</td>
            <td class="myValue">
                <g:each var="path" in="${finalPaths}">
                    ${path} <br/>
                </g:each>
            </td>
       </tr>
    </table>

    <h1>Processing</h1>
    <table>
        <g:each var="processParameter" in="${processParameters}">
        <tr>
            <td class="myKey">${processParameter.process.jobExecutionPlan.name}</td>
            <td><g:link controller="processes" action="process" id="${processParameter.process.id}">
                show details
            </g:link></td>
        </tr>
        </g:each>
    </table>


    <div class="tableBlock">
        <h1>Data Files</h1>
        <table>
            <g:each var="file" in="${metaDataFiles}">
            <tbody>
                <tr>
                    <td>${file.fileName}</td>
                    <td>${(new Date(file.dateCreated.getTime())).format("yyyy-MM-dd")}</td>
                    <td>${file.used}</td>
                </tr>
            </tbody>
            </g:each>
        </table>
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
            <g:each  var="file" in="${track.value.files}">
                <tr>
                    <td>-</td>
                    <td>s</td>
                    <td><g:link controller="dataFile" action="showDetails" id="${file.id}">${file.fileName}</g:link></td>
                    <td>${file.project}</td>
                    <td class="${file.metaDataValid}">meta-data</td>
                    <td class="${file.fileExists}">lsdf</td>
                    <td class="${file.fileLinked}">linked</td>
                    <td>${String.format("%.1f GB", file.fileSize/1e9)}</td>
                    <td>${file.dateFileSystem ? (new Date(file.dateFileSystem.getTime())).format("yyyy-MM-dd") : '&nbsp;' }</td>
                    <td>
                        <g:if test="${fastqcLinks.get(file.id)}">
                            <g:link controller="fastqcResults" action="show" id="${file.id}">fastqc</g:link>
                            ${fastqcSummary.get(file.id)}
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
                        <td class="${file.fileLinked}">linked</td>
                        <td>${String.format("%.1f GB", file.fileSize/1e9)}</td>
                        <td>${file.dateFileSystem ? (new Date(file.dateFileSystem.getTime())).format("yyyy-MM-dd") : '&nbsp;' }</td>
                        <td></td>
                    </tr>
                </g:each>
            </g:each> 
            </tbody>
        </table>
        </g:each>
        <table>
            <thead>
                <tr>
                    <td colspan="4">Files not used:</td>
                </tr>
            </thead>
            <tbody>
            <g:each var="file" in="${errorFiles}">
                <tr>
                    <td><g:link controller="dataFile" action="showDetails" id="${file.id}">${file.fileName}</g:link></td>
                    <td>${de.dkfz.tbi.otp.ngsdata.MetaDataEntry.findByDataFileAndKey(file, keys[0]).value}</td>
                    <td>${de.dkfz.tbi.otp.ngsdata.MetaDataEntry.findAllByDataFileAndKey(file, keys[1])}</td>
                    <td class="${file.metaDataValid}">meta-data</td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
  </div>
</body>
</html>
