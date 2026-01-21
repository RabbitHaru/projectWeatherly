// ===== 마이페이지 기능 구현 =====

document.addEventListener('DOMContentLoaded', function() {

    // ==========================================
    // 1. 다크모드 로직 (login.js와 통일)
    // ==========================================
    const darkModeToggle = document.getElementById('darkModeToggle');

    // 페이지 로드 시 저장된 설정 확인
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

    // 토글 버튼 클릭 이벤트
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
    // 2. 마이페이지 기존 기능 유지
    // ==========================================

    // DOM 요소 가져오기
    const profileImageInput = document.getElementById('file'); // HTML ID가 file임
    const editProfilePreview = document.getElementById('previewImage');
    const nicknameCheckBtn = document.getElementById('nicknameCheckBtn'); // HTML에 버튼 ID 필요
    const passwordChangeForm = document.querySelector('form[action="/mypage/password/change"]'); // form selector 수정

    // 파일 입력 변경 시 미리보기 (이벤트 리스너 방식 보강)
    if (profileImageInput) {
        profileImageInput.addEventListener('change', function() {
            previewFile(this);
        });
    }

    // 닉네임 중복 확인 (버튼이 있다면 동작)
    if (nicknameCheckBtn) {
        nicknameCheckBtn.addEventListener('click', function() {
            const nicknameInput = document.querySelector('input[name="nickname"]');

            if (!nicknameInput) return;
            const nickname = nicknameInput.value.trim();

            if (!nickname) {
                alert('닉네임을 입력해주세요.');
                return;
            }

            if (nickname.length < 2 || nickname.length > 10) {
                alert('닉네임은 2~10자로 입력해주세요.');
                return;
            }

            // 가짜 응답 (실제 구현 시 AJAX 사용)
            alert('사용 가능한 닉네임입니다.');
        });
    }

    // 비밀번호 변경 폼 제출 검증
    if (passwordChangeForm) {
        passwordChangeForm.addEventListener('submit', function(e) {
            const currentPassword = document.getElementById('currentPassword');
            const newPassword = document.getElementById('newPassword');
            const confirmPassword = document.getElementById('confirmPassword');

            // HTML required 속성이 있지만 추가 검증
            if (newPassword.value !== confirmPassword.value) {
                e.preventDefault();
                alert('새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
                return;
            }

            if (newPassword.value.length < 8) {
                e.preventDefault();
                alert('비밀번호는 8자 이상이어야 합니다.');
                return;
            }
        });
    }

    // 게시물 삭제 확인 (form 제출 시)
    document.querySelectorAll('form[action*="delete"]').forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!confirm('정말 삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.')) {
                e.preventDefault();
            }
        });
    });
});

// ==========================================
// 3. 전역 함수 (HTML onclick 속성 지원용)
// ==========================================

// 이미지 미리보기
window.previewFile = function(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('previewImage');
            if (preview) {
                preview.src = e.target.result;
            }
        }
        reader.readAsDataURL(input.files[0]);
    }
};

// 프로필 수정 모드 토글
window.toggleEditMode = function(showEdit) {
    const view = document.getElementById('profileView');
    const edit = document.getElementById('profileEdit');

    if (view && edit) {
        if (showEdit) {
            view.style.display = 'none';
            edit.style.display = 'block';
        } else {
            view.style.display = 'block';
            edit.style.display = 'none';
        }
    }
};

// 탭 전환 함수
window.switchTab = function(tabName) {
    // 1. 모든 탭 컨텐츠 숨김
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));

    // 2. 모든 탭 버튼 비활성화 스타일
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

    // 3. 선택된 탭 컨텐츠 표시
    const targetContent = document.getElementById('tab-' + tabName);
    if (targetContent) {
        targetContent.classList.add('active');
    }

    // 4. 클릭된 버튼 스타일 활성화 (탭 순서에 따라 매칭)
    const tabs = ['posts', 'password', 'notification', 'reports'];
    const index = tabs.indexOf(tabName);

    const navItems = document.querySelectorAll('.nav-item');
    if (index !== -1 && navItems[index]) {
        navItems[index].classList.add('active');
    }
};