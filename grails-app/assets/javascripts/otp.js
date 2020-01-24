/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*jslint browser: true */
/*global $ */

//= require modules/jqueryDatatables.js

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
        if (typeof link !== 'string') {
            link = link.toString();
        }
        if (typeof component !== 'string') {
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
                    link += parameter + "=" + encodeURIComponent(options.parameters[parameter]);
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
 * Shared definition for a datatables view.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param sourcePath The path to the ajax resource
 * @param sortOrder {@code true} for ascending, {@code false} for descending initial sorting of first column
 * @param jsonCallback (optional) the callback to invoke when json data has been returned. Gets one argument json
 * @param columnDefs (optional) Array of column definitions, can be used to enable/disable sorting of columns
 * @param postData (optional) Array of additional data to be added to the POST requests
 * @param height (optional) height in pixels
 * @param dataTableArguments (optional) map with additional arguments for initialization of dataTable
 */
$.otp.createListView = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData, height, dataTableArguments) {
    "use strict";
    var config = {
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
    };
    if (height !== undefined) {
        config.sScrollY = ($('.body').height() - height);
    }
    $.extend(config, dataTableArguments);
    $(selector).dataTable(config);
    $.otp.refreshTable.setup(selector, 10000);
    if (height !== undefined) {
        $.otp.resizeBodyInit(selector, height);
    }
};
$.otp.createInfinityScrollListView = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData, height, dataTableArguments) {
    "use strict";
    var config = {
        sDom: '<i> T rt<"clear">S',
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
                scroller: {
                    loadingIndicator: true
                },
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
    };
    if (height !== undefined) {
        config.sScrollY = ($('.body').height() - height);
    }
    $.extend(config, dataTableArguments);
    $(selector).dataTable(config);
    $.otp.refreshTable.setup(selector, 10000);
    if (height !== undefined) {
        $.otp.resizeBodyInit(selector, height);
    }
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
    var menuElement = $('.menu a[href="' + path + '"]').not('.menuLinkContainer');
    var menuElementParent = menuElement.parents('.nav_container');

    menuElement.attr('style', 'color: #fafafa;');
    if (menuElementParent.is('li')) {
        menuElementParent.children().attr('style', 'color: #fafafa;');
    }
};

/** used for data tables */
$.otp.resizeBodyInit = function (table, margin) {
    "use strict";
    $(window).resize(function () {
        $(table + '_wrapper' + ' .dataTables_scrollBody').height(($('.body').height() - margin));
    });
};

$.otp.tableButtons = [
    {
        extend: 'csvHtml5',
        text: 'Download CSV',
        footer: false,
        exportOptions: {
            columns: ':visible'
        }
    }
];

$(document).ready(function() {
    $('#project').select2();
    $('.use-select-2').select2();

    var t = $('[title]');
    t.tooltip();
    t.on('click', function () {
        $(this)
            .tooltip({
                open: function (event, ui) {
                    var $element = $(event.target);
                    ui.tooltip.click(function () {
                        $element.tooltip('close');
                    });
                },
            })
            .tooltip('open');
    });
});
