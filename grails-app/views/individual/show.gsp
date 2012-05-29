<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Individual overview</title>
</head>
<body>
  <div class="body">

    <ul>
        <li class="button"><g:link action="show" id="${nextId}">next individual</g:link></li>
        <li class="button"><g:link action="show" id="${prevId}">previous individual</g:link></li>
    </ul>

    <h1>Details of Individual</h1>
    <div class="myContent">
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
    </div>

    <h1>Samples</h1>
    <div class="myContent">
    <table>
        <g:each var="sample" in="${de.dkfz.tbi.otp.ngsdata.Sample.findAllByIndividual(ind)}">
            <tr>
                <td class="myKey">${sample.type}</td>
                <td class="myValue">${de.dkfz.tbi.otp.ngsdata.SampleIdentifier.findAllBySample(sample)}</td>
            </tr>
        </g:each>
    </table>
    </div>

<%--    ${mergedBams}--%>

    <H1>Sequencing Scans</H1>
    <g:form>

    <g:each var="type" in="${seqTypes}">

        <div class="myHeaderWide">
            ${type}
        </div>
        <div class="myContentWide">

        <table>
           <tr>
                <td></td>
                <td class="microHeader">type</td>
                <td class="microHeader">platform</td>
                <td class="microHeader">status</td>
                <td class="microHeader">center</td>
                <td class="microHeader">#lanes</td>
                <td class="microHeader">coverage</td>
                <td class="microHeader">#bases</td>
                <td class="microHeader">insert size</td>
                <td class="microHeader">merging</td>
                <td class="microHeader">IGV</td>
           </tr> 
           <g:each var="scan" in="${seqScans}">
               <g:if test="${scan.seqType == type}">
               <tr>
                    <td>
                        <g:link controller="seqScan" action="show" id="${scan.id}">
                            |details|
                        </g:link>
                    </td>
                    <td><strong>${scan.sample.type}</strong></td>
                    <td>${scan.seqPlatform}</td>
                    <td>${scan.state}</td>
                    <td>${scan.seqCenters.toLowerCase()}</td>

                    <td>${scan.nLanes}</td>
                    <td>${scan.coverage}</td>
                    <td>${scan.basePairsString()}</td>
                    <td>${scan.insertSize}</td>

                    <td class="${de.dkfz.tbi.otp.ngsdata.MergingLog.countBySeqScan(scan) != 0}">
                        <g:formatBoolean boolean="${de.dkfz.tbi.otp.ngsdata.MergingLog.countBySeqScan(scan) != 0}"
                            true="merged" false="not merged"
                        />
                    </td>
                    <td><g:if test="${igvMap.get(scan.id)}">
                            <g:checkBox name="${scan.id}" value="${false}"/>
                        </g:if>
                    </td>
               </tr>
               </g:if>
           </g:each>
        </table>
      </div>
    </g:each>

    <h1>Analysis Results</h1>
    <div class="myContentWide">
    <table>
        <tr>
            <g:each var="mutation" in="${muts}">
                ${mutation.gene}, 
            </g:each>
        </tr>
    </table>
    </div>

    <h1>Data Access</h1>
        <div class="myContentWide" align="right">
            <g:actionSubmit class="button" value="Start IGV" action="igvStart"/>
<%--            <g:actionSubmit class="button" value="Get IGV Session File" action="igvDownload"/>--%>
        </div>
    </g:form>

  </div>
</body>
</html>