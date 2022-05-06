oncontextmenu = (e) => {
    e.preventDefault();
    const href_location = e.explicitOriginalTarget.currentSrc || e.explicitOriginalTarget.ownerDocument.activeElement.attributes.href.nodeValue;
    let targetVideo = "";
    if (href_location) {
        if (href_location.startsWith("/watch")) {
            const index = href_location.indexOf("v=");
            targetVideo = href_location.slice(index+2);
        } else if (href_location.startsWith("https://i.ytimg.com/")){
            const substring = href_location.slice("https://i.ytimg.com/".length);
            const startindex = substring.indexOf("/");
            const shortened = substring.slice(startindex+1);
            const endindex = shortened.indexOf("/");
            targetVideo = shortened.slice(0, endindex);
        }
    }

    if (targetVideo) {
        const url = 'http://localhost:8080/play?video='+targetVideo;
        let url_window = window.open(url);
        setTimeout(()=> {
            url_window.close();
        }, 100);
    }
}