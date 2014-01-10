var aGauge = null;
var sGauge = null;
var wGauge = null;

function initGauges() {
    aGauge = new AquaGauge('gauge');
    aGauge.props.minValue = 0;
    aGauge.props.maxValue = 100;
    aGauge.props.noOfDivisions = 5;
    aGauge.props.noOfSubDivisions = 4;
    aGauge.props.showMinorScaleValue = true;
    aGauge.refresh(0);
    initPage();
    setupSlider();
    setupRangeSliders();

    sGauge = new AquaGauge('gaugeSmall');
    setupSmallGauge(sGauge);

    wGauge = new AquaGauge('gaugeWhite');
    setupWhiteGauge(wGauge);
}

function updateGauge(val) {
    aGauge.refresh(val);
}