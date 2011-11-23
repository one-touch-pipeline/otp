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
            <td class="myKey">sequencing technology</td>
            <td class="myValue">${scan.seqTech}</td>
       </tr>
       <tr>
            <td class="myKey">date</td>
            <td>${scan.dateCreated}</td>
       </tr>
    </table>

    <h1>Status</h1>   

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

    <h1>Sequencing tracks</h1>

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

    <h1>Runs and Lanes</h1>

    <g:each var="run" in="${runs}">

        <div class="myHeader">
            <g:link controller="run" action="display" id="${run}">
                ${run}
            </g:link>
        </div>
        <div class="myContent">

        <table>
           <g:each var="track" in="${scan.seqTracks}">
               <g:if test="${track.run.name == run}">
               <tr>
                    <td><strong>${track.laneId}</strong></td>
                    <td>${track.sample}</td>
                    <td>${track.nBaseString()}</td>
                    <td>${track.insertSize}</td>

                    <g:if test="${track.alignmentLog.size() == 0}">
                        <td>no alignment</td>
                    </g:if><g:else>

                    <g:each var="alignment" in="${track.alignmentLog}">
                        <td>${alignment.alignmentParams.programName}</td>
                    </g:each>
                    </g:else>

               </tr>
               </g:if>
           </g:each>
        </table>
      </div>

    </g:each>
  </div>
</body>
</html>