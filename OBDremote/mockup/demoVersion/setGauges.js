function setupSlider(minv, maxv, currv) {
    $("#slider").slider({
        range: false,
        min: aGauge.props.minValue,
        max: aGauge.props.maxValue,
        values: [aGauge.props.minValue],
        slide: function (event, ui) { updateGauge(parseFloat(ui.values[0])) }
    });
}

function setupRangeSliders() {
    $("#sliderG").slider({
        range: true,
        min: aGauge.props.minValue,
        max: aGauge.props.maxValue,
        values: [aGauge.props.rangeSegments[0].start, aGauge.props.rangeSegments[0].end],
        slide: function (event, ui) {
            aGauge.props.rangeSegments[0].start = parseFloat(ui.values[0]);
            aGauge.props.rangeSegments[0].end = parseFloat(ui.values[1]);
            updateGauge(aGauge.props.currentValue);
        }
    });

    $("#sliderY").slider({
        range: true,
        min: aGauge.props.minValue,
        max: aGauge.props.maxValue,
        values: [aGauge.props.rangeSegments[1].start, aGauge.props.rangeSegments[1].end],
        slide: function (event, ui) {
            aGauge.props.rangeSegments[1].start = parseFloat(ui.values[0]);
            aGauge.props.rangeSegments[1].end = parseFloat(ui.values[1]);
            updateGauge(aGauge.props.currentValue);
        }
    });

    $("#sliderR").slider({
        range: true,
        min: aGauge.props.minValue,
        max: aGauge.props.maxValue,
        values: [aGauge.props.rangeSegments[2].start, aGauge.props.rangeSegments[2].end],
        slide: function (event, ui) {
            aGauge.props.rangeSegments[2].start = parseFloat(ui.values[0]);
            aGauge.props.rangeSegments[2].end = parseFloat(ui.values[1]);
            updateGauge(aGauge.props.currentValue);
        }
    });
}

function initPage() {
    $("#title").val(aGauge.props.dialTitle);
    $("#subTitle").val(aGauge.props.dialSubTitle);
    $("#dialColor").val(aGauge.props.dialColor);
    $("#dialGradient").val(aGauge.props.dialGradientColor);
    $("#titleFont").val(aGauge.props.dialTitleTextFont);
    $("#subTitleFont").val(aGauge.props.dialSubTitleTextFont);
    $("#titleColor").val(aGauge.props.dialTitleTextColor);
    $("#subTitleColor").val(aGauge.props.dialSubTitleTextColor);
    $("#backgroundColor").val(aGauge.props.backgroundColor);
    $("#valueColor").val(aGauge.props.dialValueTextColor);
    $("#valueFont").val(aGauge.props.dialValueTextFont);
    $("#glossiness").val(aGauge.props.showGlossiness);

    $("#minValue").val(aGauge.props.minValue);
    $("#maxValue").val(aGauge.props.maxValue);
    $("#noOfDivs").val(aGauge.props.noOfDivisions);
    $("#noOfSubDivs").val(aGauge.props.noOfSubDivisions);
    $("#rimColor").val(aGauge.props.rimColor);
    $("#rimWidth").val(aGauge.props.rimWidth);
    $("#majorGraduationColor").val(aGauge.props.majorDivisionColor);
    $("#minorGraduationColor").val(aGauge.props.minorDivisionColor);
    $("#majorScaleFont").val(aGauge.props.dialScaleFont);
    $("#minorScaleFont").val(aGauge.props.dialSubScaleFont);
    $("#majorTextColor").val(aGauge.props.dialScaleTextColor);
    $("#minorTextColor").val(aGauge.props.dialSubScaleTextColor);
    $("#showMinorValue").val(aGauge.props.showMinorScaleValue);

    $("#gStart").val(aGauge.props.rangeSegments[0].start);
    $("#gEnd").val(aGauge.props.rangeSegments[0].end);
    $("#gColor").val(aGauge.props.rangeSegments[0].color);
    $("#yStart").val(aGauge.props.rangeSegments[1].start);
    $("#yEnd").val(aGauge.props.rangeSegments[1].end);
    $("#yColor").val(aGauge.props.rangeSegments[1].color);
    $("#rStart").val(aGauge.props.rangeSegments[2].start);
    $("#rEnd").val(aGauge.props.rangeSegments[2].end);
    $("#rColor").val(aGauge.props.rangeSegments[2].color);

    $("#pointerColor").val(aGauge.props.pointerColor);
    $("#pointerGradientColor").val(aGauge.props.pointerGradientColor);
    $("#pointerShadowColor").val(aGauge.props.shadowColor);    
}

function refreshView() {
    aGauge.props.dialTitle = $("#title").val();
    aGauge.props.dialSubTitle = $("#subTitle").val();
    aGauge.props.dialColor = $("#dialColor").val();
    aGauge.props.dialGradientColor = $("#dialGradient").val();
    aGauge.props.dialTitleTextFont = $("#titleFont").val();
    aGauge.props.dialSubTitleTextFont = $("#subTitleFont").val();
    aGauge.props.dialTitleTextColor = $("#titleColor").val();
    aGauge.props.dialSubTitleTextColor = $("#subTitleColor").val();
    aGauge.props.backgroundColor = $("#backgroundColor").val();
    aGauge.props.dialValueTextColor = $("#valueColor").val();
    aGauge.props.dialValueTextFont = $("#valueFont").val();
    aGauge.props.showGlossiness = $("#glossiness").attr('checked') == 'checked';

    aGauge.props.minValue = parseInt($("#minValue").val());
    aGauge.props.maxValue = parseInt($("#maxValue").val());
    aGauge.props.noOfDivisions = parseInt($("#noOfDivs").val());
    aGauge.props.noOfSubDivisions = parseInt($("#noOfSubDivs").val());
    aGauge.props.rimColor = $("#rimColor").val();
    aGauge.props.rimWidth = $("#rimWidth").val();
    aGauge.props.majorDivisionColor = $("#majorGraduationColor").val();
    aGauge.props.minorDivisionColor = $("#minorGraduationColor").val();
    aGauge.props.dialScaleFont = $("#majorScaleFont").val();
    aGauge.props.dialSubScaleFont = $("#minorScaleFont").val();
    aGauge.props.dialScaleTextColor = $("#majorTextColor").val();
    aGauge.props.dialSubScaleTextColor = $("#minorTextColor").val();
    aGauge.props.showMinorScaleValue = $("#showMinorValue").attr('checked') == 'checked';

    aGauge.props.rangeSegments[0].color = $("#gColor").val();
    aGauge.props.rangeSegments[1].color = $("#yColor").val();
    aGauge.props.rangeSegments[2].color = $("#rColor").val();

    aGauge.props.pointerColor = $("#pointerColor").val();
    aGauge.props.pointerGradientColor = $("#pointerGradientColor").val();
    aGauge.props.shadowColor = $("#pointerShadowColor").val();

    setupSlider(aGauge.props.minValue, aGauge.props.maxValue, aGauge.props.currentValue);
    aGauge.refresh(aGauge.props.currentValue);
    initPage();
}

function resetRange() {
    var diff = aGauge.props.maxValue - aGauge.props.minValue;
    var seg = diff / 3;
    var st = aGauge.props.minValue;
    for (var i = 0; i < 3; i++) {
        aGauge.props.rangeSegments[i].start = st;
        st += seg;
        aGauge.props.rangeSegments[i].end = st;
    }
    initPage();
    aGauge.refresh(aGauge.props.currentValue);
}

function setupCoolTempGauge(aGauge) {
    aGauge.props.dialColor = "rgba(120, 180, 110, 0.8)";
    aGauge.props.dialGradientColor = "#fff";
    aGauge.props.dialScaleFont = "bold 10px Trebuchet MS";
    aGauge.props.dialTitle = "Engine Temp";
    aGauge.props.dialTitleTextFont = "bold 12px Calibri";
    aGauge.props.dialValueTextFont = "bold 14px Arial Black";
    aGauge.props.dialSubTitle = "C";
    aGauge.props.rimWidth = "3";
    aGauge.props.dialSubTitleTextFont = "10px Tahoma";
    aGauge.props.rangeSegments = [{ start: 0, end: 60, color: "greenyellow" },
                                      { start: 60, end: 120, color: "yellow" },
                                      { start: 120, end: 180, color: "red"}];
    aGauge.props.pointerColor = "red";
    aGauge.props.pointerGradientColor = "maroon";
    aGauge.props.maxValue = 180;
    aGauge.props.noOfDivisions = 6;
    aGauge.refresh(90);
}

function setupRpmGauge(wGauge) {
    wGauge.props.maxValue = 6.0;
    wGauge.props.rangeSegments = [];
    wGauge.props.dialColor = "#fff";
    wGauge.props.dialGradientColor = "#fff";
    wGauge.props.noOfDivisions = 4;
    wGauge.props.noOfSubDivisions = 8;
    wGauge.props.majorDivisionColor = "black";
    wGauge.props.minorDivisionColor = "red";
    wGauge.props.dialTitle = "RPM";
    wGauge.props.dialSubTitle = "x 1000";
    wGauge.props.dialTitleTextFont = "bold 15px Calibri";
    wGauge.props.dialTitleTextColor = "maroon";
    wGauge.props.dialValueTextFont = "bold 14px Arial Black";
    wGauge.props.dialSubTitleTextFont = "bold 13px Calibri";
    wGauge.props.showGlossiness = false;
    wGauge.refresh(1.5);
}

function setupIntakeTempGauge(aGauge) {
    // aGauge.props.dialColor = "rgba(120, 180, 110, 0.8)";
    // aGauge.props.dialGradientColor = "#fff";
    aGauge.props.dialScaleFont = "bold 10px Trebuchet MS";
    aGauge.props.dialTitle = "Intake Air Temp";
    aGauge.props.dialTitleTextFont = "bold 10px Calibri";
    aGauge.props.dialValueTextFont = "bold 14px Arial Black";
    aGauge.props.dialSubTitle = "C";
    aGauge.props.rimWidth = "3";
    aGauge.props.dialSubTitleTextFont = "10px Tahoma";
    aGauge.props.rangeSegments = [{ start: 0, end: 20, color: "greenyellow" },
                                      { start: 20, end: 40, color: "yellow" },
                                      { start: 40, end: 60, color: "red"}];
    aGauge.props.pointerColor = "red";
    aGauge.props.pointerGradientColor = "maroon";
    aGauge.props.maxValue = 60;
    aGauge.props.noOfDivisions = 6;
    aGauge.refresh(22);
}
