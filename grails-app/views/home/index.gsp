<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.welcome.title"/></title>
    <r:require module="lightbox"/>
</head>
<body>
    <div class="body_grow">
        <div class="homeTable">
            <table>
                <tr>
                    <th><g:message code="index.project"/></th>
                    <th><g:message code="index.seqType"/></th>
                </tr>
                <g:each var="row" in="${projectQuery}">
                    <tr>
                        <td><b>${row.key}</b></td>
                        <td><b>${row.value}</b><td>
                    </tr>
                </g:each>
            </table>
        </div>
        <br>
    </div>
</body>
<g:javascript>
    $(function() {
        $('body').attr('style','overflow-y:scroll');
        $('.body_position').attr('style','margin-left:'+((($(window).width()-$('.body_grow').width())/2)-11)+'px;');
        $('.fullScreen').click(function(){
            $(this).parent().parent().parent().children('a').click();
        });
    });
</g:javascript>
</html>