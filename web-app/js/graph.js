/*jslint browser: true, devel: true */

$.otp = $.otp || {};
$.otp.graph = {};

$.otp.graph.overview = {
    init : function() {
        var url = $.otp.contextPath;
        RGraph.AJAX(url + '/statistic/projectCountPerDate',
                $.otp.graph.overview.projectCountPerDate);
        RGraph.AJAX(url + '/statistic/laneCountPerDate',
                $.otp.graph.overview.laneCountPerDate);
        RGraph.AJAX(url + '/statistic/sampleCountBySeqType',
                $.otp.graph.overview.sampleCountBySeqType);
        RGraph.AJAX(url + '/statistic/projectCountPerSequenceType',
                $.otp.graph.overview.projectCountPerSequenceType);
    },

    projectCountPerDate : function() {
        var json = eval('(' + this.responseText + ')');
        var data = json.value;
        var scatter1 = new RGraph.Scatter('projectCountPerDate', json.data);
        scatter1.Set('chart.defaultcolor', '#1E5CA4');
        scatter1.Set('chart.labels', json.labels);
        scatter1.Set('chart.ymax', json.count);
        scatter1.Set('chart.gutter.left', 70);
        scatter1.Set('chart.tickmarks', 'circle');
        scatter1.Set('chart.xmin', 1);
        scatter1.Set('chart.ymin', 1);
        scatter1.Set('chart.ticksize', 7);
        scatter1.Set('chart.text.size', 8);
        scatter1.Set('chart.title', 'Number of projects in OTP');
        scatter1.Set('chart.title.color', 'black');
        scatter1.Set('chart.title.size', 11);
        scatter1.Set('chart.xmax', json.daysCount);
        scatter1.Set('chart.background.grid.autofit.numvlines', json.gridCount);
        scatter1.Set('chart.text.angle', 45);
        scatter1.Set('chart.gutter.bottom', 110);
        scatter1.Set('chart.gutter.top', 90);
        scatter1.Draw();
    },

    laneCountPerDate : function() {
        var json = eval('(' + this.responseText + ')');
        var data = json.value;
        var scatter2 = new RGraph.Scatter('laneCountPerDate', json.data);
        scatter2.Set('chart.defaultcolor', '#1E5CA4');
        scatter2.Set('chart.labels', json.labels);
        scatter2.Set('chart.ylabels', true);
        scatter2.Set('chart.tickmarks', 'circle');
        scatter2.Set('chart.xmin', 1);
        scatter2.Set('chart.ymin', 0);
        scatter2.Set('chart.text.size', 8);
        scatter2.Set('chart.ticksize', 6);
        scatter2.Set('chart.title', 'Number of sequence lanes registered in OTP');
        scatter2.Set('chart.title.color', 'black');
        scatter2.Set('chart.title.size', 11);
        scatter2.Set('chart.xmax', json.daysCount);
        scatter2.Set('chart.background.grid.autofit.numvlines', json.gridCount);
        scatter2.Set('chart.text.angle', 45);
        scatter2.Set('chart.gutter.bottom', 110);
        scatter2.Set('chart.gutter.top', 90);
        scatter2.Set('chart.gutter.left', 43);
        scatter2.Set('chart.gutter.right', 70);
        scatter2.Draw();
    },

        sampleCountBySeqType: function() {
        var json = eval('(' + this.responseText + ')');
        var data = json.value;
        var pie = new RGraph.Pie('sampleCountBySeqType', json.data);
        pie.Set('chart.labels', json.labelsProzent);
        pie.Set('chart.title', 'Samples processed by sequencing technologies in OTP');
        pie.Set('chart.title.y', 9);
        pie.Set('chart.title.x', 'center');
        pie.Set('chart.title.size', 11);
        pie.Set('chart.title.color', 'black');
        pie.Set('chart.stroke', 'white');
        pie.Set('chart.colors', [ '#858137', '#2D6122', '#373502', '#ACAB90',
                '#7A5D07', '#5A1B02', '#863312', '#1E5CA4', '#565B7A',
                '#9C702A' ]);
        pie.Set('chart.strokestyle', 'white');
        pie.Set('chart.labels.sticks', true);
        pie.Set('chart.shadow', true);
        pie.Set('chart.shadow.offsetx', 2);
        pie.Set('chart.shadow.offsety', 2);
        pie.Set('chart.shadow.blur', 1);
        pie.Set('chart.exploded', 2);
        pie.Set('chart.text.size', 8);
        pie.Set('chart.labels.sticks.length', 15);
        pie.Set('chart.variant', '3d');
        pie.Set('chart.gutter.bottom', 90);
        pie.Set('chart.gutter.top', 90);
        pie.Set('chart.gutter.left', 100);
        pie.Set('chart.gutter.right', 25);
        pie.Draw();
        RGraph.Effects.Pie.RoundRobin(pie, {
            frames : 335
        });
    },
        projectCountPerSequenceType : function() {
        var json = eval('(' + this.responseText + ')');
        var data = json.value;
        var bar1 = new RGraph.Bar('projectCountPerSequenceTypeBar', json.data);
        bar1.Set('chart.background.grid', false);
        bar1.Set('chart.labels', json.labels);
        bar1.Set('chart.title',
                'Number of projects using sequencing technologies in OTP');
        bar1.Set('chart.title.y', 15);
        bar1.Set('chart.title.x', 'center');
        bar1.Set('chart.title.size', 11);
        bar1.Set('chart.title.color', 'black');
        bar1.Set('chart.colors', [ '#1E5CA4' ]);
        bar1.Set('chart.shadow', true);
        bar1.Set('chart.shadow.blur', 10);
        bar1.Set('chart.shadow.color', '#666');
        bar1.Set('chart.gutter.left', 40);
        bar1.Set('chart.gutter.right', 70);
        bar1.Set('chart.hmargin.grouped', 1);
        bar1.Set('chart.labels.above', true);
        bar1.Set('chart.xlabels.offset', 9);
        bar1.Set('chart.variant', '3d');
        bar1.Set('chart.text.size', 8);
        bar1.Set('chart.hmargin', 9);
        bar1.Set('chart.ticksize', 7);
        bar1.Set('chart.text.angle', 45);
        bar1.Set('chart.labels.above.angle', null);
        bar1.Set('chart.labels.above.size', 9);
        bar1.Set('chart.background.grid.border', false);
        bar1.Set('chart.gutter.bottom', 220);
        bar1.Set('chart.gutter.top', 30);
        bar1.Draw();
    }
}

$.otp.graph.project = {
    init : function() {
        var url = $.otp.contextPath;
        var project = $('#project_select').val();
        RGraph.AJAX(url + '/statistic/sampleTypeCountBySeqType?projectName='
                + project, $.otp.graph.project.sampleTypeCountBySeqType);
        RGraph.AJAX(url + '/statistic/sampleTypeCountByPatient?projectName='
                + project, $.otp.graph.project.sampleTypeCountByPatient);
        RGraph.AJAX(url + '/statistic/laneCountPerDateByProject?projectName='
                + project, $.otp.graph.project.laneCountPerDateByProject);
    },

    sampleTypeCountBySeqType : function() {
        var json = eval('(' + this.responseText + ')');
        RGraph.Reset(document.getElementById('sampleTypeCountBySeqType'));
        var pie1 = new RGraph.Pie('sampleTypeCountBySeqType', json.data);
        var data = json.value;
        pie1.Set('chart.labels', json.labelsProzent);
        pie1.Set('chart.title', 'Samples processed by different sequencing technologies');
        pie1.Set('chart.title.y', 18);
        pie1.Set('chart.title.x', 'center');
        pie1.Set('chart.title.size', 11);
        pie1.Set('chart.title.color', 'black');
        pie1.Set('chart.colors', [ '#858137', '#2D6122', '#373502', '#ACAB90',
                '#7A5D07', '#5A1B02', '#863312', '#1E5CA4', '#565B7A',
                '#9C702A' ]);
        pie1.Set('chart.labels.sticks', true);
        pie1.Set('chart.shadow', true);
        pie1.Set('chart.shadow.offsetx', 2);
        pie1.Set('chart.shadow.offsety', 2);
        pie1.Set('chart.shadow.blur', 1);
        pie1.Set('chart.exploded', 1);
        pie1.Set('chart.text.size', 8);
        pie1.Set('chart.labels.sticks.length', 0);
        pie1.Set('chart.gutter.bottom', 130);
        pie1.Set('chart.gutter.top', 100);
        pie1.Set('chart.gutter.left', 235);
        pie1.Set('chart.gutter.right', 200);
        pie1.Draw();
        RGraph.Effects.Pie.RoundRobin(pie1, {
            frames : 335
        });
    },

    laneCountPerDateByProject : function() {
        var json = eval('(' + this.responseText + ')');
        RGraph.Reset(document.getElementById('laneCountPerDateByProject'));
        var scatter3 = new RGraph.Scatter('laneCountPerDateByProject', json.data);
        var data = json.value;
        scatter3.Set('chart.defaultcolor', '#1E5CA4');
        scatter3.Set('chart.labels', json.labels);
        var count = json.count;
        if (count > 10) {
            count = 10;
        }
        scatter3.Set('chart.ylabels.count', count);
        scatter3.Set('chart.ymax', json.count);
        scatter3.Set('chart.tickmarks', 'circle');
        scatter3.Set('chart.xmin', 0);
        scatter3.Set('chart.ymin', 0);
        scatter3.Set('chart.ticksize', 7);
        scatter3.Set('chart.title',
                'Number of sequence lanes registered for ' + $('#project_select').val());
        scatter3.Set('chart.title.color', 'black');
        scatter3.Set('chart.title.size', 11);
        scatter3.Set('chart.title.y', 18);
        scatter3.Set('chart.text.size', 8);
        scatter3.Set('chart.xmax', json.daysCount);
        scatter3.Set('chart.background.grid.autofit.numvlines', json.gridCount);
        scatter3.Set('chart.text.angle', 65);
        scatter3.Set('chart.gutter.bottom', 170);
        scatter3.Set('chart.gutter.right', 70);
        scatter3.Set('chart.gutter.left', 90);
        scatter3.Set('chart.gutter.top', 50);
        scatter3.Draw();
    },

    sampleTypeCountByPatient : function() {
        var json = eval('(' + this.responseText + ')');
        var canvas = document.getElementById('sampleTypeCountByPatient');
        canvas.height = ((json.count * 15) + 250);
        RGraph.Reset(canvas);
        var hbar = new RGraph.HBar('sampleTypeCountByPatient', json.data);
        var data = json.value;
        hbar.Set('chart.background.grid', false);
        hbar.Set('chart.labels', json.labels);
        hbar.Set('chart.title',
                'Patients and the number of samples (non-redundant)');
        hbar.Set('chart.title.x', 300);
        hbar.Set('chart.title.size', 11);
        hbar.Set('chart.title.color', 'black');
        hbar.Set('chart.colors', [ '#1E5CA4' ]);
        hbar.Set('chart.shadow', true);
        hbar.Set('chart.units.post', ' Sample(s)');
        hbar.Set('chart.shadow.offsetx', 0);
        hbar.Set('chart.shadow.offsety', 0);
        hbar.Set('chart.shadow.color', '#aaa');
        hbar.Set('chart.strokestyle', 'rgba(0,0,0,0)');
        hbar.Set('chart.xmax', 13);
        hbar.Set('chart.variant', '3d');
        hbar.Set('chart.labels.above', true);
        hbar.Set('chart.text.size', 8);
        hbar.Set('chart.margin', 3);
        hbar.Set('chart.noxaxis', true);
        hbar.Set('chart.noxtickmarks', true);
        hbar.Set('chart.xlabels', false);
        hbar.Set('chart.labels.above.size', 9);
        hbar.Set('chart.linewidth', 50);
        hbar.Set('chart.text.angle', 30);
        hbar.Set('chart.labels.above.angle', null);
        hbar.Set('chart.gutter.bottom', 150);
        hbar.Set('chart.gutter.left', 175);
        hbar.Set('chart.gutter.top', 70);
        hbar.Draw();
    }
}

$.otp.graph.projectStatistic = {
    init : function() {
        var url = $.otp.contextPath;
        var projectStatistic  = $('#project_select').val();
        RGraph.AJAX(url
                + '/projectStatistic/laneNumberByProject?projectName='
                + projectStatistic,
                $.otp.graph.projectStatistic.laneNumberByProject);
    },

    laneNumberByProject : function() {
        var jsonList = eval('(' + this.responseText + ')');
        var div = $('#laneNumberByProject');
        div.empty();
        for ( var pos = 0; pos < jsonList.length; pos = pos + 1) {
            var json = jsonList[pos];
            var canvas = document.createElement("canvas");
            canvas.id = 'laneNumberByProject_' + json.seqTypeId;
            canvas.height = ((json.count * 15) + 180);
            canvas.width = 650;
            div.append(canvas);
            RGraph.Reset(canvas);
            var bar3 = new RGraph.HBar('laneNumberByProject_'
                    + json.seqTypeId, json.data);
            var data = json.value;
            bar3.Set('chart.background.grid', false);
            bar3.Set('chart.labels', json.labels);
            bar3.Set('chart.title', json.seqTypeName + ' ' + json.seqTypeLibName + ': Sample & lane');
            bar3.Set('chart.title.x', 300);
            bar3.Set('chart.title.size', 11);
            bar3.Set('chart.title.color', 'black');
            bar3.Set('chart.colors', [ '#1E5CA4' ]);
            bar3.Set('chart.shadow', true);
            bar3.Set('chart.units.post', ' Lane(s)');
            bar3.Set('chart.shadow.offsetx', 0);
            bar3.Set('chart.shadow.offsety', 0);
            bar3.Set('chart.shadow.color', '#aaa');
            bar3.Set('chart.strokestyle', 'rgba(0,0,0,0)');
            bar3.Set('chart.hmargin.grouped', 1);
            bar3.Set('chart.labels.above', true);
            bar3.Set('chart.text.size', 8);
            bar3.Set('chart.xaxispos.text', 'center');
            bar3.Set('chart.noxaxis', true);
            bar3.Set('chart.noxtickmarks', true);
            bar3.Set('chart.xmax', 29);
            bar3.Set('chart.xlabels', false);
            bar3.Set('chart.labels.above.size', 9);
            bar3.Set('chart.gutter.bottom', 90);
            bar3.Set('chart.gutter.top', 50);
            bar3.Set('chart.gutter.left', 290);
            bar3.Draw();
        }
    }
}
