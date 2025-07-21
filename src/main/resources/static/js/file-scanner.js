document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('scanForm');
  const startBtn = document.getElementById('startBtn');
  const cancelBtn = document.getElementById('cancelBtn');
  const infoBar = document.getElementById('info');
  const tokenDisplay = document.getElementById('tokenDisplay');
  const timerEl = document.getElementById('timer');
  const countEl = document.getElementById('count');
  const spinner = document.getElementById('spinner');
  const resultsTable = document.getElementById('resultsTable');
  const resultsBody = resultsTable.querySelector('tbody');
  const errorBox = document.getElementById('error');

  let token = null, timerInterval = null, elapsed = 0, foundCount = 0;

  form.addEventListener('submit', async e => {
    e.preventDefault();
    resetUI();
    toggleForm(true);

    const payload = {
      directoryPath: form.directoryPath.value.trim() || null,
      threadsCount: form.threadsCount.value ? parseInt(form.threadsCount.value, 10) : null,
      depthLimit: form.depthLimit.value ? parseInt(form.depthLimit.value, 10) : null,
      scanFilter: {
        namePattern: form.namePattern.value.trim() || null,
        sizeInBytesInterval: (form.sizeStart.value || form.sizeEnd.value)
          ? { start: form.sizeStart.value ? parseInt(form.sizeStart.value,10) : null,
              end: form.sizeEnd.value ? parseInt(form.sizeEnd.value,10) : null }
          : null,
        lastModifiedDateInterval: (form.dateStart.value || form.dateEnd.value)
          ? { start: form.dateStart.value || null,
              end: form.dateEnd.value || null }
          : null,
        lastModifiedTimeInterval: (form.timeStart.value || form.timeEnd.value)
          ? { start: form.timeStart.value || null,
              end: form.timeEnd.value || null }
          : null,
        textContent: form.textContent.value.trim() || null
      }
    };

    try {
      const res = await fetch('/api/file-scanner/scan', {
        method: 'POST',
        headers: buildHeaders(),
        credentials: 'same-origin',
        body: JSON.stringify(payload)
      });
      const json = await res.json();
      if (!res.ok) throw new Error(json.errors ? Object.values(json.errors).join(', ') : res.statusText);

      token = json.data.token;
      tokenDisplay.textContent = `Token: ${token}`;
      infoBar.classList.remove('hidden');
      spinner.classList.remove('hidden');
      timerInterval = setInterval(() => {
        elapsed++;
        const m = String(Math.floor(elapsed/60)).padStart(2,'0');
        const s = String(elapsed%60).padStart(2,'0');
        timerEl.textContent = `${m}:${s}`;
      }, 1000);

      poll();
    } catch (err) {
      showError(`Start error: ${err.message}`);
      toggleForm(false);
    }
  });

  cancelBtn.addEventListener('click', async () => {
    if (!token) return;
    try {
      await fetch(`/api/file-scanner/scan/${token}`, {
        method: 'DELETE',
        headers: buildHeaders()
      });
      stopScanning('Scan canceled.');
    } catch (err) {
      showError(`Cancel error: ${err.message}`);
    }
  });

  async function poll() {
    try {
      const res = await fetch(`/api/file-scanner/scan/${token}`, { headers: buildHeaders() });
      const json = await res.json();
      if (!res.ok && res.status !== 202) throw new Error(res.statusText);

      const dto = json.data;
      updateResults(dto.result || []);
      if (dto.completed) {
        stopScanning('Scan completed.');
      } else {
        setInterval(poll, 1500);
      }
    } catch (err) {
      stopScanning(`Polling error: ${err.message}`);
    }
  }

  function updateResults(list) {
    if (list.length > foundCount) {
      list.slice(foundCount).forEach((path, i) => {
        const row = resultsBody.insertRow();
        row.insertCell().textContent = foundCount + i + 1;
        row.insertCell().textContent = path;
      });
      foundCount = list.length;
      countEl.textContent = `Found: ${foundCount}`;
      resultsTable.classList.remove('hidden');
    }
  }

  function stopScanning(msg) {
    clearInterval(timerInterval);
    spinner.classList.add('hidden');
    showError(msg);
    toggleForm(false);
  }

  function toggleForm(disable) {
    Array.from(form.elements).forEach(el => { if (el.tagName==='INPUT') el.disabled = disable; });
    startBtn.disabled = disable;
    cancelBtn.disabled = !disable;
  }

  function resetUI() {
    elapsed = foundCount = 0;
    timerEl.textContent = '00:00';
    countEl.textContent = 'Found: 0';
    infoBar.classList.add('hidden');
    spinner.classList.add('hidden');
    resultsTable.classList.add('hidden');
    resultsBody.innerHTML = '';
    errorBox.classList.add('hidden');
    errorBox.textContent = '';
  }

  function showError(txt) {
    errorBox.textContent = txt;
    errorBox.classList.remove('hidden');
  }

  function buildHeaders() {
    const hdr = { 'Content-Type': 'application/json' };
    const auth = sessionStorage.getItem('authHeader');
    if (auth) hdr['Authorization'] = auth;
    return hdr;
  }
});
