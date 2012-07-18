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
        <g:if test="${next}">
            <li class="button"><g:link action="show" id="${next.id}">next individual</g:link></li>
        </g:if>
        <g:if test="${previous}">
            <li class="button"><g:link action="show" id="${previous.id}">previous individual</g:link></li>
        </g:if>
    </ul>

    <h1>Details of Individual</h1>
    <div class="tableBlock">
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
    <div class="tableBlock">
    <table>
        <g:each var="sample" in="${ind.samples}">
            <tr>
                <td class="myKey">${sample.sampleType.name}</td>
                <td class="myValue">${sample.sampleIdentifiers}</td>
            </tr>
        </g:each>
    </table>
    </div>

<%--    ${mergedBams}--%>

    <H1>Sequencing Scans</H1>
    <g:form>

    <g:each var="type" in="${ind.seqTypes}">

        <div class="tableBlock">
            <h1>${type}</h1>
            <table>
                <thead>
                <tr>
                    <th></th>
                    <th>type</th>
                    <th>platform</th>
                    <th>status</th>
                    <th>center</th>
                    <th>#lanes</th>
                    <th>coverage</th>
                    <th>#bases</th>
                    <th>insert size</th>
                    <th>merging</th>
                    <th>IGV</th>
                </tr>
                </thead>
                <tbody>
                <g:each var="scan" in="${ind.seqScans}">
                    <g:if test="${scan.seqType.id == type.id}">
                        <tr>
                            <td><g:link controller="seqScan" action="show" id="${scan.id}">|details|</g:link></td>
                            <td><strong>${scan.sample.sampleType.name}</strong></td>
                            <td>${scan.seqPlatform}</td>
                            <td>${scan.state}</td>
                            <td>${scan.seqCenters.toLowerCase()}</td>
                            <td>${scan.nLanes}</td>
                            <td>${scan.coverage}</td>
                            <td>${scan.basePairsString()}</td>
                            <td>${scan.insertSize}</td>

                            <td class="${scan.merged}"><g:formatBoolean boolean="${scan.merged}" true="merged" false="not merged"/></td>
                            <td>
                                <g:if test="${igvMap.get(scan.id)}">
                                    <g:checkBox name="${scan.id}" value="${false}"/>
                                </g:if>
                            </td>
                        </tr>
                    </g:if>
                </g:each>
                </tbody>
            </table>
        </div>
    </g:each>

    <h1>Analysis Results</h1>
    <div class="myContentWide">
    <table>
        <tr>
            <g:each var="mutation" in="${ind.mutations}">
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