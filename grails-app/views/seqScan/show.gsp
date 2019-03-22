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

<%@ page import="de.dkfz.tbi.otp.ngsdata.*" contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="seqScan.title"/></title>
</head>
<body>
  <div class="body_grow">
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
                <sec:ifAllGranted roles="ROLE_ADMIN">
                    <g:link controller="run" action="show" id="${run}">${run}</g:link>
                </sec:ifAllGranted>
                <sec:ifNotGranted roles="ROLE_ADMIN">
                    ${run}
                </sec:ifNotGranted>
            </h3>
            <table>
                <tbody>
                <g:each var="track" in="${tracks}">
                    <g:if test="${track.seqTrack.run.name == run}">
                    <tr>
                        <td><strong>${track.seqTrack.laneId}</strong></td>
                        <g:each var="dataFile" in="${track.seqTrack.dataFiles}">
                            <td><strong><g:link controller="dataFile" action="showDetails" id="${dataFile.id}">${dataFile.readName}</g:link></strong></td>
                        </g:each>
                        <td>${track.seqTrack.sample}</td>
                        <td>${track.seqTrack.nBaseString()}</td>
                        <td>${track.seqTrack.insertSize}</td>
                        <g:if test="${AlignmentLog.countBySeqTrack(track.seqTrack) != 0}">
                            <td>
                                <ul>
                                <g:each var="alignment" in="${AlignmentLog.findAllBySeqTrack(track.seqTrack)}">
                                    <li>${alignment.alignmentParams.pipeline}</li>
                                </g:each>
                                </ul>
                            </td>
                        </g:if>
                    </tr>
                    </g:if>
                </g:each>
                </tbody>
            </table>
        </div>
    </g:each>

    <g:if test="${MergingLog.findBySeqScan(scan)}">
        <div class="tableBlock">
            <h2><g:message code="seqScan.mergedAlignmentFiles"/></h2>
            <table>
                <tbody>
                <g:each var="log" in="${MergingLog.findAllBySeqScan(scan)}">
                    <g:each var="dataFile" in="${MergedAlignmentDataFile.findAllByMergingLog(log)}">
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
    </g:if>
  </div>
</body>
<asset:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</asset:script>
</html>
