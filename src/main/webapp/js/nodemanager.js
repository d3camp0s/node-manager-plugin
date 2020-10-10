
//
function deleteGroup(checkUrl,params,target,callback) {

    if (params.method == "post") {
        var idx = checkUrl.indexOf('?');
        params.parameters = checkUrl.substring(idx + 1);
        checkUrl = checkUrl.substring(0, idx);
    }
    else{
        window.alert("failed to invoke "+checkUrl);
    }

    new Ajax.Request(checkUrl, {
        parameters: params.parameters,
        onComplete: function(rsp) {
            applyErrorMessage(target, rsp);
            layoutUpdateCallback.call();

            try {
                if( rsp.status==200 )
                callback();
            } catch(e) {
                window.alert("failed to evaluate callback: "+e.message);
            }
        }
    });

}