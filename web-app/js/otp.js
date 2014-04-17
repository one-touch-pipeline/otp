/*jslint browser: true */
/*global $ */

var tableTools_button_options = [{"sExtends":"csv", "bFooter": false},{"sExtends":"pdf","bFooter": false}];
$.otp = {
    contextPath: $("head meta[name=contextPath]").attr("content"),
    /**
     * Helper method to extend the given link by a further component.
     * Ensures that there is exactly one slash between the link and the further
     * component.
     * @param link The original link
     * @param component The new component to add
     * @returns link + '/' + component
     */
    addLinkComponent: function (link, component) {
        "use strict";
        if (component === undefined || !component) {
            return link;
        }
        if (!isNaN(link)) {
            link = link.toString();
        }
        if (!isNaN(component)) {
            component = component.toString();
        }
        if (link.charAt(link.length - 1) !== "/" && component.charAt(0) !== "/") {
            link += "/";
        } else if (link.charAt(link.length - 1) === "/" && component.charAt(0) === "/") {
            component = component.substring(1);
        }
        link += component;
        return link;
    },
    /**
     * Creates an URL from the passed in options in the same way as the Grails
     * createLink tag. The options is an object with the following attributes:
     * <ul>
     * <li>controller</li>
     * <li>action</li>
     * <li>id</li>
     * <li>parameters</li>
     * </ul>
     *
     * All elements are optional. Parameters is an object which gets serialized
     * into key/value pairs for the query part of the URL.
     *
     * If all elements are provided the following link structure is generated:
     * <strong>/applicationContextPath/controller/action/id?key1=value1&key2=value2</strong>
     * @param options The URL parts to construct the link from
     * @returns an URL to be used in e.g. href element of an a-attribute
     */
    createLink: function (options) {
        "use strict";
        var link, parameter, counter;
        link = $.otp.contextPath;
        if (options === undefined || !options) {
            return link;
        }
        link = $.otp.addLinkComponent(link, options.controller);
        link = $.otp.addLinkComponent(link, options.action);
        link = $.otp.addLinkComponent(link, options.id);
        if (options.parameters !== undefined && options.parameters && Object.keys(options.parameters).length > 0) {
            link += "?";
            counter = 0;
            for (parameter in options.parameters) {
                if (options.parameters.hasOwnProperty(parameter)) {
                    if (counter > 0) {
                        link += "&";
                    }
                    link += parameter + "=" + options.parameters[parameter];
                    counter += 1;
                }
            }
        }
        return link;
    },
    /**
     * Creates the HTML markup for an a element from the passed in options.
     * For the actual link (href) the same attributes in options are supported
     * as in {@link $.otp.createLink}. In addition the following attributes in
     * options are supported:
     * <ul>
     * <li>title</li>
     * <li>text</li>
     * <li>target</li>
     * </ul>
     *
     * Title is used for the title attribute of the a-attribute and text is used
     * for the innerHTML element of the a-attribute
     * @param options The options defining the hyperlink
     * @returns {String} Markup for HTML element a
     */
    createLinkMarkup: function (options) {
        "use strict";
        var link, text, title, target;
        link = '<a href="' + $.otp.createLink(options) + '"';
        text = "";
        if (options !== undefined && options) {
            if (options.text !== undefined && options.text) {
                text = options.text;
            }
            if (options.title !== undefined && options.title) {
                title = options.title;
            }
            if (options.target !== undefined && options.target) {
                target = options.target;
            }
        }
        if (title !== undefined) {
            link += ' title="' + title + '"';
        }
        if (target !== undefined) {
            link += ' target="' + target + '"';
        }
        return link + '>' + text + '</a>';
    }
};

$.i18n.properties({
    name: 'messages',
    path: $.otp.createLink({
        controller: 'js',
        action: 'i18n/'
    }),
    mode: "map"
});

$.otp.message = function (message, warning) {
    "use strict";
    if (!message) {
        return;
    }
    var classes, button, divCode;
    classes = "message";
    if (warning) {
        classes += " errors";
    }
    button = $("<div class=\"close\"><button></button></div>");
    $("button", button).click(function () {
        $(this).parent().parent().remove();
    });
    divCode = $("<div class=\"" + classes + "\"><p>" + message + "</p></div>");
    button.appendTo(divCode);
    divCode.append($("<div style=\"clear: both;\"></div>"));
    $("#infoBox").append(divCode);
};

$.otp.infoMessage = function (message) {
    "use strict";
    this.message(message, false);
};

$.otp.warningMessage = function (message) {
    "use strict";
    this.message(message, true);
};

/**
 * Generates a generic datatable.
 * The datatable expects an ajax source at "dataTableSource" which provides a JSON response.
 * The first column of the data source is expected to be a complex datatabe consisting of <em>id</em>
 * and a descriptive <em>text</em>.
 * @param selector The jQuery selector of the table
 * @param showLink The link to the controller/action to show more details of an element
 */
$.otp.genericList = function (selector, showLink) {
    "use strict";
    $(selector).dataTable({
        sDom: 'T<"clear">lfrtip',
        oTableTools: {
            sSwfPath : $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
            aButtons : tableTools_button_options
        },
        bFilter: true,
        bProcessing: true,
        bServerSide: true,
        bSort: true,
        bJQueryUI: false,
        bAutoWidth: false,
        sAjaxSource: 'dataTableSource',
        bPaginate: false,
        bScrollCollapse: true,
        sScrollY: ($(window).height() - 440),
        bDeferRender: true,
        fnServerData: function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "error": function () {
                    // clear the table
                    fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                },
                "success": function (json) {
                    var i, rowData;
                    for (i = 0; i < json.aaData.length; i += 1) {
                        rowData = json.aaData[i];
                        rowData[0] = $.otp.createLinkMarkup({
                            controller: showLink,
                            id: rowData[0].id,
                            text: rowData[0].text
                        });
                    }
                    fnCallback(json);
                }
            });
        }
    });
};

/**
 * Shared definition for a datatables view.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param sourcePath The path to the ajax resource
 * @param sortOrder {@code true} for ascending, {@code false} for descending initial sorting of first column
 * @param jsonCallback (optional) the callback to invoke when json data has been returned. Gets one argument json
 * @param columnDefs (optional) Array of column definitions, can be used to enable/disable sorting of columns
 * @param postData (optional) Array of additional data to be added to the POST requests
 */
$.otp.createListView = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData) {
    "use strict";
    $(selector).dataTable({
        bFilter: false,
        bJQueryUI: false,
        bSort: true,
        bProcessing: true,
        bServerSide: true,
        bAutoWidth: false,
        bScrollCollapse: true,
        sScrollY: ($('.body').height() - 140),
        bPaginate: false,
        bDeferRender: true,
        sAjaxSource: sourcePath,
        fnServerData: function (sSource, aoData, fnCallback) {
            var i;
            if (postData) {
                for (i = 0; i < postData.length; i += 1) {
                    aoData.push(postData[i]);
                }
            }
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    if (jsonCallback) {
                        jsonCallback(json);
                    }
                    fnCallback(json);
                }
            });
        },
        aoColumnDefs: columnDefs,
        aaSorting: [[0, sortOrder ? "asc" : "desc"]]
    });
    $.otp.refreshTable.setup(selector, 10000);
    $.otp.resizeBodyInit(selector, 140);
};

$.otp.workFlow = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData) {
    "use strict";
    $(selector).dataTable({
        bFilter: false,
        bJQueryUI: false,
        bSort: true,
        bProcessing: true,
        bServerSide: true,
        bAutoWidth: false,
        bScrollCollapse: true,
        sScrollY: ($('.body').height() - 240),
        bScrollInfinite: true,
        bDeferRender: true,
        sAjaxSource: sourcePath,
        fnServerData: function (sSource, aoData, fnCallback) {
            var i;
            if (postData) {
                for (i = 0; i < postData.length; i += 1) {
                    aoData.push(postData[i]);
                }
            }
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    if (jsonCallback) {
                        jsonCallback(json);
                    }
                    fnCallback(json);
                }
            });
        },
        aoColumnDefs: columnDefs,
        aaSorting: [[0, sortOrder ? "asc" : "desc"]]
    });
    $.otp.refreshTable.setup(selector, 10000);
    $.otp.resizeBodyInit(selector, 240);
};

$.otp.registerStep = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData) {
    "use strict";
    $(selector).dataTable({
        bFilter: false,
        bJQueryUI: false,
        bSort: true,
        bProcessing: true,
        bServerSide: true,
        bAutoWidth: false,
        bScrollCollapse: true,
        sScrollY: ($('.body').height() - 230),
        bPaginate: false,
        bDeferRender: true,
        sAjaxSource: sourcePath,
        fnServerData: function (sSource, aoData, fnCallback) {
            var i;
            if (postData) {
                for (i = 0; i < postData.length; i += 1) {
                    aoData.push(postData[i]);
                }
            }
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    if (jsonCallback) {
                        jsonCallback(json);
                    }
                    fnCallback(json);
                }
            });
        },
        aoColumnDefs: columnDefs,
        aaSorting: [[0, sortOrder ? "asc" : "desc"]]
    });
    $.otp.refreshTable.setup(selector, 10000);
    $.otp.resizeBodyInit(selector, 230);
};

/**
 * Handles auto refresh of tables.
 */
$.otp.refreshTable = {
    /**
     * Setup the autorefresh of tables.
     * @param selector A jquery selector for a datatable to be refreshed
     * @param time the time interval for refresh
     */
    setup: function (selector, time) {
        "use strict";
        window.setInterval(function () {
            if ($.otp.autorefresh.enabled && !document.hidden) {
                $(selector).dataTable().fnDraw();
            }
        }, time);
    }
};

/**
 * Handles the enable/disable auto-refresh functionality.
 */
$.otp.autorefresh = {
    /**
     * Whether auto-refresh is currently enabled.
     */
    enabled: false,
    /**
     * Registers the click handler on the links
     */
    setup: function (enabled) {
        "use strict";
        $("#refreshBox a").click($.otp.autorefresh.handleClick);
        $.otp.autorefresh.enabled = enabled;
    },
    /**
     * The click handler for the links. Performs an AJAX request to enable/disable auto-refresh.
     * @param event
     */
    handleClick: function (event) {
        "use strict";
        event.preventDefault();
        $.getJSON($(this).attr("href"), $.otp.autorefresh.ajaxHandler);
    },
    /**
     * Callback for AJAX request to enable/diasble auto-refresh
     * @param data
     */
    ajaxHandler: function (data) {
        "use strict";
        if (data.enabled === true) {
            $("#refreshBox span.enable").hide();
            $("#refreshBox span.disable").show();
            $.otp.autorefresh.enabled = true;
        } else if (data.enabled === false) {
            $("#refreshBox span.enable").show();
            $("#refreshBox span.disable").hide();
            $.otp.autorefresh.enabled = false;
        }
    }
};

$.otp.simpleSearch = {
    search: function (element, table) {
        "use strict";
        $('#' + table).dataTable().fnFilter($(element).val());
    }
};

$.otp.highlight = function (path) {
    "use strict";
    var pathSplit = path.split("/");
    if (pathSplit.length > 3) {
        $('.menuContainer #' + pathSplit[2] + ' a').attr('style', 'color: #fafafa;');
        if (pathSplit[2] === "overviewMB" || pathSplit[2] === "projectOverview" || pathSplit[2] === "projectStatistic") {
            $('.menuContainer #overview a:first').attr('style', 'color: #fafafa;');
        } else if (pathSplit[2] === "userAdministration" || pathSplit[2] === "group" || pathSplit[2] === "crashRecovery" || pathSplit[2] === "shutdown" || pathSplit[2] === "notification" || pathSplit[2] === "processingOption" || pathSplit[2] === "softwareTool") {
            $('.menuContainer #admin a:first').attr('style', 'color: #fafafa;');
        }
    } else {
        $('.menuContainer #home a').attr('style', 'color: #fafafa;');
    }
};

$.otp.projectOverviewStatistic = {
    register : function() {
        "use strict";
        $('#project_select').change(function() {
            $.otp.graph.projectStatistic.init();
        });
    }
};

$.otp.createListViewProcessingStep = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData) {
    "use strict";
    $(selector).dataTable({
        bFilter: false,
        bJQueryUI: false,
        bSort: true,
        bProcessing: true,
        bServerSide: true,
        bAutoWidth: false,
        bScrollCollapse: true,
        bPaginate: false,
        bDeferRender: true,
        sAjaxSource: sourcePath,
        fnServerData: function (sSource, aoData, fnCallback) {
            var i;
            if (postData) {
                for (i = 0; i < postData.length; i += 1) {
                    aoData.push(postData[i]);
                }
            }
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    if (jsonCallback) {
                        jsonCallback(json);
                    }
                    fnCallback(json);
                }
            });
        },
        aoColumnDefs: columnDefs,
        aaSorting: [[0, sortOrder ? "asc" : "desc"]]
    });
    $.otp.refreshTable.setup(selector, 10000);
};

$.otp.resizeBodyInit = function (table, margin) {
    "use strict";
    $(window).resize(function () {
        $(table + '_wrapper' + ' .dataTables_scrollBody').height(($('.body').height() - margin));
    });
};
$.otp.resizeBodyInit_nTable = function (table, margin) {
    $(table).height(($('.body').height() - margin));
    $(window).resize(function() {
        $(table).height(($('.body').height() - margin));
    })
}
$.otp.growBodyInit = function (margin) {
    "use strict";
    var grow_body_h = $('.body_grow').height();
    if (grow_body_h > ($(window).height() - margin)) {
        $('body').attr('style', 'overflow-y:scroll');
        $('.body_position').attr('style', 'margin-left:' + ((($(window).width() - $('.body_grow').width()) / 2) - 11) + 'px;');
    }
    $(window).resize(function () {
        if (grow_body_h > ($(window).height() - margin)) {
            $('body').attr('style', 'overflow-y:scroll');
            $('.body_position').attr('style', 'margin-left:' + ((($(window).width() - $('.body_grow').width()) / 2) - 11) + 'px;');
        } else {
            $('body').attr('style', 'overflow-y:hidden');
            $('.body_position').attr('style', 'margin-left:auto;');
        }
    });
};
