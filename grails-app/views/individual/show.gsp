<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Individual overview</title>
</head>
<body>
  <div class="body">

    <h1>Details of Individual</h1>
    <table>
       <tr> 
            <td class="myKey">PID</td>
            <td class="myValue">${ind.pid}</td>
       </tr>
       <tr>
            <td class="myKey">Mock PID</td>
            <td class="myValue">${ind.mockPid}</td>
       </tr>
       <tr>
            <td class="myKey">Mock Full Name</td>
            <td class="myValue">${ind.mockFullName}</td>
       </tr>
       <tr>
            <td class="myKey">Type</td>
            <td class="myValue">${ind.type}</td>
       </tr>
       <tr>
            <td class="myKey">Project</td>
            <td class="myValue">${ind.project}</td>
       </tr> 
    </table>

    <h1>Samples</h1>
    <table>
        <g:each var="sample" in="${ind.samples}">
            <tr>
                <td class="myKey">${sample.type}</td>
                <td class="myValue">${sample.sampleIdentifiers}</td>
            </tr>
        </g:each>
    </table>

    <H1>Sequencing Scans</H1>

    <g:each var="type" in="${seqTypes}">

        <div class="myHeader">
            ${type}
        </div>
        <div class="myContent">

        <table>
           <g:each var="scan" in="${seqScans}">
               <g:if test="${scan.seqType == type}">
               <tr>
                    <td>
                        <g:link controller="seqScan" action="show" id="${scan.id}">
                            ${scan.id}
                        </g:link>
                    </td>
                    <td><strong>${scan.sample.type}</strong></td>
                    <td>${scan.state}</td>
                    <td>${scan.seqCenters.toLowerCase()}</td>

                    <td>${scan.nLanes}</td>
                    <td>${scan.coverage}</td>
                    <td>${scan.basePairsString()}</td>

                    <g:if test="scan.mergingLog.size() == 0">
                        <td class="false">not merged</td>
                    </g:if>
                    <g:else>
                        <td>merged</td>
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