// ===== 마이페이지 기능 구현 =====

document.addEventListener('DOMContentLoaded', function() {

    // ==========================================
    // 1. 다크모드 로직
    // ==========================================
    const darkModeToggle = document.getElementById('darkModeToggle');
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

    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function() {
            document.body.classList.toggle('dark-mode');
            const icon = this.querySelector('i');

            if (document.body.classList.contains('dark-mode')) {
                icon.classList.remove('fa-moon');
                icon.classList.add('fa-sun');
                localStorage.setItem('darkMode', 'enabled');
            } else {
                icon.classList.remove('fa-sun');
                icon.classList.add('fa-moon');
                localStorage.setItem('darkMode', 'disabled');
            }
        });
    }

    // ==========================================
    // 2. 프로필 수정 및 닉네임 중복 확인
    // ==========================================
    const profileImageInput = document.getElementById('file');
    const nicknameCheckBtn = document.getElementById('nicknameCheckBtn');
    const nicknameInput = document.getElementById('nicknameInput');
    const nicknameMessage = document.getElementById('nicknameMessage');
    const profileEditForm = document.getElementById('profileEditForm');

    if (profileImageInput) {
        profileImageInput.addEventListener('change', function() {
            previewFile(this);
        });
    }

    let originalNickname = "";
    let isNicknameVerified = true;

    if (nicknameInput) {
        originalNickname = nicknameInput.value;
    }

    if (nicknameInput) {
        nicknameInput.addEventListener('input', function() {
            const currentVal = this.value.trim();
            if (currentVal === originalNickname) {
                isNicknameVerified = true;
                nicknameMessage.textContent = "";
                if(nicknameCheckBtn) nicknameCheckBtn.style.background = "#6c757d";
            } else {
                isNicknameVerified = false;
                nicknameMessage.textContent = "중복 확인이 필요합니다.";
                nicknameMessage.style.color = "#e74c3c";
                if(nicknameCheckBtn) nicknameCheckBtn.style.background = "#667eea";
            }
        });
    }

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
            if (nickname === originalNickname) {
                nicknameMessage.textContent = '현재 사용 중인 본인의 닉네임입니다.';
                nicknameMessage.style.color = '#28a745';
                isNicknameVerified = true;
                return;
            }

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

    if (profileEditForm) {
        profileEditForm.addEventListener('submit', function(e) {
            if (!isNicknameVerified) {
                e.preventDefault();
                alert('닉네임 중복 확인을 해주세요.');
                nicknameInput.focus();
                return;
            }
        });
    }

    // ==========================================
    // 3. 비밀번호 변경 기능
    // ==========================================
    const passwordChangeForm = document.querySelector('form[action="/mypage/password/change"]');

    if (passwordChangeForm) {
        passwordChangeForm.addEventListener('submit', function(e) {
            const currentPassword = document.querySelector('input[name="currentPassword"]');
            const newPassword = document.querySelector('input[name="newPassword"]');
            const confirmPassword = document.querySelector('input[name="confirmPassword"]');

            if (!currentPassword || !newPassword || !confirmPassword) return;

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
                alert('현재 비밀번호와 새 비밀번호가 같습니다.');
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

    // 게시물 삭제 확인
    document.querySelectorAll('form[action*="delete"]').forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!confirm('정말 삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.')) {
                e.preventDefault();
            }
        });
    });

    // ==========================================
    // [추가됨] 4. 프로필 이미지 에러 처리
    // ==========================================
    const profileImages = document.querySelectorAll('.profile-img-wrapper img, #previewImage');
    profileImages.forEach(img => {
        img.addEventListener('error', function() {
            this.src = '/uploads/default.png';
            this.style.background = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
        });
    });

    // ==========================================
    // [추가됨] 5. URL 파라미터(tab)에 따른 탭 활성화
    // ==========================================
    const urlParams = new URLSearchParams(window.location.search);
    const activeTab = urlParams.get('tab');
    if (activeTab) {
        if (typeof switchTab === 'function') {
            switchTab(activeTab);
        }
    }
});

// ==========================================
// 6. 전역 함수 (HTML onclick 등에서 사용)
// ==========================================

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

window.toggleEditMode = function(showEdit) {
    const view = document.getElementById('profileView');
    const edit = document.getElementById('profileEdit');

    if (view && edit) {
        if (showEdit) {
            view.style.display = 'none';
            edit.style.display = 'block';
            const nicknameMessage = document.getElementById('nicknameMessage');
            if(nicknameMessage) nicknameMessage.textContent = "";
        } else {
            view.style.display = 'block';
            edit.style.display = 'none';
        }
    }
};

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