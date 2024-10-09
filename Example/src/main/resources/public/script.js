const backendApi = "/api";
const updateSessionTime = 10; // In seconds

async function runOnBackEnd(methodName, data = {}) {
    async function getSession() {
        let sessionId = sessionStorage.getItem('sessionId');

        // If no session ID, create a new session
        if (!sessionId) {
            let session = await postBackend({ methodName: "newSession" });
            if (session && session.sessionId) {
                sessionStorage.setItem('sessionId', session.sessionId);
                sessionId = session.sessionId;
            } else {
                console.error("Failed to create new session.");
                return null;
            }
        }
        return sessionId;
    }

    function resetSession() {
        sessionStorage.setItem("sessionId", "");
    }

    async function postBackend(data = {}) {
        return fetch(backendApi, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }).then(async response => {
            const responseData = await response.json();

            if (responseData.error) {
                console.error(JSON.stringify(responseData));
            }
            if (responseData.resetSession) {
                resetSession();
            }

            return responseData;
        }).catch(() => {
            console.error("Network error occurred.");
            return null;
        });
    }

    // Get the current session ID and make the API call
    const sessionId = await getSession();
    if (sessionId) {
        return await postBackend({ data: data, methodName: methodName, sessionId: sessionId });
    }
}

// Automatically update the session time periodically
setInterval(() => {
    runOnBackEnd("updateSession", {}).then(r => {});
}, updateSessionTime * 1000);
