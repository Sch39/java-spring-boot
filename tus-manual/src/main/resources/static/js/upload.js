
const fileInput = document.getElementById('fileInput');
const uploadButton = document.getElementById('uploadButton');
const pauseButton = document.getElementById('pauseButton');
const resumeButton = document.getElementById('resumeButton');
const progress = document.getElementById('progress');
const percentage = document.getElementById('percentage');
const status = document.getElementById('status');

let upload = null;
let isPaused = false;
let isFinished = false;
let isFailed = false;

function updateButtons() {
    const hasUpload = upload !== null;

    uploadButton.disabled = hasUpload && !isFinished && !isFailed;
    pauseButton.disabled = !hasUpload || isPaused || isFinished || isFailed;
    resumeButton.disabled = !hasUpload || !isPaused || isFinished;
    fileInput.disabled = hasUpload && !isFinished && !isFailed;
}

function createUpload(file) {
    isPaused = false;
    isFinished = false;
    isFailed = false;

    upload = new tus.Upload(file, {
        endpoint: '/files',
        retryDelays: [0, 1000, 3000, 5000],

        chunkSize: 5 * 1024 * 1024,

        metadata: {
            filename: file.name,
            filetype: file.type || 'application/octet-stream'
        },

        onError(error) {
            isFailed = true;

            status.textContent = `Upload gagal: ${error.message}`;
            console.error('TUS upload error:', error);

            updateButtons();
        },

        onProgress(bytesUploaded, bytesTotal) {
            const percent = bytesTotal > 0
                ? (bytesUploaded / bytesTotal) * 100
                : 0;

            progress.value = percent;
            percentage.textContent = `${percent.toFixed(2)}%`;

            status.textContent =
                `Terupload ${bytesUploaded} dari ${bytesTotal} bytes`;
        },

        onSuccess() {
            isFinished = true;
            isPaused = false;

            progress.value = 100;
            percentage.textContent = '100%';
            status.textContent = 'Upload berhasil diselesaikan!';

            console.log('Upload selesai:', upload.url);

            updateButtons();
        }
    });

    updateButtons();
}

uploadButton.addEventListener('click', () => {
    const file = fileInput.files[0];

    if (!file) {
        status.textContent = 'Silakan pilih file terlebih dahulu.';
        return;
    }

    // Jangan membuat upload baru jika masih ada upload aktif.
    if (upload && !isFinished && !isFailed) {
        return;
    }

    progress.value = 0;
    percentage.textContent = '0%';

    createUpload(file);

    status.textContent = 'Memulai upload...';
    upload.start();
});

pauseButton.addEventListener('click', async () => {
    if (!upload || isPaused || isFinished) {
        return;
    }

    // Tunggu pembatalan request yang sedang berlangsung.
    await upload.abort();

    isPaused = true;
    status.textContent = 'Upload dijeda. Tekan Resume untuk melanjutkan.';

    updateButtons();
});

resumeButton.addEventListener('click', () => {
    if (!upload || !isPaused || isFinished) {
        return;
    }

    isPaused = false;
    status.textContent = 'Melanjutkan upload...';

    updateButtons();

    // Gunakan instance yang sama agar URL upload tetap sama.
    upload.start();
});

fileInput.addEventListener('change', () => {
    status.textContent = fileInput.files.length
        ? `File dipilih: ${fileInput.files[0].name}`
        : 'Pilih file untuk memulai upload.';
});
