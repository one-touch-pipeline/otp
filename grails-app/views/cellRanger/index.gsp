<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "cellRanger.title", args: [project?.name])}</title>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:if test="${projects}">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]"/>
        <br><br><br><br><br>

        <g:if test="${configExists}">
            <p>
                Select:
                <g:form action="index" method="GET">
                    <g:select name="sampleType.id" from="${allSampleTypes}" optionKey="id" value="${sampleType?.id}"
                              noSelection="${[(""): "All"]}"/>
                    <g:select name="individual.id" from="${allIndividuals}" optionKey="id" value="${individual?.id}"
                              noSelection="${[(""): "All"]}"/>
                    <g:submitButton name="Submit"/>
                </g:form>
            </p>

            <g:if test="${samples}">
                <g:form action="create">
                    <input type="hidden" name="project.id" value="${project?.id}"/>
                    <input type="hidden" name="sampleType.id" value="${sampleType?.id}"/>
                    <input type="hidden" name="individual.id" value="${individual?.id}"/>
                    <table>
                        <tr>
                            <th>${g.message(code: "cellRanger.sampleType")}</th>
                            <th>${g.message(code: "cellRanger.individual")}</th>
                            <th>${g.message(code: "cellRanger.expectedCells")}</th>
                            <th>${g.message(code: "cellRanger.enforcedCells")}</th>
                            <th></th>
                        </tr>
                        <g:each in="${mwps}" var="mwp">
                            <tr>
                                <td>${mwp.sampleType} (${mwp})</td>
                                <td>${mwp.individual}</td>
                                <td>${mwp.expectedCells}</td>
                                <td>${mwp.enforcedCells}</td>
                                <td></td>
                            </tr>
                        </g:each>
                        <tr>
                            <td><ul>
                                <g:each in="${selectedSampleTypes}" var="sampleType">
                                    <li>${sampleType}</li>
                                </g:each>
                            </ul></td>
                            <td><ul>
                                <g:each in="${selectedIndividuals}" var="individual">
                                    <li>${individual}</li>
                                </g:each>
                            </ul></td>
                            <td><label>${g.message(code: "cellRanger.expectedCells")}:
                                <input name="expectedCells"/>
                            </label>
                            </td>
                            <td><label>${g.message(code: "cellRanger.enforcedCells")}:
                                <input name="enforcedCells"/>
                            </label></td>
                            <td><g:submitButton name="Save"/></td>
                        </tr>
                    </table>
                </g:form>
            </g:if>
            <g:else>
            <p>
                ${g.message(code: "cellRanger.noSamples")}
            </p>
            </g:else>
        </g:if>
        <g:else>
            ${g.message(code: "cellRanger.noConfig")}
        </g:else>
    </g:if>
    <g:else>
        <h3>${g.message(code: "default.no.project")}</h3>
    </g:else>
</div>
</body>
</html>
