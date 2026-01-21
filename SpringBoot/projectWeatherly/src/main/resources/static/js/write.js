document.addEventListener('DOMContentLoaded', function() {
    console.log("Write page script loaded.");

    // ==========================================
    // 1. 다크모드 로직 (헤더의 토글 버튼 연동)
    // ==========================================

    // [수정됨] HTML의 id인 'darkmode-toggle'로 변경
    const darkModeToggle = document.getElementById('darkmode-toggle');

    // 1-1. 저장된 설정 불러오기
    const savedMode = localStorage.getItem('darkMode');
    if (savedMode === 'enabled') {
        document.body.classList.add('dark-mode');
        if (darkModeToggle) {
            const icon = darkModeToggle.querySelector('i');
            if (icon) {
                icon.classList.remove('fa-moon');
                icon.classList.add('fa-sun');
            }
        }
    }

    // 1-2. 토글 버튼 클릭 이벤트
    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function() {
            document.body.classList.toggle('dark-mode');
            const icon = this.querySelector('i');

            if (document.body.classList.contains('dark-mode')) {
                // 다크모드 켜짐
                icon.classList.remove('fa-moon');
                icon.classList.add('fa-sun');
                localStorage.setItem('darkMode', 'enabled');
            } else {
                // 다크모드 꺼짐
                icon.classList.remove('fa-sun');
                icon.classList.add('fa-moon');
                localStorage.setItem('darkMode', 'disabled');
            }
        });
    }

    // ==========================================
    // 2. 카테고리 선택 기능
    // ==========================================
    const categoryTags = document.querySelectorAll('.category-tag');
    const categoryInput = document.getElementById('category');

    if (categoryTags.length > 0 && categoryInput) {
        categoryTags.forEach(tag => {
            tag.addEventListener('click', function() {
                // 모든 태그에서 selected 클래스 제거
                categoryTags.forEach(t => t.classList.remove('selected'));

                // 클릭한 태그에 selected 클래스 추가
                this.classList.add('selected');

                // hidden input 값 업데이트 (서버 전송용)
                const value = this.getAttribute('data-category');
                categoryInput.value = value;

                console.log("Selected category:", value);
            });
        });
    }

    // ==========================================
    // 3. 글자 수 세기
    // ==========================================
    const contentTextarea = document.getElementById('content');
    const contentCounter = document.getElementById('content-counter');

    if (contentTextarea && contentCounter) {
        contentTextarea.addEventListener('input', function() {
            const length = this.value.length;
            contentCounter.textContent = `${length}자`;

            // 10자 미만이면 빨간색, 아니면 회색
            if (length < 10) {
                contentCounter.style.color = '#e74c3c';
            } else {
                // 다크모드인지 확인하여 색상 결정
                contentCounter.style.color = document.body.classList.contains('dark-mode') ? '#ccc' : '#6c757d';
            }
        });
    }

    // ==========================================
    // 4. 이미지 업로드 및 미리보기
    // ==========================================
    const fileUploadArea = document.getElementById('file-upload-area');
    const fileInput = document.getElementById('image-upload');
    const filePreview = document.getElementById('file-preview');

    // 4-1. 업로드 영역 클릭 시 실제 파일 input 클릭 트리거
    if (fileUploadArea && fileInput) {
        fileUploadArea.addEventListener('click', function() {
            fileInput.click();
        });
    }

    // 4-2. 파일 선택 시 미리보기 생성
    if (fileInput && filePreview) {
        fileInput.addEventListener('change', function(e) {
            const files = e.target.files;

            // 기존 미리보기 초기화
            filePreview.innerHTML = '';

            if (files.length === 0) return;

            Array.from(files).forEach(file => {
                // 이미지 파일인지 확인
                if (!file.type.startsWith('image/')) return;

                // 용량 제한 (5MB)
                if (file.size > 5 * 1024 * 1024) {
                    alert('파일 크기는 5MB를 초과할 수 없습니다.');
                    return;
                }

                const reader = new FileReader();
                reader.onload = function(e) {
                    const item = document.createElement('div');
                    item.className = 'file-preview-item';
                    item.innerHTML = `
                        <img src="${e.target.result}" alt="미리보기">
                        <button class="file-remove-btn" type="button">
                            <i class="fas fa-times"></i>
                        </button>
                    `;

                    // 삭제 버튼 클릭 시 미리보기 제거
                    // (주의: input.files를 직접 수정하는 것은 JS 보안상 복잡하므로, 여기선 UI만 제거함)
                    const removeBtn = item.querySelector('.file-remove-btn');
                    removeBtn.addEventListener('click', function(evt) {
                        evt.stopPropagation(); // 부모(fileUploadArea) 클릭 방지
                        item.remove();
                    });

                    filePreview.appendChild(item);
                };
                reader.readAsDataURL(file);
            });
        });
    }
});