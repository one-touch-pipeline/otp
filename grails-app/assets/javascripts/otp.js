/*
 * Copyright 2011-2020 The OTP authors
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

$.otp = {
    contextPath:      $("head meta[name=contextPath]").attr("content"),
    projectName:      $("head meta[name=projectName]").attr("content"),
    projectParameter: $("head meta[name=projectParameter]").attr("content"),

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

        var parameters;
        if ($.otp.projectName) {
            parameters = $.extend({ [$.otp.projectParameter]: $.otp.projectName }, options.parameters);
        } else {
            parameters = options.parameters;
        }
        if (parameters !== undefined && parameters && Object.keys(parameters).length > 0) {
            link += "?";
            counter = 0;
            for (parameter in parameters) {
                if (parameters.hasOwnProperty(parameter)) {
                    if (counter > 0) {
                        link += "&";
                    }
                    link += parameter + "=" + encodeURIComponent(parameters[parameter]);
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
    button = $("<div class=\"close-info-box\"><button></button></div>");
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

$.otp.getDownloadButton = function (columnSelector, fileName) {
    const defaultFileName = document.title.replaceAll(" ", "_");
    const date = new Date();
    const formattedDate = date.getUTCFullYear() + '-' + (date.getUTCMonth()+1) + '-' + date.getUTCDate();

    return [{
        extend: 'csvHtml5',
        text: 'Download CSV',
        title: (fileName || defaultFileName) + '_' + formattedDate,
        footer: false,
        exportOptions: {
            columns: columnSelector || ':visible'
        }
    }]
}

$.otp.getDownloadButtonServerSide = function (downloadLink) {
    return [{
        extend: 'csv',
        text: 'Download CSV',
        titleAttr: 'Attention: Download can take a while',
        action: function (e, dt, node, config) {
            var iframe = document.createElement('iframe');
            iframe.style.height = "0px";
            iframe.style.width = "0px";
            iframe.style.border = "0px";
            iframe.src = downloadLink();
            document.body.appendChild(iframe);
        }
    }]
}

/**
 * Applies the default OTP-wide select2 to the jquery selection.
 *
 * This is needed as a separate function, to make it available for the "destroy>clone>add-back" dance when cloning selects (e.g. for multi-input-fields).
 * Select2 is very picky about its cloning-procedures, see:
 *   - https://stackoverflow.com/questions/17175534/cloned-select2-is-not-responding
 *   - https://github.com/select2/select2/issues/2522
 *
 * @param jqSelection JQ selector, e.g. "$('.use-select-2').not('.dont-use-select-2')"
 *        Careful: This method assumes that the selection only contains `select` tags.
 *        If other tags are in there, strange things will happen.
 */
$.otp.applySelect2 = function (jqSelection) {
    /** Syncs the HTML5 validity styling between the 'real' select, and its Select2 imitation */
    function syncValidity(realSelect) {
        const s2 = $(realSelect).data('select2').$selection; // voodoo to go from the 'real' select to its select2 imitation input-field.
        if (realSelect.checkValidity()) {
            s2.removeClass('select-2-otp-error');
        } else {
            s2.addClass('select-2-otp-error');
        }
    }

    jqSelection.select2({
        minimumResultsForSearch: 10,

        /*
         * Select2 doesn't fully resolve width-properties through complicated style-sheets,
         * especially when the target element is hidden during document load.
         * The official fix is to use "width: 'computedstyle'", but this calculates each width
         * per-item, which makes for sloppy alignment in table-like settings, such as DataTableFilter.
         * To work around this, items using select2 will often need an explicit `min-width` or `width` attribute
         * To further enforce certain OTP-styling, we also add the custom 'select-2-otp-theme' to Select2's
         * generated elements, so we have something to "grab on to" for our own CSS.
         *
         * official fix: https://github.com/select2/select2/pull/5559
         * background: https://github.com/select2/select2/issues/3733
         *   > "Select2 doesn't inherit styling from the parent elements because the effort involved is pretty intense,
         *   >  and it's not something we're really interested in implementing."
         */
        width: 'computedstyle',
        containerCssClass: 'select-2-otp-theme'
    }).change(function (ev) { // handler to keep html5 validity-state and -visuals synced between 'real' select and select2 elements.
        syncValidity(ev.target);
    }).each(function (i, e) { // initial sync when first applying, to match other fields (:invalid style is shown immediately on page load.)
        syncValidity(e);
    });
};

$(document).ready(function() {
    $.otp.highlight(window.location.pathname);

    // apply select2 fancy search-box to all select-boxes that want it.
    // Currently opt-in, by setting css-class on the g:select
    // Since there are some cases where select2 doesn't integrate nicely we also provide
    // an opt-out class. If you use the opt-out, please leave a comment explaining why!
    $.otp.applySelect2($('.use-select-2').not('.dont-use-select-2'));

    var t = $('[title]');
    if ($.fn.tooltip.Constructor === undefined) {
        // use jQueryUI
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
    } else {
        // use Bootstrap
        t.tooltip({ trigger: 'hover focus click' });
    }
});
