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
        <li class="button"><g:link action="show" id="${nextId}">next run</g:link></li>
        <li class="button"><g:link action="show" id="${prevId}">previous run</g:link></li>
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
            <td class="myValue">${run.seqTech}</td>
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
            <td class="myKey">data path</td>
            <td class="myValue">${run.dataPath}</td>
       </tr>
       <tr>
            <td class="myKey">meta data path</td>
            <td class="myValue">${run.mdPath}</td>
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


    <h1>Status</h1>
    <table>
       <tr>
            <td class="myKey">is complete</td>
            <td class="${run.complete}">${run.complete}</td>
       </tr>
               <tr>
            <td class="myKey">all files used</td>
            <td class="${run.allFilesUsed}">${run.allFilesUsed}</td>
       </tr>
       <tr>
            <td class="myKey">data moved to final location</td>
            <td class="${run.finalLocation}">${run.finalLocation}</td>
       </tr>
            <tr>
            <td class="myKey">run from multiple sources</td>
            <td class="${run.multipleSource}">${run.multipleSource}</td>
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


    <div class="myHeaderWide">
        Data Files
    </div>

    <div class="myContentWide">

        <table>

           <g:each var="file" in="${de.dkfz.tbi.otp.ngsdata.DataFile.findAllByRun(run)}">
           <g:if test="${file.fileType.type.toString() == 'METADATA'}">
           <tr>
                <td>${file.fileName}</td>
                <td>${(new Date(file.dateCreated.getTime())).format("yyyy-MM-dd")}</td>
                <td>${file.fileType.type}</td>
<%--                <td>${file.fileExists}</td>--%>
<%--                <td>${file.used}</td>--%>
           </tr>
           </g:if>
           </g:each>
        </table>

        <%--  Data Files located on Seq Tracks  --%>
        <table>
        <g:each var="track" in="${de.dkfz.tbi.otp.ngsdata.SeqTrack.findAllByRun(run, [sort: 'laneId'])}">

            <tr>
                <td class="miniHeader" colspan="3">${track}</td>
                <td class="miniHeader" colspan="3">insert size: ${track.insertSize}</td>
                <td class="miniHeader" colspan="3">number of base pairs: ${track.nBaseString()}</td>
            </tr>
            <g:each var="file" in="${de.dkfz.tbi.otp.ngsdata.DataFile.findAllBySeqTrack(track)}">
                <tr>
                    <td>-</td>
                    <td>s</td>
                    <td><g:link controller="dataFile" action="showDetails" id="${file.id}">
                        ${file.fileName}</g:link>
                    </td>
                    <td>${file.project}</td>
                    <td class="${file.metaDataValid}">
                        <g:if test="${file.metaDataValid}">meta-data valid</g:if>
                        <g:else>md invalid</g:else>
                    </td>
                    <td class="${file.fileExists}">on LSDF</td>
                    <td class="${file.fileLinked}">linked</td>
                    <td>${String.format("%.1f GB", file.fileSize/1e9)}</td>
                    <td>
                    <g:if test="${file.dateFileSystem}">
                    ${(new Date(file.dateFileSystem.getTime())).format("yyyy-MM-dd")}
                    </g:if>
                    <g:else>
                    &nbsp;
                    </g:else>
                    </td>
                </tr>
            </g:each>
            <g:each var="alignment" in="${de.dkfz.tbi.otp.ngsdata.AlignmentLog.findAllBySeqTrack(track)}">
                <g:each var="file" in="${de.dkfz.tbi.otp.ngsdata.DataFile.findAllByAlignmentLog(alignment)}">
                    <tr>
                        <td>-</td>
                        <td>a</td>

                        <td><g:link controller="dataFile" action="showDetails" id="${file.id}">
                            ${file.fileName}</g:link>
                        </td>

                        <td>${alignment.alignmentParams.programName}
                        <td>${alignment.executedBy}</td>

                        <td class="${file.fileExists}">on LSDF</td>
                        <td class="${file.fileLinked}">linked</td>
                        <td>${String.format("%.1f GB", file.fileSize/1e9)}</td>
                        <td>
	                    <g:if test="${file.dateFileSystem}">
	                    ${(new Date(file.dateFileSystem.getTime())).format("yyyy-MM-dd")}
	                    </g:if>
	                    <g:else>
	                    &nbsp;
	                    </g:else>
                        </td>
                    </tr>
                </g:each>
            </g:each> 
          </g:each>
        </table>

        <%--  Data Files with errors  --%>
        <table>
            <g:if test="${run.allFilesUsed == false}">
            <tr>
                <td class="miniHeader" colspan="4">Files not used:</td>
            </tr>
            </g:if>

            <g:each var="file" in="${de.dkfz.tbi.otp.ngsdata.DataFile.findAllByRun(run, [sort: 'fileName'])}">
            <g:if test="${file.used == false}">
                <tr>
                    <td><g:link controller="dataFile" action="showDetails" id="${file.id}">
                        ${file.fileName}</g:link></td>
                    <td>${de.dkfz.tbi.otp.ngsdata.MetaDataEntry.findByDataFileAndKey(file, keys[0]).value}</td>
                    <td>${de.dkfz.tbi.otp.ngsdata.MetaDataEntry.findAllByDataFileAndKey(file, keys[1])}</td>
                    <td class="${file.metaDataValid}">meta-data</td>
            </g:if>
            </g:each>
        </table>

      </div>

  </div>
</body>
</html>