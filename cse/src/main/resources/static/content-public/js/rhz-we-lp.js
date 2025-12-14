let registrationPopupFirstRun = true;
let button;
let registerButton;
let haveanaccountButton;
let idonthaveanaccountButton;

function getMatchGroupValue(name) {
    let val = '';
    let elems = document.getElementsByClassName(name);
    if (elems.length > 0) {
        val = elems[0].value;
    }
    return val;
}

function fieldsMatchCheck(name) {
    let passwordsMatch;
    let prevVal = "";
    let elems = document.getElementsByClassName(name);
    for (let i = 0; i < elems.length; i++) {
        if (passwordsMatch === undefined) {
            prevVal = elems[i].value;
            passwordsMatch = true;
        } else {
            if (passwordsMatch === undefined) {
                passwordsMatch = false;
            }
            if (passwordsMatch === true) {
                if (elems[i].value !== prevVal) {
                    passwordsMatch = false;
                }
            }
        }
    }
    if (passwordsMatch === undefined) passwordsMatch = false;
    return passwordsMatch;
}

function loginPanelLogout() {
    $.ajax({
        url : './account/logout',
        type: 'GET',
        contentType: "application/json",
        data : ({}),
        success: function (data) {
            location.reload(true);
        }
    });
}

function loginPanelInit () {
    //console.log("ready");

    haveanaccountButton = document.getElementById("loginpanel-button-ihaveanaccount");
    haveanaccountButton.addEventListener("click", function () {
        document.getElementById("loginpanel-guest-signin").style.display = "block";
        document.getElementById("loginpanel-guest-default").style.display = "none";
    });

    idonthaveanaccountButton = document.getElementById("loginpanel-button-idonthaveanaccount");
    idonthaveanaccountButton.addEventListener("click", function () {
        document.getElementById("loginpanel-guest-signin").style.display = "none";
        document.getElementById("loginpanel-guest-default").style.display = "block";
    });

    let reqCount = 0;

    registerButton = document.getElementById("loginpanel-button-register");
    registerButton.addEventListener("click", function () {
        if (registrationPopupFirstRun) {

            let loginBlockWall = document.createElement('div');
            loginBlockWall.id = "registration-popup";
            loginBlockWall.classList.add("editor");
            document.getElementById("page-body").appendChild(loginBlockWall);
            let registrationDiv = document.getElementById(
                "registration-window").cloneNode(true);
            document.getElementById("popup-registration-container").removeChild(document.getElementById("registration-window"));
            registrationDiv.style.display = "block";
            loginBlockWall.appendChild(registrationDiv);

            let regexLogin = /^[a-z0-9_-]{4,25}$/;
            let regexPassword = /(?=.*\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9]{8,}/;
            let regexEmail = /^(([^<>()\[\]\.,;:\s@\"]+(\.[^<>()\[\]\.,;:\s@\"]+)*)|(\".+\"))@(([^<>()[\]\.,;:\s@\"]+\.)+[^<>()[\]\.,;:\s@\"]{2,})$/i;
            let regexShownName = /^[a-zA-Z ]{4,25}$/;
            let regexAddress = /^([a-zA-Z0-9., -]){15,200}$/;
            let regexPhone = /((8|\+7)[\- ]?)?(\(?\d{3}\)?[\- ]?)?[\d\- ]{7,10}/;
            let regWindowInputStates = {};

            function getRegWindowTypeFromId(input) {
                return input.id.replace("registration-window-","").replace("-input","");
            }

            let registrationInputs = document.getElementsByName("registration-window-input");
            registrationInputs.forEach(function (reginp) {
                if (reginp.required) {
                    reqCount++;
                }

                reginp.addEventListener("keyup", function () {
                    let divname = getRegWindowTypeFromId(reginp);
                    let matchesRegex = false;
                    if (divname === "login") {
                        if (reginp.value.match(regexLogin)) matchesRegex = true;
                    } else if (divname === "email") {
                        if (reginp.value.match(regexEmail)) matchesRegex = true;
                    } else if (divname === "shown-name") {
                        if (reginp.value.match(regexShownName)) matchesRegex = true;
                    } else if (divname === "psw") {
                        if (reginp.value.match(regexPassword)) matchesRegex = true;
                    } else if (divname === "physical-address") {
                        if (reginp.value.match(regexAddress)) matchesRegex = true;
                    } else if (divname === "phone-number") {
                        if (reginp.value.match(regexPhone)) matchesRegex = true;
                    }
                    if (matchesRegex) {
                        reginp.classList.remove("transition");
                        reginp.classList.add("input-correct");
                        regWindowInputStates[reginp.id] = "yes";
                    } else {
                        reginp.classList.remove("input-correct");
                        reginp.classList.add("transition");
                        regWindowInputStates[reginp.id] = "no";
                    }
                    let matches = 0;
                    for (let inpState in regWindowInputStates) {
                        if (document.getElementById(inpState).required) {
                            if (regWindowInputStates[inpState] === "yes") matches++;
                        }
                    }

                    if (fieldsMatchCheck("registration-window-match")) {
                        matches++;
                    } else {
                        matches--;
                    }

                    if (fieldsMatchCheck("registration-window-match") && matches === reqCount) {
                        document.getElementById("registration-window-button-commit").style.display ="block";
                    } else {
                        document.getElementById("registration-window-button-commit").style.display ="none";
                    }
                })
            });

            // let buttonCommitRegistration = document.getElementById("registration-window-button-commit");
            // buttonCommitRegistration.addEventListener("click", function () {
            //     buttonCommitRegistration.style.display = "none";
            //     let data = {};
            //     registrationInputs.forEach(function (reginp) {
            //         saltSrc += reginp.id;
            //         if (!reginp.classList.contains("registration-window-match")) {
            //             data[reginp.id] = reginp.value;
            //         } else {
            //             data["pswhash"] = sha512(getMatchGroupValue("registration-window-match"));
            //             data["pswsalt"] = sha512(saltSrc);
            //         }
            //     });
            //     $.ajax({
            //         url : './account/register',
            //         type: 'POST',
            //         contentType: "application/json",
            //         data : JSON.stringify(data),
            //         success: function (data) {
            //             loginBlockWall.style.display = "none";
            //         },
            //         error: function() {
            //             button.style.display = "block";
            //             buttonCommitRegistration.style.display = "block";
            //             document.getElementById("loginpanel-notification-failure").style.display = "block";
            //         }
            //     });
            // });

            let buttonCancelRegistration = document.getElementById("registration-window-button-cancel");
            buttonCancelRegistration.addEventListener("click", function () {
                loginBlockWall.style.display = "none";
            });
            registrationPopupFirstRun = false;
        } else {
            document.getElementById("registration-popup").style.display = "block";
        }
    });


    let loginPanelInputs = document.getElementsByName("loginpanel-input");
    button = document.getElementById("loginpanel-button-signin");
    button.addEventListener("click",function () {
        let data = {};
        button.style.display = "none";
        loginPanelInputs.forEach(function (i) {
            if (i.id === "loginpanel-password") {
                data[i.id] = sha512(i.value);
            } else {
                data[i.id] = i.value;
            }

        });

        $.ajax({
            url : './account/auth',
            type: 'POST',
            contentType: "application/json",
            data : JSON.stringify(data),
            success: function (data) {
                switch (data.status) {
                    case 200:
                        location.reload(true);
                        break;
                    case 400:
                        document.getElementById("loginpanel-notification-failure").style.display = "block";
                        document.getElementById("loginpanel-notification-description").innerHTML = data.message;
                        break;
                    case 500:
                        document.getElementById("loginpanel-notification-failure").style.display = "block";
                        document.getElementById("loginpanel-notification-description").innerHTML = data.message;
                        break;

                }

            },
            error: function() {
                button.style.display = "block";
                document.getElementById("loginpanel-notification-failure").style.display = "block";
            }
        });
    });



    let inputStates = {};
    loginPanelInputs.forEach(function (input) {
        input.addEventListener("keyup", function () {
            let val = input.value.trim();
            if (val.length > 0) {
                inputStates[input.id] = "yes";
            } else {
                inputStates[input.id] = "no";
            }
            if (loginPanelInputs.length === Object.size(inputStates)) {
                let found = 0;
                for (let istate in inputStates) {
                    if (inputStates[istate] === "yes") {
                        found++;
                    }
                }
                if (found === loginPanelInputs.length) {
                    //console.log("show button");
                    document.getElementById("loginpanel-button-signin").style.display = "inline-block";
                } else {
                    //console.log("hide button");
                    document.getElementById("loginpanel-button-signin").style.display = "none";
                }
            }
        })
    });

    Object.size = function(obj) {
        let size = 0, key;
        for (key in obj) {
            if (obj.hasOwnProperty(key)) size++;
        }
        return size;
    };
}