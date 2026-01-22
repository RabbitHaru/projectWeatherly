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
    // 2. 마이페이지 프로필 수정 및 닉네임 중복 확인
    // ==========================================

    const profileImageInput = document.getElementById('file');
    const nicknameCheckBtn = document.getElementById('nicknameCheckBtn');
    const nicknameInput = document.getElementById('nicknameInput');
    const nicknameMessage = document.getElementById('nicknameMessage');
    const profileEditForm = document.getElementById('profileEditForm');

    // 파일 입력 변경 시 미리보기
    if (profileImageInput) {
        profileImageInput.addEventListener('change', function() {
            previewFile(this);
        });
    }

    // ★ 닉네임 중복 확인 로직 시작
    let originalNickname = "";
    let isNicknameVerified = true; // 초기값은 본인이므로 true

    if (nicknameInput) {
        originalNickname = nicknameInput.value;
    }

    // 닉네임이 변경되면 검증 상태를 false로 변경
    if (nicknameInput) {
        nicknameInput.addEventListener('input', function() {
            const currentVal = this.value.trim();

            if (currentVal === originalNickname) {
                // 원래 닉네임으로 돌아오면 검증된 것으로 간주
                isNicknameVerified = true;
                nicknameMessage.textContent = "";
                if(nicknameCheckBtn) nicknameCheckBtn.style.background = "#6c757d"; // 회색
            } else {
                // 변경되었으면 검증 필요
                isNicknameVerified = false;
                nicknameMessage.textContent = "중복 확인이 필요합니다.";
                nicknameMessage.style.color = "#e74c3c"; // 빨간색
                if(nicknameCheckBtn) nicknameCheckBtn.style.background = "#667eea"; // 파란색 (강조)
            }
        });
    }

    // [중복 확인] 버튼 클릭 시
    if (nicknameCheckBtn) {
        nicknameCheckBtn.addEventListener('click', function() {
            const nickname = nicknameInput.value.trim();

            if (!nickname) {
                alert('닉네임을 입력해주세요.');
                return;
            }

            if (nickname.length < 2 || nickname.length > 10) {
                nicknameMessage.textContent = '닉네임은 2~10자로 입력해주세요.';
                nicknameMessage.style.color = '#e74c3c';
                return;
            }

            // 원래 닉네임과 같다면 서버 통신 없이 통과
            if (nickname === originalNickname) {
                nicknameMessage.textContent = '현재 사용 중인 본인의 닉네임입니다.';
                nicknameMessage.style.color = '#28a745'; // 초록색
                isNicknameVerified = true;
                return;
            }

            // 서버에 중복 확인 요청 (signup.js와 동일한 API)
            fetch('/auth/api/check-nickname?nickname=' + encodeURIComponent(nickname))
                .then(response => response.json())
                .then(isDuplicate => {
                    if (isDuplicate) {
                        nicknameMessage.textContent = '이미 사용 중인 닉네임입니다.';
                        nicknameMessage.style.color = '#e74c3c';
                        isNicknameVerified = false;
                    } else {
                        nicknameMessage.textContent = '사용 가능한 닉네임입니다.';
                        nicknameMessage.style.color = '#28a745';
                        isNicknameVerified = true;
                    }
                })
                .catch(err => {
                    console.error('Error:', err);
                    alert('중복 확인 중 오류가 발생했습니다.');
                });
        });
    }

    // [저장] 폼 제출 시 검증
    if (profileEditForm) {
        profileEditForm.addEventListener('submit', function(e) {
            // 닉네임 검증이 안 되었으면 제출 차단
            if (!isNicknameVerified) {
                e.preventDefault();
                alert('닉네임 중복 확인을 해주세요.');
                nicknameInput.focus();
                return;
            }
        });
    }
    // ★ 닉네임 중복 확인 로직 끝

    // ==========================================
    // 3. 비밀번호 변경 기능
    // ==========================================
    const passwordChangeForm = document.querySelector('form[action="/mypage/password/change"]');

    if (passwordChangeForm) {
        passwordChangeForm.addEventListener('submit', function(e) {
            const currentPassword = document.querySelector('input[name="currentPassword"]');
            const newPassword = document.querySelector('input[name="newPassword"]');
            const confirmPassword = document.querySelector('input[name="confirmPassword"]');

            if (!currentPassword || !newPassword || !confirmPassword) {
                console.error('비밀번호 필드를 찾을 수 없습니다.');
                return;
            }

            const currentValue = currentPassword.value.trim();
            const newValue = newPassword.value.trim();
            const confirmValue = confirmPassword.value.trim();

            if (!currentValue || !newValue || !confirmValue) {
                e.preventDefault();
                alert('모든 필드를 입력해주세요.');
                return;
            }

            if (currentValue === newValue) {
                e.preventDefault();
                alert('현재 비밀번호와 새 비밀번호가 같습니다.\n다른 비밀번호를 입력해주세요.');
                newPassword.focus();
                return;
            }

            if (newValue !== confirmValue) {
                e.preventDefault();
                alert('새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
                confirmPassword.focus();
                return;
            }

            if (newValue.length < 8) {
                e.preventDefault();
                alert('비밀번호는 8자 이상이어야 합니다.');
                newPassword.focus();
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
// 4. 전역 함수 (HTML onclick 속성 지원용)
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

            // 수정 모드 진입 시 닉네임 검증 상태 초기화 (원래 닉네임이므로 통과 상태)
            const nicknameMessage = document.getElementById('nicknameMessage');
            if(nicknameMessage) nicknameMessage.textContent = "";
        } else {
            view.style.display = 'block';
            edit.style.display = 'none';
        }
    }
};

// 탭 전환 함수
window.switchTab = function(tabName) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

    const targetContent = document.getElementById('tab-' + tabName);
    if (targetContent) {
        targetContent.classList.add('active');
    }

    const tabs = ['posts', 'password', 'notification', 'reports'];
    const index = tabs.indexOf(tabName);

    const navItems = document.querySelectorAll('.nav-item');
    if (index !== -1 && navItems[index]) {
        navItems[index].classList.add('active');
    }
};