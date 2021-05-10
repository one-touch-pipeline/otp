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

$(function() {
    var cell = $(".anchor:target").parent();
    cell.css("border", "2px solid black");
    $(window).on('hashchange', function () {
        if (cell) {
            cell.css("border", "1px solid white");
        }
        cell = $(".anchor:target").parent();
        cell.css("border", "2px solid black");
    });
});

// sticky header
$(function() {
    $('table').each(function() {
        if ($(this).find('thead').length > 0 && $(this).find('th').length > 0) {
            // Clone <thead>
            var $w	   = $(window),
                $t	   = $(this),
                $thead = $t.find('thead').clone(),
                $col   = $t.find('thead, tbody').clone();

                // Add class, remove margins, reset width and wrap table
            $t
            .addClass('sticky-enabled')
            .css({
                margin: 0,
                width: '100%'
            }).wrap('<div class="sticky-wrap" />');

            if ($t.hasClass('overflow-y')) {
                $t.removeClass('overflow-y').parent().addClass('overflow-y');
            }

            // Create new sticky table head (basic)
            $t.after('<table class="sticky-thead" />');

            // If <tbody> contains <th>, then we create sticky column and intersect (advanced)
            if ($t.find('tbody th').length > 0) {
                $t.parent().parent().after('<table class="sticky-col" /><table class="sticky-intersect" />');
            }

            // Create shorthand for things
            var $stickyHead  = $(this).siblings('.sticky-thead'),
                $stickyCol   = $(this).parent().parent().siblings('.sticky-col'),
                $stickyInsct = $(this).parent().parent().siblings('.sticky-intersect'),
                $stickyWrap  = $(this).parent('.sticky-wrap');

            $stickyHead.append($thead);

            $stickyCol
            .append($col)
                .find('thead tr').find('th:gt(0)').remove()
                .end().end()
                .find('tbody td').remove();

            $stickyInsct
            .append($col.clone())
                .find('thead tr').find('th:gt(0)').remove()
                .end().end()
                .find('tbody').remove();

            var setPositionAndSize = function () {
                var stickyCols = $('.sticky-col');
                var stickyIntersects = $('.sticky-intersect');
                var i;

                for (i = 0; i < stickyCols.length; i++) {
                    var stickyCol = $(stickyCols[i]);
                    stickyCol.css({
                        left: stickyCol.parent().find('.fixed-scrollbar-container').position().left,
                        top: stickyCol.parent().find('.fixed-scrollbar-container').position().top
                    });
                }

                for (i = 0; i < stickyIntersects.length; i++) {
                    var stickyIntersect = $(stickyIntersects[i]);
                    stickyIntersect.css({
                        left: stickyIntersect.parent().find('.fixed-scrollbar-container').position().left
                    });
                }

                $t
                .find('thead th').each(function (i) {
                    $stickyHead.find('th').eq(i).width($(this).width());
                })
                .end()
                .find('tr').each(function (i) {
                    $stickyCol.find('tr').eq(i).height($(this).height());
                    $stickyInsct.find('tr').eq(i).height($(this).height());
                });

                // Set width of sticky table head
                $stickyHead.width($t.width());

                // Set width of sticky table col
                $stickyCol.find('th').add($stickyInsct.find('th')).width($t.find('thead th').width());

            },
            repositionStickyHead = function () {
                // Return value of calculated allowance
                var allowance = calcAllowance();

                // Check if wrapper parent is overflowing along the y-axis
                if ($t.height() > $stickyWrap.height()) {
                    // If it is overflowing (advanced layout)
                    // Position sticky header based on wrapper scrollTop()
                    if ($stickyWrap.scrollTop() > 0) {
                        // When top of wrapping parent is out of view
                        $stickyHead.css({
                            opacity: 1,
                            top: $stickyWrap.scrollTop()
                        });
                        $stickyInsct.css({
                            opacity: 1,
                            top: -$stickyWrap.offset().top
                        });
                    } else {
                        // When top of wrapping parent is in view
                        $stickyHead.add($stickyInsct).css({
                            opacity: 0,
                            top: 0
                        });
                    }
                } else {
                    // If it is not overflowing (basic layout)
                    // Position sticky header based on viewport scrollTop
                    if ($w.scrollTop() > $t.offset().top && $w.scrollTop() < $t.offset().top + $t.outerHeight() - allowance) {
                        // When top of viewport is in the table itself
                        $stickyHead.css({
                            opacity: 1,
                            top: $w.scrollTop() - $t.offset().top
                        });
                        $stickyInsct.css({
                            opacity: 1,
                            top: $w.scrollTop()
                        });
                    } else {
                        // When top of viewport is above or below table
                        $stickyHead.add($stickyInsct).css({
                            opacity: 0,
                            top: 0
                        });
                    }
                }
            },
            calcAllowance = function () {
                var a = 0;
                // Calculate allowance
                $t.find('tbody tr:lt(3)').each(function () {
                    a += $(this).height();
                });

                // Set fail safe limit (last three row might be too tall)
                // Set arbitrary limit at 0.25 of viewport height, or you can use an arbitrary pixel value
                if (a > $w.height() * 0.25) {
                    a = $w.height() * 0.25;
                }

                // Add the height of sticky header
                a += $stickyHead.height();
                return a;
            };

        setPositionAndSize();

        $t.parent('.sticky-wrap').scroll(function() {
            repositionStickyHead();
        });

        $w
        .load(function() {
            setPositionAndSize();
            repositionStickyHead();
        })
        .on("resize", function () {
            setPositionAndSize();
            repositionStickyHead();
        })
        .scroll(repositionStickyHead);
        }
    });
});

// fixed horizontal scrollbar
$(function($) {
    var fixedBarTemplate = '<div class="fixed-scrollbar"><div></div></div>';
    var fixedBarCSS = { display: 'none', overflowX: 'scroll', position: 'fixed',  width: '100%', bottom: 0 };
    $('.fixed-scrollbar-container').css('overflow','AUTO');

    $('.fixed-scrollbar-container').each(function() {
        var $container = $(this);
        if ($container[0].offsetWidth < $container[0].scrollWidth) {
            var $bar = $(fixedBarTemplate).appendTo($container).css(fixedBarCSS);

            $bar.scroll(function() {
                $container.scrollLeft($bar.scrollLeft());
            });

            $bar.data("status", "off");
        }
    });

    var fixSize = function() {
        $('.fixed-scrollbar').each(function() {
            var $bar = $(this);
            var $container = $bar.parent();

            $bar.children('div').height(1).width($container[0].scrollWidth);
            $bar.width($container.width()).scrollLeft($container.scrollLeft());
        });

        $(window).trigger("scroll.fixedbar");
    };

    $(window).on("load.fixedbar resize.fixedbar", function() {
        fixSize();
    });

    var scrollTimeout = null;

    $(window).add($('.fixed-scrollbar-container')).on("scroll.fixedbar", function() {
        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(function() {
            $('.fixed-scrollbar-container').each(function() {
                var $container = $(this);
                var $bar = $container.children('.fixed-scrollbar');

                if ($bar.length && ($container[0].scrollWidth > $container.width())) {
                    var containerOffset = {top: $container.offset().top, bottom: $container.offset().top + $container.height() };
                    var windowOffset = {top: $(window).scrollTop(), bottom: $(window).scrollTop() + $(window).height() };

                    if ((containerOffset.top > windowOffset.bottom) || (windowOffset.bottom > containerOffset.bottom)) {
                        if ($bar.data("status") == "on") {
                            $bar.hide().data("status", "off");
                        }
                    } else {
                        if ($bar.data("status") == "off") {
                            $bar.show().data("status", "on");
                        }
                        $bar.scrollLeft($container.scrollLeft());
                    }
                } else {
                    if ($bar.data("status") == "on") {
                        $bar.hide().data("status", "off");
                    }
                }
            });
        }, 20);
    });

    $(window).trigger("scroll.fixedbar");
});
