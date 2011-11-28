<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of Processes for Workflow ${name}</title>
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
                    <th>Creation Date</th>
                    <th>Last Update</th>
                    <th>Current Processing Step</th>
                    <th>Status</th>
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
                sAjaxSource: '${request.contextPath + '/processes/planData/' +  id + '/'}',
                fnServerData: function(sSource, aoData, fnCallback) {
                    $.ajax({
                        "dataType": 'json',
                        "type": "POST",
                        "url": sSource,
                        "data": aoData,
                        "success": function(json) {
                            for (var i=0; i<json.aaData.length; i++) {
                                var rowData = json.aaData[i];
                                rowData[1] = '<img src="${request.contextPath}/images/status/' + statusToImage(rowData[1].name) + '" alt="' + rowData[1].name + '" title="' + rowData[1].name + '">';
                                if (rowData[2]) {
                                    rowData[2] = $.timeago(new Date(rowData[2]));
                                } else {
                                    rowData[2] = "-";
                                }
                                if (rowData[3]) {
                                    rowData[3] = $.timeago(new Date(rowData[3]));
                                } else {
                                    rowData[3] = "-";
                                }
                                rowData[5] = rowData[5].name;
                            }
                            fnCallback(json);
                    }});
                },
                aaSorting: [[0, "desc"]],
                aoColumnDefs: [
                    { "bSortable": true, "aTargets": [0] },
                    { "bSortable": false, "aTargets": [1] },
                    { "bSortable": true, "aTargets": [2] },
                    { "bSortable": false, "aTargets": [3] },
                    { "bSortable": false, "aTargets": [4] },
                    { "bSortable": false, "aTargets": [5] }
                ]
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
    </g:javascript>
  </div>
</body>
</html>