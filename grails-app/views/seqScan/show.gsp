<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Details of Sequencing Scan</title>
</head>
<body>
  <div class="body">

    <H1>Details of Sequence Scan:  ${scan}</H1>

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