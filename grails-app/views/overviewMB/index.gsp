<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title>Insert title here</title>
</head>
<body>
    <div class="body">
        <div class="table">
            <h1>Centers Overview</h1>
            <div style="width: 20px; height: 20px;"></div>
            <table>
                <thead>
                    <tr>
                        <th><g:message code="overview.statistic.center.name" /></th>
                        <th><g:message code="overview.statistic.center.totalCount" /></th>
                        <th><g:message code="overview.statistic.center.newCount" /></th>
                    </tr>
                </thead>
                <tbody>
                    <g:each var="line" in="${centers}">
                        <tr>
                            <g:each var="token" in="${line}">
                                <td class="overview_td">
                                    ${token}
                                </td>
                            </g:each>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </div>
        <div style="width: 20px; height: 20px;"></div>
        <div class="table">
            <h1>Sequencing Types Overview</h1>
            <div style="width: 20px; height: 20px;"></div>
            <table>
                <thead>
                    <tr>
                        <th><g:message code="overview.statistic.seq.name" /></th>
                        <th><g:message code="overview.statistic.total.laneCount" /></th>
                        <th><g:message code="overview.statistic.recent.laneCount" /></th>
                    </tr>
                </thead>
                <tbody>
                    <g:each var="line" in="${types}">
                        <tr>
                            <g:each var="token" in="${line}">
                                <td class="overview_td">
                                    ${token}
                                </td>
                            </g:each>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
