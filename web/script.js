(function () {
  const term = document.getElementById('terminal');
  const input = document.getElementById('input');
  const wsUrl = document.getElementById('wsUrl');
  const connectBtn = document.getElementById('connectBtn');
  const disconnectBtn = document.getElementById('disconnectBtn');
  const enterBtn = document.getElementById('enterBtn');
  const ctrlCBtn = document.getElementById('ctrlCBtn');
  const backspaceBtn = document.getElementById('backspaceBtn');
  const localEcho = document.getElementById('localEcho');

  let ws = null;
  let pingTimer = null;

  function append(spanClass, text) {
    const el = document.createElement('span');
    if (spanClass) el.className = spanClass;
    el.textContent = text;
    term.appendChild(el);
    term.appendChild(document.createElement('br'));
    term.scrollTop = term.scrollHeight;
  }

  function appendRaw(text) {
    const el = document.createElement('span');
    el.textContent = text;
    term.appendChild(el);
    term.scrollTop = term.scrollHeight;
  }

  // Remove ANSI CSI sequences, OSC sequences (like title/8003 markers), and bracketed paste toggles
  function sanitizeAnsi(s) {
    if (!s) return s;
    // Normalize CRLF
    s = s.replace(/\r\n/g, '\n');
    // OSC sequences: ESC ] ... BEL or ESC ] ... ESC \
    s = s.replace(/\u001b\][0-9]{1,4};[^\u0007\u001b]*?(\u0007|\u001b\\)/g, '');
    // CSI sequences: ESC [ params intermediates final-byte
    s = s.replace(/\u001b\[[0-9;?]*[ -\/]*[@-~]/g, '');
    // SOS/PM/APC (rare): ESC _ ... ESC \
    s = s.replace(/\u001b_[\s\S]*?\u001b\\/g, '');
    // Remove stray BEL used for titles
    s = s.replace(/\u0007/g, '');
    return s;
  }

  function setConnectedState(connected) {
    connectBtn.disabled = connected;
    disconnectBtn.disabled = !connected;
  }

  function connect() {
    if (ws) ws.close();
    try {
      ws = new WebSocket(wsUrl.value);
    } catch (e) {
      append('err', 'Invalid WS URL');
      return;
    }

    ws.binaryType = 'arraybuffer';

    ws.onopen = () => {
      setConnectedState(true);
      append('ok', 'Connected');
      clearInterval(pingTimer);
      pingTimer = setInterval(() => {
        try { ws.send(new Uint8Array([0x9])); } catch (_) {}
      }, 30000);
    };

    ws.onmessage = (evt) => {
      if (evt.data instanceof ArrayBuffer) {
        const text = new TextDecoder('utf-8').decode(new Uint8Array(evt.data));
        appendRaw(sanitizeAnsi(text));
      } else if (typeof evt.data === 'string') {
        appendRaw(sanitizeAnsi(evt.data));
      }
    };

    ws.onclose = (evt) => {
      setConnectedState(false);
      clearInterval(pingTimer);
      append('err', `Disconnected (${evt.code}${evt.reason ? ': ' + evt.reason : ''})`);
    };

    ws.onerror = (evt) => {
      append('err', 'WebSocket error');
    };
  }

  function disconnect() {
    if (ws) {
      ws.close();
      ws = null;
    }
  }

  function sendLine() {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    const text = input.value;
    if (localEcho.checked) appendRaw(text + '\n');
    ws.send(text + '\n');
    input.value = '';
    input.focus();
  }

  function sendCtrlC() {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(new Uint8Array([0x03]));
  }

  function sendBackspace() {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(new Uint8Array([0x7f]));
  }

  connectBtn.addEventListener('click', connect);
  disconnectBtn.addEventListener('click', disconnect);
  enterBtn.addEventListener('click', sendLine);
  ctrlCBtn.addEventListener('click', sendCtrlC);
  backspaceBtn.addEventListener('click', sendBackspace);
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      sendLine();
    } else if (e.key === 'c' && e.ctrlKey) {
      e.preventDefault();
      sendCtrlC();
    } else if (e.key === 'Backspace' && e.ctrlKey) {
      e.preventDefault();
      sendBackspace();
    }
  });

  connect();
})();

