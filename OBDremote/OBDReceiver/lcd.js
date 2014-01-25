function resetDisplay(e) {
    var display = document.getElementById(e);
    var notches = display.getElementsByClassName("notchBG");
    for (var i=0; i < notches.length; i++) {
        notches[i].style.background="#F0F0F0";
    }
}

function displayDigit(n, digit) {
    if (digit==null || digit==undefined) {
        return;
    }
    switch(n) {
        case 0:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[1].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[4].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        case 1:
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            break;
        case 2:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[4].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        case 3:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        case 4:
            digit.getElementsByClassName("notchBG")[1].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            break;
        case 5:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[1].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        case 6:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[1].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[4].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        case 7:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            break;
        case 8:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[1].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[4].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        case 9:
            digit.getElementsByClassName("notchBG")[0].style.background="#000000";
            digit.getElementsByClassName("notchBG")[1].style.background="#000000";
            digit.getElementsByClassName("notchBG")[2].style.background="#000000";
            digit.getElementsByClassName("notchBG")[3].style.background="#000000";
            digit.getElementsByClassName("notchBG")[5].style.background="#000000";
            digit.getElementsByClassName("notchBG")[6].style.background="#000000";
            break;
        default:
    }
}

function displayDigito(p) {
    var n = parseInt(document.forms["aForm"]["numero"].value);
    if (n==null || isNaN(n)) {
        alert("Escolhe um numero");
        return;
    }
    displayDigit(n,p);
}

function displayNumber() {
    var n = parseInt(document.forms["aForm"]["numero"].value);
    if (n==null || isNaN(n)) {
        alert("Not a number");
        return;
    }

    if (n < 0 || n > 999) {
        alert("Number out of range (0-999): " + n);
        return;
    }

    var centena = parseInt(n / 100);
    n = n - centena * 100;
    var dezena = parseInt(n / 10);
    var unidade = n - dezena * 10;

    if (centena > 0) displayDigit(centena, document.getElementById('centena'));
    if (centena > 0 || dezena > 0) displayDigit(dezena, document.getElementById('dezena'));
    displayDigit(unidade, document.getElementById('unidade'));

}
