<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="shortcut icon" href="${assetPath(src: 'v2/otp-favicon.ico')}" type="image/x-icon">
    <title><g:layoutTitle default="OTP"/></title>
    <meta name="contextPath" content="${request.contextPath}">
    <meta name="controllerName" content="${controllerName}">
    <meta name="actionName" content="${actionName}">
    <asset:javascript src="v2/application.js"/>
    <asset:stylesheet src="v2/application.css"/>
    <asset:javascript src="v2/pages/${controllerName}/script.js"/>
    <asset:stylesheet src="v2/pages/${controllerName}/style.css"/>
    <asset:javascript src="v2/pageSpecific.js"/>
    <g:layoutHead/>
</head>
<body>

    <g:layoutBody/>

    <div id="footer">
        <div class="container wrapper">
            <div class="row">
                <div class="col-lg-12 footer-copyright">
                    <ul>
                        <li><a data-toggle="modal" href="#contact">Contact/Imprint</a></li>
                        <li>
                            OTP is operated by the <a href="http://ibios.dkfz.de/tbi/index.php/data-management" target="_blank">Data&nbsp;Management&nbsp;and&nbsp;Genomics&nbsp;IT&nbsp;group&nbsp;(DMG)</a>
                            in the <a href="http://ibios.dkfz.de/tbi/" target="_blank">Theoretical&nbsp;Bioinformatics&nbsp;division</a>
                            at the <a href="https://www.dkfz.de/" target="_blank">German&nbsp;Cancer&nbsp;Research&nbsp;Center&nbsp;(DKFZ)</a>
                        </li>
                        <li class="copyright-grey"><g:render template="/templates/version"/></li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <a class="scroll-to-top js-scroll-to-top" href="#">
        <img class="img-responsive" src="${assetPath(src: 'v2/to-top.png')}" alt="">
    </a>

<otp:modal modalId="contact" title="Contact">
    <p>OTP is operated by</p>
    <p>German Cancer Research Center (DKFZ)<br>Division of Theoretical Bioinformatics<br>Data Management and Genomics IT (DMG)</p>
    <h2>Support</h2>
    <p><a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a></p>
    <h2>Person in charge</h2>
    <p>Jürgen Eils (j.eils@dkfz.de)</p>
    <h2>Postal address</h2>
    <p>Deutsches Krebsforschungszentrum<br>Im Neuenheimer Feld 280<br>69120 Heidelberg<br>Germany</p>
</otp:modal>

    <asset:deferredScripts/>
    <g:render template="/layouts/piwik"/>
</body>
</html>
