function playPause() {
    fetch('/api/play');
    setTimeout(getPlaylist, 400);
}

function stop() {
    fetch('/api/stop');
    setTimeout(getPlaylist, 400);
}

function fullscreen() {
    fetch('/api/fullscreen');
}

function volumeup() {
    fetch('/api/volumeup');
}

function volumedown() {
    fetch('api/volumedown');
}

function next() {
    fetch('api/next');
    setTimeout(getPlaylist, 400);
}

function previous() {
    fetch('api/previous');
    setTimeout(getPlaylist, 400);
}

function emptyPlaylist() {
    fetch('api/emptyplaylist');
    setTimeout(getPlaylist, 400);
}

function addToQueueAndPlay(doPlay) {
    const queueInputElement = document.getElementById("queue_input_id");
    const video = queueInputElement.value || null;
    if (video == null) return;
    addToQueueAndPlayUrl(video, doPlay);
    queueInputElement.value = "";
}

function addToQueueAndPlayUrl(url, doPlay) {
    if (doPlay) {
        fetch('api/queue?video=' + url + "&play=true");
    } else {
        fetch('api/queue?video=' + url);
    }
}

function playItem(itemId) {
    fetch('api/select/'+itemId);
    setTimeout(getPlaylist, 400);
}

function setVolume() {
    const value = document.getElementById("volume_slider_id").value;
    fetch('api/setvolume?value=' + value).then(() => {
        document.getElementById("volume_value_id").textContent = value + "%";
        }
    )
}

function updateValue() {
    document.getElementById("volume_value_id").textContent = document.getElementById("volume_slider_id").value;
}

function setTime() {
    const value = document.getElementById("playback_slider_id").value;
    fetch('api/settime?value=' + value).then(() => {
        updatePageState();
    });
}


function getPlaylist() {
    fetch('api/getplaylist').then(async (response) => {
        const data = await response.json();
        const table_body = document.getElementById("playlist_table_body_id");
        table_body.innerHTML = "";
        if (data.items.length === 0) {
            const row = document.createElement("tr");
            let i = 0;
            while (i < 3) {
                const col = document.createElement("td");
                col.textContent = "-";
                row.appendChild(col);
                i++;
            }
            table_body.appendChild(row);
        } else {
            for (let entry of data.items) {
                const row = document.createElement("tr");
                const playActiveCol = document.createElement("td");
                if (entry.id === "plid_"+data.activeIndex) {
                    playActiveCol.textContent = "→";
                }
                const titleCol = document.createElement("td");
                if (entry.id.startsWith("plid_")) {
                    const titleColLink = document.createElement("a");
                    titleColLink.textContent = entry.title;
                    titleColLink.href = "#";
                    titleColLink.onclick = () => {playItem(entry.id)};
                    titleCol.appendChild(titleColLink);
                } else {
                    titleCol.textContent = entry.title;
                }

                const durationCol = document.createElement("td");
                durationCol.textContent = lengthString(entry.duration);
                row.appendChild(playActiveCol);
                row.appendChild(titleCol);
                row.appendChild(durationCol);
                table_body.appendChild(row);
            }
        }
    });
}

function lengthString(duration) {
    if (duration >= 3600) {
        const hours = Math.floor(duration / 3600);
        const minutes = (""+Math.floor((duration % 3600) / 60)).padStart(2, '0');
        const seconds = (""+(duration % 60)).padStart(2, '0');
        return "" + hours + ":" + minutes + ":" + seconds;
    } else if (duration >= 60) {
        const minutes = (""+(Math.floor(duration / 60))).padStart(2, '0');
        const seconds = (""+(duration % 60)).padStart(2, '0');
        return "" + minutes + ":" + seconds;
    } else {
        return "00:" + (""+duration).padStart(2, '0');
    }
}

function updateCounter() {
    counter++;
    document.getElementById("display_label_id").textContent = "(" + counter + ")";
}

function isValidUrl(string) {
    return (string.startsWith("http") && string.includes("://")) || (string.includes("youtube.com"));
}
async function clipboardCopy() {
    const auto_copy = document.getElementById("auto_copy_checkbox_id").checked;
    if (!auto_copy) return;
    if (navigator.clipboard.readText === undefined) return;
    try {
        const text = await navigator.clipboard.readText();
        if (!isValidUrl(text)) return;
        addToQueueAndPlayUrl(text, true);
        await navigator.clipboard.writeText("");
        updateCounter();
    } catch (exception) {}
}

function updatePageState() {
    fetch("api/vlcstatus").then(
        async (response) => {
            const data = await response.json();
            const volume = Math.floor((data.volume / 512.0) * 200.0);
            const playPosition = Math.floor(data.length * data.position);
            document.getElementById("volume_slider_id").value = volume;
            document.getElementById("volume_value_id").textContent = (volume + "%");
            document.getElementById("playback_slider_id").value = data.position * 100;
            document.getElementById("playback_slider_text_id").textContent = lengthString(playPosition);
        }
    );
    getPlaylist();
    clipboardCopy();
}

let counter = 0;

function executeOnPageLoad() {
    updatePageState();
    updateCounter();
}