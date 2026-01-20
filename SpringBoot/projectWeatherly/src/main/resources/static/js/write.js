document.querySelectorAll('.category-tag').forEach(tag => {
    tag.addEventListener('click', function() {
        document.querySelectorAll('.category-tag').forEach(t => t.classList.remove('selected'));
        this.classList.add('selected');
        document.getElementById('category').value = this.dataset.category;
    });
});

const contentTextarea = document.getElementById('content');
const contentCounter = document.getElementById('content-counter');
contentTextarea.addEventListener('input', function() {
    const length = this.value.length;
    contentCounter.textContent = `${length}자`;
    contentCounter.style.color = length < 10 ? '#e74c3c' : '#6c757d';
});

const fileUploadArea = document.getElementById('file-upload-area');
const fileInput = document.getElementById('image-upload');
const filePreview = document.getElementById('file-preview');

fileUploadArea.addEventListener('click', () => fileInput.click());
fileInput.addEventListener('change', function(e) {
    const files = e.target.files;
    filePreview.innerHTML = '';
    Array.from(files).forEach(file => {
        if (file.size > 5 * 1024 * 1024) return alert('파일 크기는 5MB를 초과할 수 없습니다.');
        const reader = new FileReader();
        reader.onload = function(e) {
            const item = document.createElement('div');
            item.className = 'file-preview-item';
            item.innerHTML = `<img src="${e.target.result}" alt="미리보기"><button class="file-remove-btn" type="button"><i class="fas fa-times"></i></button>`;
            item.querySelector('button').addEventListener('click', () => item.remove());
            filePreview.appendChild(item);
        };
        reader.readAsDataURL(file);
    });
});