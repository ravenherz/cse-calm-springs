function getErrorDescription() {

    const code = "error";
    let url = new URL(window.location.href);
    let codeValue = url.searchParams.get(code);
    console.log(codeValue);
    let data = {};
    data[code] = codeValue;
    $.ajax({
        url: "./rest/error",
        type: 'POST',
        dataType: 'json',
        contentType: 'application/json',
        mimeType: 'text/html',
        data: JSON.stringify(data),
        success: function (data) {
            for (var key in data){
                let name = "errorPage-"+key+"";
                let elem = document.getElementById(name);
                let atvl = data[key];
                console.log(elem);
                console.log(atvl);
                elem.innerHTML = atvl;
            }
        }
    });
}