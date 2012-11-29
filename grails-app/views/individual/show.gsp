<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="individual.show.title"/></title>
</head>
<body>
  <div class="body">

    <ul>
        <g:if test="${next}">
            <li class="button"><g:link action="show" id="${next.id}"><g:message code="individual.show.nextIndividual"/></g:link></li>
        </g:if>
        <g:if test="${previous}">
            <li class="button"><g:link action="show" id="${previous.id}"><g:message code="individual.show.previousIndividual"/></g:link></li>
        </g:if>
    </ul>

    <h1><g:message code="individual.show.title"/></h1>
    <div class="tableBlock">
    <table>
       <tr> 
            <td class="myKey"><g:message code="individual.show.details.pid"/></td>
            <td class="myValue">${ind.pid}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="individual.show.details.mockPid"/></td>
            <td class="myValue">${ind.mockPid}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="individual.show.details.mockFullName"/></td>
            <td class="myValue">${ind.mockFullName}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="individual.show.details.type"/></td>
            <td class="myValue">${ind.type}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="individual.show.details.project"/></td>
            <td class="myValue">${ind.project}</td>
       </tr> 
    </table>
    </div>

    <h1><g:message code="individual.show.samples"/></h1>
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

    <H1><g:message code="individual.show.sequencingScans"/></H1>
    <g:form>

    <g:each var="type" in="${ind.seqTypes}">

        <div class="tableBlock">
            <h1>${type}</h1>
            <table>
                <thead>
                <tr>
                    <th></th>
                    <th><g:message code="individual.show.sequencingScans.type"/></th>
                    <th><g:message code="individual.show.sequencingScans.platform"/></th>
                    <th><g:message code="individual.show.sequencingScans.status"/></th>
                    <th><g:message code="individual.show.sequencingScans.center"/></th>
                    <th><g:message code="individual.show.sequencingScans.numberOfLanes"/></th>
                    <th><g:message code="individual.show.sequencingScans.coverage"/></th>
                    <th><g:message code="individual.show.sequencingScans.numberOfBases"/></th>
                    <th><g:message code="individual.show.sequencingScans.insertSize"/></th>
                    <th><g:message code="individual.show.sequencingScans.merging"/></th>
                    <th><g:message code="individual.show.sequencingScans.igv"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="scan" in="${ind.seqScans}">
                    <g:if test="${scan.seqType.id == type.id}">
                        <tr>
                            <td><g:link controller="seqScan" action="show" id="${scan.id}"><g:message code="individual.show.sequencingScans.details"/></g:link></td>
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

    <g:if test="${ind.mutations}">
        <h1><g:message code="individual.show.analysisResults"/></h1>
        <div class="tableBlock">
            <g:each var="mutation" in="${ind.mutations}">
                ${mutation.gene},
            </g:each>
        </div>
    </g:if>

    <h1><g:message code="individual.show.dataAccess"/></h1>
        <div class="buttons">
            <g:actionSubmit class="button" value="Start IGV" action="igvStart"/>
<%--            <g:actionSubmit class="button" value="Get IGV Session File" action="igvDownload"/>--%>
        </div>
    </g:form>

  </div>
</body>
</html>