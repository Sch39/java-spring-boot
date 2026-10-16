const uploadApp = document.getElementById('uploadApp');
const fileInput = document.getElementById('fileInput');
const uploadButton = document.getElementById('uploadButton');
const pauseButton = document.getElementById('pauseButton');
const resumeButton = document.getElementById('resumeButton');
const progress = document.getElementById('progress');
const percentage = document.getElementById('percentage');
const transferred = document.getElementById('transferred');
const fileName = document.getElementById('fileName');
const fileSize = document.getElementById('fileSize');
const status = document.getElementById('status');

const endpoint = uploadApp?.dataset.uploadEndpoint;
const activeStates = new Set(['checking-resume', 'uploading', 'pausing', 'paused']);

let upload = null;
let uploadAttempt = 0;
let state = fileInput.files.length > 0 ? 'file-selected' : 'idle';
let statusMessage = state === 'file-selected'
    ? 'File siap di-upload.'
    : 'Pilih file untuk memulai upload.';
let statusTone = 'neutral';

function formatBytes(bytes) {
    if (!Number.isFinite(bytes) || bytes <= 0) {
        return '0 B';
    }

    const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
    const unitIndex = Math.min(
        Math.floor(Math.log(bytes) / Math.log(1024)),
        units.length - 1
    );
    const value = bytes / (1024 ** unitIndex);
    const digits = value >= 10 || unitIndex === 0 ? 0 : 1;

    return `${value.toFixed(digits)} ${units[unitIndex]}`;
}

function selectedFile() {
    return fileInput.files[0] ?? null;
}

function updateFileSummary() {
    const file = selectedFile();

    fileName.textContent = file ? file.name : 'Belum ada file dipilih';
    fileSize.textContent = file ? formatBytes(file.size) : '—';
}

function updateProgress(bytesUploaded, bytesTotal) {
    const rawPercentage = bytesTotal > 0
        ? (bytesUploaded / bytesTotal) * 100
        : 0;
    const safePercentage = Math.min(100, Math.max(0, rawPercentage));

    progress.value = safePercentage;
    progress.textContent = `${safePercentage.toFixed(0)}%`;
    percentage.textContent = `${safePercentage.toFixed(2)}%`;
    transferred.textContent = `${formatBytes(bytesUploaded)} dari ${formatBytes(bytesTotal)}`;
}

function setStatus(message, tone = 'neutral') {
    statusMessage = message;
    statusTone = tone;
}

function renderState() {
    const file = selectedFile();
    const isActive = activeStates.has(state);

    uploadButton.disabled = !file || isActive || state === 'pausing';
    uploadButton.textContent = state === 'failed' ? 'Coba lagi' : 'Mulai upload';
    pauseButton.disabled = state !== 'uploading';
    resumeButton.disabled = state !== 'paused';
    fileInput.disabled = isActive || state === 'pausing';

    status.textContent = statusMessage;
    status.dataset.tone = statusTone;
    status.setAttribute('role', statusTone === 'error' ? 'alert' : 'status');

    updateFileSummary();
}

function isCurrentUpload(candidate, attempt) {
    return upload === candidate && uploadAttempt === attempt;
}

function resetUploadUi() {
    upload = null;
    updateProgress(0, selectedFile()?.size ?? 0);
}

function failUpload(message, error) {
    state = 'failed';
    setStatus(message, 'error');
    console.error('Tus upload error:', error);
    renderState();
}

async function startUpload() {
    const file = selectedFile();

    if (!file) {
        state = 'idle';
        setStatus('Pilih file terlebih dahulu.', 'warning');
        renderState();
        return;
    }

    if (!window.tus?.Upload) {
        failUpload(
            'Library upload gagal dimuat. Periksa koneksi internet lalu muat ulang halaman.',
            new Error('tus-js-client is unavailable')
        );
        return;
    }

    if (!endpoint) {
        failUpload(
            'Endpoint upload tidak tersedia. Muat ulang halaman atau hubungi administrator.',
            new Error('Missing Tus upload endpoint')
        );
        return;
    }

    if (activeStates.has(state) || state === 'pausing') {
        return;
    }

    const attempt = ++uploadAttempt;
    state = 'checking-resume';
    setStatus('Mencari upload sebelumnya untuk file ini…', 'info');
    updateProgress(0, file.size);
    renderState();

    const currentUpload = new window.tus.Upload(file, {
        endpoint,
        retryDelays: [0, 1000, 3000, 5000],
        chunkSize: 5 * 1024 * 1024,
        removeFingerprintOnSuccess: true,
        metadata: {
            filename: file.name,
            filetype: file.type || 'application/octet-stream'
        },
        onError(error) {
            if (!isCurrentUpload(currentUpload, attempt)) {
                return;
            }

            failUpload('Upload gagal. Periksa koneksi lalu tekan “Coba lagi”.', error);
        },
        onProgress(bytesUploaded, bytesTotal) {
            if (!isCurrentUpload(currentUpload, attempt)) {
                return;
            }

            state = 'uploading';
            setStatus('Upload sedang berlangsung. Anda dapat menjedakannya kapan saja.', 'info');
            updateProgress(bytesUploaded, bytesTotal);
            renderState();
        },
        onSuccess() {
            if (!isCurrentUpload(currentUpload, attempt)) {
                return;
            }

            state = 'completed';
            updateProgress(file.size, file.size);
            setStatus(`Upload selesai. Resource tersimpan di ${currentUpload.url}`, 'success');
            renderState();
        }
    });

    upload = currentUpload;

    try {
        const previousUploads = await currentUpload.findPreviousUploads();

        if (!isCurrentUpload(currentUpload, attempt)) {
            return;
        }

        if (previousUploads.length > 0) {
            currentUpload.resumeFromPreviousUpload(previousUploads[0]);
            setStatus('Upload sebelumnya ditemukan. Melanjutkan dari progres terakhir…', 'info');
        } else {
            setStatus('Memulai upload baru…', 'info');
        }

        state = 'uploading';
        renderState();
        currentUpload.start();
    } catch (error) {
        if (isCurrentUpload(currentUpload, attempt)) {
            failUpload('Riwayat upload tidak dapat dibaca. Tekan “Coba lagi” untuk mengulang.', error);
        }
    }
}

uploadButton.addEventListener('click', startUpload);

pauseButton.addEventListener('click', async () => {
    const currentUpload = upload;
    const attempt = uploadAttempt;

    if (!currentUpload || state !== 'uploading') {
        return;
    }

    state = 'pausing';
    setStatus('Menjeda request yang sedang berjalan…', 'warning');
    renderState();

    try {
        await currentUpload.abort();

        if (!isCurrentUpload(currentUpload, attempt)) {
            return;
        }

        state = 'paused';
        setStatus('Upload dijeda. Tekan “Lanjutkan” untuk meneruskan dari offset terakhir.', 'warning');
        renderState();
    } catch (error) {
        if (!isCurrentUpload(currentUpload, attempt)) {
            return;
        }

        state = 'uploading';
        setStatus('Upload tidak berhasil dijeda dan masih berjalan.', 'error');
        console.error('Failed to pause Tus upload:', error);
        renderState();
    }
});

resumeButton.addEventListener('click', () => {
    if (!upload || state !== 'paused') {
        return;
    }

    state = 'uploading';
    setStatus('Melanjutkan upload dari offset terakhir…', 'info');
    renderState();
    upload.start();
});

fileInput.addEventListener('change', () => {
    if (activeStates.has(state) || state === 'pausing') {
        return;
    }

    uploadAttempt += 1;
    resetUploadUi();

    if (selectedFile()) {
        state = 'file-selected';
        setStatus('File siap di-upload.', 'neutral');
    } else {
        state = 'idle';
        setStatus('Pilih file untuk memulai upload.', 'neutral');
    }

    renderState();
});

updateFileSummary();
updateProgress(0, selectedFile()?.size ?? 0);
renderState();
