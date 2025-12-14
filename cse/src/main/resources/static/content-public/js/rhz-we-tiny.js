var buttonInitialized = {};

function validateData(required, formId) {
    let button = document.getElementsByClassName(formId + "-button")[0];
    let valid = [];
    let collectibles = document.getElementsByClassName(formId + "-collectible");
    for (let i = 0; i < collectibles.length; i++) {
        let collectible = collectibles[i];
        if (collectible.value.length > 0) {
            if (!collectible.classList.contains("valid")) {
                collectible.classList.add("valid");
            }
            if (collectible.required) {
                valid.push(collectible.id);
            }
        } else {
            if (collectible.classList.contains("invalid")) {
                collectible.classList.remove("invalid");
            }
        }
    }
    if (valid.length === required.length) {
        if (buttonInitialized[formId] === undefined) {
            buttonInitialized[formId] = true;
            button.addEventListener("click", function() {

                let rest = document.getElementById(formId + "-rest").value;
                button.style.display = "none";
                let data2 = {};
                let collectibles = document.getElementsByClassName("valid");
                for (let i = 0; i < collectibles.length; i++) {
                    if (collectibles[i].type === "password") {
                        data2[collectibles[i].id] = sha512(collectibles[i].value);
                    } else {
                        data2[collectibles[i].id] = collectibles[i].value;
                    }
                }
                $.ajax({
                    url: rest,
                    type: 'POST',
                    dataType: 'json',
                    contentType: 'application/json',
                    mimeType: 'text/html',
                    data: JSON.stringify(data2),
                    success: function (data) {
                        if (data.hasOwnProperty("status")) {
                            if (data.status === 200) {
                                location.reload(true);
                            } else {
                                button.style.display = "block"
                            }
                        }
                    },
                });
            });
        }
        button.style.display = "block";
    } else {
        button.style.display = "none";
    }
}

function registerForm(parent, formId) {
    let data = {};
    data['form'] = formId;
    $.ajax({
        url: "./rest/forms/render",
        type: 'POST',
        dataType: 'json',
        contentType: 'application/json',
        mimeType: 'text/html',
        data: JSON.stringify(data),
        success: function (data) {
            if (data.hasOwnProperty("restObject")) {
                document.getElementById(parent).innerHTML = data.restObject;


                let collectibles = document.getElementsByClassName(formId + "-collectible");
                let required = [];
                for (let i = 0; i < collectibles.length; i++) {
                    if (collectibles[i].required === true) {
                        required.push(collectibles[i].id);
                    }
                }

                setInterval(function(){
                    validateData(required, formId);
                }, 1000);
            }
        }
    });
}



