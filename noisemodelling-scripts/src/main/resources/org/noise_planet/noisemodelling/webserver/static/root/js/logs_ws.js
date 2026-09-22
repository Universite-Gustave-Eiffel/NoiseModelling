// small helper function for selecting element by id
let id = id => document.getElementById(id);
let lastEpoch = 0;
function openJobLogsStream(url, lastMessageEpoch) {
    let modifiedUrl = url+"?lastReceivedMessageEpoch="+lastMessageEpoch;
    console.log("Opening WebSocket connection to " + modifiedUrl);
    //Establish the WebSocket connection and set up event handlers
    let ws = new WebSocket(modifiedUrl);
    lastEpoch = lastMessageEpoch;
    ws.onmessage = msg => addLogline(msg)
    ws.onerror = () => console.error("WebSocket error: " + ws.readyState);
    ws.onclose = () => {
        console.log("WebSocket closed. Attempting to reconnect in 5 seconds...");
        setTimeout(() => openJobLogsStream(url, lastEpoch), 5000);
    };

}

function addLogline(msg) {
    let logContainer = id("logs");
    if (logContainer) {
        let [epoch, message] = msg.data.split(":");
        let messageEpoch = parseInt(epoch);
        if(messageEpoch > lastEpoch) {
            lastEpoch = messageEpoch;
        }
        logContainer.insertAdjacentText("afterbegin", message);
    }
}
