<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Details of Sequencing Scan</title>
</head>
<body>
  <div class="body">

    <H1>General</H1>

    <div class="myContent">
    <table>
       <tr>
            <td class="myKey">individual</td>
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
            <td class="myKey">sample</td>
            <td class="myValue">${scan.sample}</td>
       </tr>  
       <tr> 
            <td class="myKey">sequencing type</td>
            <td class="myValue">${scan.seqType}</td>
       </tr>
       <tr> 
            <td class="myKey">technology platform</td>
            <td class="myValue">${scan.seqPlatform}</td>
       </tr>
       <tr>
            <td class="myKey">date</td>
            <td>${scan.dateCreated}</td>
       </tr>
    </table>
    </div>

    <h1>Status</h1>   

    <div class="myContent">
    <table>
      <tr>
        <td class="myKey">status</td>
        <td>${scan.state}</td>
      </tr> 
      <tr>
        <td class="myKey">quality status</td>
        <td>${scan.qcState}</td>
      </tr>
    </table>
    </div>

    <h1>Sequencing tracks</h1>

    <div class="myContent">
    <table>
        <tr>
            <td class="myKey">number of lanes</td>
            <td>${scan.nLanes}</td>
        </tr>
        <tr>
            <td class="myKey">number of base pairs</td>
            <td>${scan.basePairsString()}</td>
        </tr>
        <tr>
            <td class="myKey">coverage</td>
            <td>${scan.coverage}</td>
        </tr>
    </table>
    </div>

    <h1>Runs, Lanes, Merging</h1>

    <g:each var="run" in="${runs}">

        <div class="myHeader">
            <g:link controller="run" action="display" id="${run}">
                ${run}
            </g:link>
        </div>
        <div class="myContent">

        <table>
           <g:each var="track" in="${tracks}">
               <g:if test="${track.seqTrack.run.name == run}">
               <tr>
                    <td><strong>${track.seqTrack.laneId}</strong></td>
                    <td>${track.seqTrack.sample}</td>
                    <td>${track.seqTrack.nBaseString()}</td>
                    <td>${track.seqTrack.insertSize}</td>

                    <g:if test="${de.dkfz.tbi.otp.ngsdata.AlignmentLog.countBySeqTrack(track.seqTrack) == 0}">
                        <td>no alignment</td>
                    </g:if><g:else>

                    <g:each var="alignment" in="${de.dkfz.tbi.otp.ngsdata.AlignmentLog.findAllBySeqTrack(track.seqTrack)}">
                        <td>${alignment.alignmentParams.pipeline}</td>
                    </g:each>
                    </g:else>

               </tr>
               </g:if>
           </g:each>
        </table>
      </div>
    </g:each>
    

    <div class="myHeader">
        Merged alignment files
    </div>
    
    <div class="myContent">
    <table>
    <g:each var="log" in="${de.dkfz.tbi.otp.ngsdata.MergingLog.findAllBySeqScan(scan)}">
        <tr>
            <g:each var="dataFile" in="${de.dkfz.tbi.otp.ngsdata.DataFile.findAllByMergingLog(log)}">
                <td>
                <g:link controller="dataFile" action="show" id="${dataFile.id}">
                ${dataFile.fileName}
                </g:link>
                </td>
                <td>${dataFile.fileSizeString()}</td>
            </g:each>
            <td>${log.executedBy}</td>
            <td>${log.status}</td>
            <td>${log.alignmentParams}</td>
    </g:each>
    </table>
    </div>

  </div>
</body>
</html>