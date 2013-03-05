<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="seqScan.title"/></title>
</head>
<body>
  <div class="body">
    <h1><g:message code="seqScan.title"/></h1>
    <h2><g:message code="seqScan.general"/></h2>

    <div class="tableBlock">
    <table>
       <tr>
            <td class="myKey"><g:message code="seqScan.general.individual"/></td>
            <td>
            <g:link
                    controller="individual" 
                    action="show" 
                    id="${scan.sample.individual.id}"
            >
                ${scan.sample.individual}
            </g:link>
            </td>
       <tr> 
            <td class="myKey"><g:message code="seqScan.general.sample"/></td>
            <td class="myValue">${scan.sample}</td>
       </tr>  
       <tr> 
            <td class="myKey"><g:message code="seqScan.general.sequencingType"/></td>
            <td class="myValue">${scan.seqType}</td>
       </tr>
       <tr> 
            <td class="myKey"><g:message code="seqScan.general.technologyPlatform"/></td>
            <td class="myValue">${scan.seqPlatform}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqScan.general.date"/></td>
            <td>${scan.dateCreated}</td>
       </tr>
    </table>
    </div>

    <h2><g:message code="seqScan.status"/></h2>

    <div class="tableBlock">
    <table>
      <tr>
        <td class="myKey"><g:message code="seqScan.status"/></td>
        <td>${scan.state}</td>
      </tr> 
      <tr>
        <td class="myKey"><g:message code="seqScan.status.qualityStatus"/></td>
        <td>${scan.qcState}</td>
      </tr>
    </table>
    </div>

    <h2><g:message code="seqScan.status.sequencingTracks"/></h2>

    <div class="tableBlock">
    <table>
        <tr>
            <td class="myKey"><g:message code="seqScan.status.numberOfLanes"/></td>
            <td>${scan.nLanes}</td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="seqScan.status.numberOfBasePairs"/></td>
            <td>${scan.basePairsString()}</td>
        </tr>
        <tr>
            <td class="myKey"><g:message code="seqScan.status.coverage"/></td>
            <td>${scan.coverage}</td>
        </tr>
    </table>
    </div>

    <h2><g:message code="seqScan.runsLanesMerging"/></h2>

    <g:each var="run" in="${runs}">

        <div class="tableBlock">
            <h3>
                <g:link controller="run" action="show" id="${run}">${run}</g:link>
            </h3>
            <table>
                <tbody>
                <g:each var="track" in="${tracks}">
                    <g:if test="${track.seqTrack.run.name == run}">
                    <tr>
                        <td><strong>${track.seqTrack.laneId}</strong></td>
                        <td>${track.seqTrack.sample}</td>
                        <td>${track.seqTrack.nBaseString()}</td>
                        <td>${track.seqTrack.insertSize}</td>
                        <g:if test="${de.dkfz.tbi.otp.ngsdata.AlignmentLog.countBySeqTrack(track.seqTrack) == 0}">
                            <td><g:message code="seqScan.runsLanesMerging.noAlignment"/></td>
                        </g:if>
                        <g:else>
                            <td>
                                <ul>
                                <g:each var="alignment" in="${de.dkfz.tbi.otp.ngsdata.AlignmentLog.findAllBySeqTrack(track.seqTrack)}">
                                    <li>${alignment.alignmentParams.pipeline}</li>
                                </g:each>
                                </ul>
                            </td>
                        </g:else>
                    </tr>
                    </g:if>
                </g:each>
                </tbody>
            </table>
        </div>
    </g:each>

    <div class="tableBlock">
        <h2><g:message code="seqScan.mergedAlignmentFiles"/></h2>
        <table>
            <tbody>
            <g:each var="log" in="${de.dkfz.tbi.otp.ngsdata.MergingLog.findAllBySeqScan(scan)}">
                <g:each var="dataFile" in="${de.dkfz.tbi.otp.ngsdata.MergedAlignmentDataFile.findAllByMergingLog(log)}">
                    <tr>
                        <td><g:link controller="mergedAlignmentDataFile" action="show" id="${dataFile.id}">${dataFile.fileName}</g:link></td>
                        <td>${dataFile.fileSizeString()}</td>
                        <td class="dataFile.indexFileExists"><g:message code="seqScan.mergedAlignmentFiles.indexed"/></td>
                        <td>${log.executedBy}</td>
                        <td>${log.status}</td>
                        <td>${log.alignmentParams}</td>
                    </tr>
                </g:each>
            </g:each>
            </tbody>
        </table>
    </div>
  </div>
</body>
</html>
