const debugIsEnabled = false;
const restPath = "rest/setup";
var latestRequestDate = undefined;

function co(data) {
    console.log(data);
}

function cod (dataToDisplay) {
    if (debugIsEnabled) {
        console.log(dataToDisplay);
    }
}

function updatePageContents(data) {
    updatePercents(data.progress);
    data.onloadHtmlUpdates.forEach(function (elementUpdate) {
        let elementToUpdate = document.getElementById(elementUpdate.elementId);
        elementUpdate.updates.forEach(function (update) {
                elementToUpdate[update.field] = update.value;
            }
        )
    });
}

function updatePercents(percent) {
    let progressBar = document.getElementById("setupDialogue-stageProgress");
    progressBar.style.width = percent + "%";
}

function collectAndGo(buttonBoolean) {
    let data = {};
    latestRequestDate=getDate();
    data['date'] = latestRequestDate;
    data['button'] = ""+buttonBoolean;
    let collectibles = getCollectibles();
    for (let i = 0; i < collectibles.length; i++) {
        data[collectibles[i].id] = collectibles[i].value;
    }
    $.ajax({
        url: restPath,
        type: 'POST',
        dataType: 'json',
        contentType: 'application/json',
        mimeType: 'text/html',
        data: JSON.stringify(data),
        success: function (data) {
            updatePageContents(data);
            for (let i = 0; i < collectibles.length; i++) {
                document.getElementById(collectibles[i].id).addEventListener(
                    "keyup",
                    function () {
                        let nextButton = document.getElementById(
                            "setupDialogue-controlButton");
                        nextButton.style.display = areRequiredCollectiblesFullfilled()
                            ? "block" : "none";
                    })
            }
        }
    });
}

function areRequiredCollectiblesFullfilled() {
    let collectibles = getCollectibles();
    let reqCount = 0;
    let filledCount = 0;
    for (let i = 0; i < collectibles.length; i++) {
        if (collectibles[i].required) { reqCount++; }
        if (collectibles[i].required && collectibles[i].value.trim().length > 0) { filledCount++; }
    }
    return reqCount === filledCount;
}

function getCollectibles() {
    return document.getElementsByClassName("collectible");
}

function getDate() {
    return ""+new Date()+"";
}
