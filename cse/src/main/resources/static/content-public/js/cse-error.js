function getErrorDescription() {

    const code = "error";
    let url = new URL(window.location.href);
    let codeValue = url.searchParams.get(code);
    let data = {};
    data[code] = codeValue;
    $.ajax({
        url: "./rest/error",
        type: 'GET',
        dataType: 'json',
        data: data,
        success: function (data) {
            for (var key in data){
                let name = "errorPage-"+key+"";
                let elem = document.getElementById(name);
                if (elem) {
                    elem.innerHTML = data[key];
                }
            }
        }
    });
}