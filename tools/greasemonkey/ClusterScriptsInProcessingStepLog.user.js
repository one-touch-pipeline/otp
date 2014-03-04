// ==UserScript==
// @name        Display Cluster Scripts in ProcessingStepLog
// @namespace   https://otp.dkfz.de/otp/
// @include     https://otp.dkfz.de/otp/processes/processingStepLog/*
// @version     1.0.2
// @grant       none
// @require     http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js
// ==/UserScript==

/*
This little Greasemonkey script makes the manual restarting of cluster
jobs easier.

When the script is installed and you visit
https://otp.dkfz.de/otp/processes/processingStepLog/***, an additional
textbox will be displayed on top of the page. It contains the cluster
scripts that were submitted by the OTP job. Line breaks and special HTML
characters are displayed properly. The scripts are automatically
selected when you open the page such that all you have to do is pressing
Ctrl+C on that page and then paste it into the shell.
">> ${USER}_job_ids" is appended to each cluster script. Effectively,
this causes all cluster job IDs to be conveniently listed in a file
"unixUser_job_ids" in the current directory.
*/

var prefix = 'executed command: ';
var scripts = ''
jQuery('td[title=Message]').filter(function() { return jQuery(this).text().indexOf(prefix) === 0; }).each(function(index) {
    scripts += $(this).text().substring(prefix.length) + ' >> ${USER}_job_ids \n\n\n';
});
jQuery('<div><h1>Cluster Scripts</h1><textarea id="scripts" rows="10" cols="80">' + scripts + '\n</textarea></div>').prependTo('body');
document.getElementById('scripts').select();
