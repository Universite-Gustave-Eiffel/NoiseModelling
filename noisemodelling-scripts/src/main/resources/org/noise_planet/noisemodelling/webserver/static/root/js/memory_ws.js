// small helper function for selecting element by id
let id = id => document.getElementById(id);

function openMemoryStatsStream(url) {
    console.log("Opening WebSocket connection to " + url);
    // Establish the WebSocket connection and set up event handlers
    let ws = new WebSocket(url);
    ws.onmessage = msg => updateMemoryGraph(JSON.parse(msg.data));
    ws.onerror = () => console.error("WebSocket error: " + ws.readyState);
    ws.onclose = () => {
        console.log("WebSocket closed. Attempting to reconnect in 5 seconds...");
        setTimeout(() => openMemoryStatsStream(url), 5000);
    };
}

function updateMemoryGraph(data) {
    const max = data.max;
    const used = data.used;
    const free = data.free;

    // Update text values
    id("memory-max").textContent = formatBytes(max);
    id("memory-used").textContent = formatBytes(used);
    id("memory-free").textContent = formatBytes(free);

    // Update bars
    const usedPercent = (used / max) * 100;
    const freePercent = (free / max) * 100;

    id("memory-used-bar").style.width = usedPercent + "%";
    id("memory-free-bar").style.width = freePercent + "%";
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
