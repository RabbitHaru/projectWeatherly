// 다크모드 초기화 함수
function initializeDarkMode() {
    const darkModeToggle = document.getElementById('darkModeToggle');

    // 다크모드 토글이 없을 경우 처리
    if (!darkModeToggle) {
        return;
    }

    // 로컬 스토리지에서 다크모드 설정 확인
    const isDarkMode = localStorage.getItem('darkMode') === 'true';

    // 초기 다크모드 설정 적용
    if (isDarkMode) {
        document.body.classList.add('dark-mode');
        darkModeToggle.innerHTML = '<i class="fas fa-sun"></i>';
        if (darkModeToggle.hasAttribute('aria-label')) {
            darkModeToggle.setAttribute('aria-label', '라이트 모드로 전환');
        }
    } else {
        document.body.classList.remove('dark-mode');
        darkModeToggle.innerHTML = '<i class="fas fa-moon"></i>';
        if (darkModeToggle.hasAttribute('aria-label')) {
            darkModeToggle.setAttribute('aria-label', '다크 모드로 전환');
        }
    }

    // 다크모드 토글 이벤트 리스너
    darkModeToggle.addEventListener('click', function() {
        const isDarkMode = document.body.classList.toggle('dark-mode');

        // 로컬 스토리지에 저장
        localStorage.setItem('darkMode', isDarkMode.toString());

        // 아이콘 및 텍스트 변경
        if (isDarkMode) {
            this.innerHTML = '<i class="fas fa-sun"></i>';
            if (this.hasAttribute('aria-label')) {
                this.setAttribute('aria-label', '라이트 모드로 전환');
            }
        } else {
            this.innerHTML = '<i class="fas fa-moon"></i>';
            if (this.hasAttribute('aria-label')) {
                this.setAttribute('aria-label', '다크 모드로 전환');
            }
        }
    });
}

// 초기화 코드
document.addEventListener('DOMContentLoaded', function() {
    // 다크모드 초기화
    initializeDarkMode();

    // 글자 수 카운터 초기화
    updateContentCounter();

    // 카테고리 선택
    document.querySelectorAll('.category-tag').forEach(tag => {
        tag.addEventListener('click', function() {
            // 모든 태그에서 selected 클래스 제거
            document.querySelectorAll('.category-tag').forEach(t => {
                t.classList.remove('selected');
            });

            // 클릭한 태그에 selected 클래스 추가
            this.classList.add('selected');

            // 히든 인풋에 값 설정
            document.getElementById('category').value = this.dataset.category;
        });
    });

    // 이미지 삭제 체크박스 토글
    document.querySelectorAll('.image-delete-checkbox input').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const imageItem = this.closest('.existing-image-item');
            if (imageItem) {
                if (this.checked) {
                    imageItem.classList.add('to-delete');
                    const img = imageItem.querySelector('img');
                    if (img) img.style.opacity = '0.5';
                } else {
                    imageItem.classList.remove('to-delete');
                    const img = imageItem.querySelector('img');
                    if (img) img.style.opacity = '1';
                }
            }
        });
    });

    // 폼 제출 시 유효성 검사
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function(e) {
            const title = document.getElementById('title')?.value?.trim();
            const content = document.getElementById('content')?.value?.trim();
            const category = document.getElementById('category')?.value;

            if (!title) {
                e.preventDefault();
                alert('제목을 입력해주세요.');
                document.getElementById('title')?.focus();
                return;
            }

            if (!category) {
                e.preventDefault();
                alert('카테고리를 선택해주세요.');
                return;
            }

            if (!content || content.length < 10) {
                e.preventDefault();
                alert('내용은 최소 10자 이상 입력해주세요.');
                document.getElementById('content')?.focus();
                return;
            }

            // 게시글 ID 확인
            const boardId = document.querySelector('input[name="id"]')?.value;
            if (!boardId || boardId === '0') {
                e.preventDefault();
                alert('유효하지 않은 게시글입니다.');
                return;
            }

            // 수정 확인
            if (!confirm('게시글을 수정하시겠습니까?')) {
                e.preventDefault();
            }
        });
    }
});

// 내용 글자 수 카운터
const contentTextarea = document.getElementById('content');
const contentCounter = document.getElementById('content-counter');

function updateContentCounter() {
    if (!contentTextarea || !contentCounter) return;

    const length = contentTextarea.value.length;
    const counterSpan = contentCounter.querySelector('span');
    if (counterSpan) {
        counterSpan.textContent = length;
    } else {
        contentCounter.textContent = length + '자';
    }

    if (length < 10) {
        contentCounter.style.color = '#e74c3c';
    } else {
        contentCounter.style.color = '#6c757d';
    }
}

if (contentTextarea) {
    contentTextarea.addEventListener('input', updateContentCounter);
    // 초기값 설정
    setTimeout(updateContentCounter, 100);
}

// 파일 업로드 기능
const fileUploadArea = document.getElementById('file-upload-area');
const fileInput = document.getElementById('image-upload');
const filePreview = document.getElementById('file-preview');

if (fileUploadArea && fileInput) {
    // 파일 업로드 영역 클릭 시 파일 선택 다이얼로그 열기
    fileUploadArea.addEventListener('click', function() {
        fileInput.click();
    });

    // 파일 선택 시 미리보기 생성
    fileInput.addEventListener('change', function(e) {
        const files = e.target.files;

        // 기존 미리보기 초기화
        if (filePreview) {
            filePreview.innerHTML = '';
        }

        Array.from(files).forEach((file, index) => {
            if (file.size > 5 * 1024 * 1024) { // 5MB 제한
                alert('파일 크기는 5MB를 초과할 수 없습니다: ' + file.name);
                return;
            }

            const reader = new FileReader();
            reader.onload = function(e) {
                const previewItem = document.createElement('div');
                previewItem.className = 'file-preview-item';
                previewItem.dataset.fileIndex = index;

                const img = document.createElement('img');
                img.src = e.target.result;
                img.alt = '새 이미지 미리보기';

                const removeBtn = document.createElement('button');
                removeBtn.className = 'file-remove-btn';
                removeBtn.type = 'button'; // 폼 제출 방지
                removeBtn.innerHTML = '<i class="fas fa-times"></i>';
                removeBtn.addEventListener('click', function() {
                    // DataTransfer를 사용하여 파일 목록에서 제거
                    const dt = new DataTransfer();
                    const fileList = fileInput.files;

                    for (let i = 0; i < fileList.length; i++) {
                        if (i !== parseInt(previewItem.dataset.fileIndex)) {
                            dt.items.add(fileList[i]);
                        }
                    }

                    fileInput.files = dt.files;
                    previewItem.remove();
                    updateFilePreview();
                });

                previewItem.appendChild(img);
                previewItem.appendChild(removeBtn);
                if (filePreview) {
                    filePreview.appendChild(previewItem);
                }
            };

            reader.readAsDataURL(file);
        });
    });

    // 미리보기 업데이트 함수
    function updateFilePreview() {
        if (!filePreview) return;

        const files = fileInput.files;
        filePreview.innerHTML = '';

        Array.from(files).forEach((file, index) => {
            const reader = new FileReader();
            reader.onload = function(e) {
                const previewItem = document.createElement('div');
                previewItem.className = 'file-preview-item';
                previewItem.dataset.fileIndex = index;

                const img = document.createElement('img');
                img.src = e.target.result;
                img.alt = '새 이미지 미리보기';

                const removeBtn = document.createElement('button');
                removeBtn.className = 'file-remove-btn';
                removeBtn.type = 'button';
                removeBtn.innerHTML = '<i class="fas fa-times"></i>';
                removeBtn.addEventListener('click', function() {
                    const dt = new DataTransfer();
                    const fileList = fileInput.files;

                    for (let i = 0; i < fileList.length; i++) {
                        if (i !== parseInt(previewItem.dataset.fileIndex)) {
                            dt.items.add(fileList[i]);
                        }
                    }

                    fileInput.files = dt.files;
                    updateFilePreview();
                });

                previewItem.appendChild(img);
                previewItem.appendChild(removeBtn);
                filePreview.appendChild(previewItem);
            };

            reader.readAsDataURL(file);
        });
    }

    // 드래그 앤 드롭 파일 업로드
    fileUploadArea.addEventListener('dragover', function(e) {
        e.preventDefault();
        this.style.borderColor = '#667eea';
        this.style.background = '#f8f9fa';
    });

    fileUploadArea.addEventListener('dragleave', function(e) {
        e.preventDefault();
        this.style.borderColor = '#dee2e6';
        this.style.background = '';
    });

    fileUploadArea.addEventListener('drop', function(e) {
        e.preventDefault();
        this.style.borderColor = '#dee2e6';
        this.style.background = '';

        const files = e.dataTransfer.files;
        fileInput.files = files;

        // 파일 변경 이벤트 트리거
        const event = new Event('change', { bubbles: true });
        fileInput.dispatchEvent(event);
    });
}

// 이미지 로드 오류 처리
document.querySelectorAll('.existing-image-item img').forEach(img => {
    img.addEventListener('error', function() {
        this.style.display = 'none';
        const parent = this.closest('.existing-image-item');
        if (parent) {
            parent.querySelector('.existing-image-info').innerHTML =
                '<span style="color: #e74c3c;">이미지를 로드할 수 없습니다</span>';
        }
    });
});