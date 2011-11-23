<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of Workflows</title>
<link rel='stylesheet' href='http://www.datatables.net//release-datatables/media/css/demo_table.css' />
<jqDT:resources/>
<g:javascript library="jquery.dataTables" />
<g:javascript src="jquery.timeago.js"/>
</head>
<body>
  <div class="body">
    <div id="workflowOverview">
        <table id="workflowOverviewTable">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>&nbsp;</th>
                    <th>Workflow</th>
                    <th>#</th>
                    <th>Last Success</th>
                    <th>Last Failure</th>
                    <th>Duration</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $('#workflowOverviewTable').dataTable({
                sPaginationType: "full_numbers",
                bJQueryUI: true,
                bProcessing: true,
                bServerSide: true,
                sAjaxSource: '${request.contextPath + '/processes/listData'}',
                fnServerData: function(sSource, aoData, fnCallback) {
                    $.ajax({
                        "dataType": 'json',
                        "type": "POST",
                        "url": sSource,
                        "data": aoData,
                        "success": function(json) {
                            for (var i=0; i<json.aaData.length; i++) {
                                var rowData = json.aaData[i];
                                rowData[0] = '<img src="${request.contextPath}/images/status/' + statusToImage(rowData[0].name) + '" alt="' + rowData[0].name + '" title="' + rowData[0].name + '">';
                                rowData[2] = '<a href="${request.contextPath}/processes/show/' + rowData[2].id + '">' + rowData[2].name + '</a>'
                                if (rowData[4]) {
                                    rowData[4] = $.timeago(new Date(rowData[4]));
                                } else {
                                    rowData[4] = "-";
                                }
                                if (rowData[5]) {
                                    rowData[5] = $.timeago(new Date(rowData[5]));
                                } else {
                                    rowData[5] = "-";
                                }
                                if (rowData[6]) {
                                    rowData[6] = formatTimespan(rowData[6]);
                                } else {
                                    rowData[6] = "-";
                                }
                            }
                            fnCallback(json);
                    }});
                }
            });
        });
        /**
         * Converts a status name to the image anme.
        **/
        function statusToImage(status) {
            switch (status) {
            case "NEW":
                return "empty.png";
            case "DISABLED":
                return "grey.png";
            case "RUNNING":
                return "green_anime.gif";
            case "RUNNINGFAILEDBEFORE":
                return "red_anime.gif";
            case "SUCCESS":
                return "green.png";
            case "FAILURE":
                return "red.png";
            }
            return null;
        }

        /**
         * Converts the time span in msec into a human readable format.
        **/
        function formatTimespan(msec) {
            if (msec < 1000) {
                return msec + " msec";
            }
            var sec = Math.round(msec/1000);
            msec = msec%1000;
            if (sec < 60) {
                return sec + " sec " + msec + " msec";
            }
            var min = msec/60;
            sec = Math.round(sec % 60);
            if (min < 60) {
                return min + " min " + sec + " sec";
            }
            var hour = min/60;
            min = Math.round(min % 60);
            if (hour < 24) {
                return hour + " h " + min + " min";
            }
            var day = Math.round(hour/24);
            hour = hour%24;
            return day + " day(s) " + hour + " h";
        }
    </g:javascript>
  </div>
</body>
</html>