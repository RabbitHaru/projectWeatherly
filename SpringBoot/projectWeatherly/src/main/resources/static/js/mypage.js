// ===== 마이페이지 기능 구현 =====

document.addEventListener('DOMContentLoaded', function() {
    // DOM 요소 가져오기
    const editProfileBtn = document.getElementById('editProfileBtn');
    const profileView = document.getElementById('profileView');
    const profileEditForm = document.getElementById('profileEditForm');
    const cancelEditBtn = document.getElementById('cancelEditBtn');
    const changeImageBtn = document.getElementById('changeImageBtn');
    const profileImageInput = document.getElementById('profileImageInput');
    const editProfilePreview = document.getElementById('editProfilePreview');
    const removeImageBtn = document.getElementById('removeImageBtn');
    const changePasswordBtn = document.getElementById('changePasswordBtn');
    const passwordModal = document.getElementById('passwordModal');
    const closePasswordModal = document.getElementById('closePasswordModal');
    const cancelPasswordChange = document.getElementById('cancelPasswordChange');
    const passwordChangeForm = document.getElementById('passwordChangeForm');
    const nicknameCheckBtn = document.getElementById('nicknameCheckBtn');
    const loadMorePostsBtn = document.getElementById('loadMorePostsBtn');

    // 프로필 수정 모드 전환
    if (editProfileBtn && profileView && profileEditForm) {
        editProfileBtn.addEventListener('click', function() {
            profileView.style.display = 'none';
            profileEditForm.style.display = 'block';
            editProfileBtn.style.display = 'none';
        });
    }

    // 프로필 수정 취소
    if (cancelEditBtn) {
        cancelEditBtn.addEventListener('click', function() {
            profileView.style.display = 'block';
            profileEditForm.style.display = 'none';
            editProfileBtn.style.display = 'flex';
        });
    }

    // 프로필 이미지 변경
    if (changeImageBtn && profileImageInput) {
        changeImageBtn.addEventListener('click', function() {
            profileImageInput.click();
        });

        profileImageInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                if (file.size > 5 * 1024 * 1024) {
                    alert('파일 크기는 5MB를 초과할 수 없습니다.');
                    return;
                }

                const reader = new FileReader();
                reader.onload = function(e) {
                    editProfilePreview.src = e.target.result;

                    // 프로필 보기 이미지도 업데이트
                    const profileImage = document.getElementById('profileImage');
                    if (profileImage) {
                        profileImage.src = e.target.result;
                    }
                };
                reader.readAsDataURL(file);
            }
        });
    }

    // 이미지 제거
    if (removeImageBtn) {
        removeImageBtn.addEventListener('click', function() {
            const defaultImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='150' viewBox='0 0 150 150'%3E%3Ccircle cx='75' cy='60' r='30' fill='%233498db'/%3E%3Cpath d='M45,120 Q75,90 105,120' fill='none' stroke='%233498db' stroke-width='8'/%3E%3C/svg%3E";
            editProfilePreview.src = defaultImage;

            // 프로필 보기 이미지도 기본 이미지로 변경
            const profileImage = document.getElementById('profileImage');
            if (profileImage) {
                profileImage.src = defaultImage;
            }

            // 파일 입력 초기화
            if (profileImageInput) {
                profileImageInput.value = '';
            }
        });
    }

    // 비밀번호 변경 모달 열기
    if (changePasswordBtn && passwordModal) {
        changePasswordBtn.addEventListener('click', function() {
            passwordModal.style.display = 'flex';
        });
    }

    // 비밀번호 변경 모달 닫기
    if (closePasswordModal) {
        closePasswordModal.addEventListener('click', function() {
            passwordModal.style.display = 'none';
            resetPasswordForm();
        });
    }

    if (cancelPasswordChange) {
        cancelPasswordChange.addEventListener('click', function() {
            passwordModal.style.display = 'none';
            resetPasswordForm();
        });
    }

    // 모달 외부 클릭 시 닫기
    if (passwordModal) {
        passwordModal.addEventListener('click', function(e) {
            if (e.target === passwordModal) {
                passwordModal.style.display = 'none';
                resetPasswordForm();
            }
        });
    }

    // 비밀번호 변경 폼 초기화
    function resetPasswordForm() {
        if (passwordChangeForm) {
            passwordChangeForm.reset();

            // 검증 메시지 초기화
            const messages = passwordChangeForm.querySelectorAll('.validation-message');
            messages.forEach(message => {
                message.textContent = '';
                message.className = 'validation-message';
            });
        }
    }

    // 닉네임 중복 확인
    if (nicknameCheckBtn) {
        nicknameCheckBtn.addEventListener('click', function() {
            const nicknameInput = document.getElementById('editNickname');
            const nicknameMessage = document.getElementById('nicknameMessage');

            if (!nicknameInput || !nicknameMessage) return;

            const nickname = nicknameInput.value.trim();

            if (!nickname) {
                nicknameMessage.textContent = '닉네임을 입력해주세요.';
                nicknameMessage.className = 'validation-message error';
                return;
            }

            if (nickname.length < 2 || nickname.length > 10) {
                nicknameMessage.textContent = '닉네임은 2~10자로 입력해주세요.';
                nicknameMessage.className = 'validation-message error';
                return;
            }

            // 중복 확인 API 호출 (가짜 구현)
            nicknameCheckBtn.disabled = true;
            nicknameCheckBtn.textContent = '확인중...';
            nicknameMessage.textContent = '닉네임을 확인하고 있습니다...';
            nicknameMessage.className = 'validation-message info';

            // 실제로는 서버 API 호출
            setTimeout(() => {
                // 예시: 항상 사용 가능하다고 가정
                nicknameMessage.textContent = '사용 가능한 닉네임입니다.';
                nicknameMessage.className = 'validation-message success';
                nicknameCheckBtn.disabled = false;
                nicknameCheckBtn.textContent = '중복확인';
            }, 1000);
        });
    }

    // 비밀번호 변경 폼 제출
    if (passwordChangeForm) {
        passwordChangeForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const currentPassword = document.getElementById('currentPassword');
            const newPassword = document.getElementById('newPassword');
            const confirmPassword = document.getElementById('confirmPassword');
            const currentPasswordMessage = document.getElementById('currentPasswordMessage');
            const newPasswordMessage = document.getElementById('newPasswordMessage');
            const confirmPasswordMessage = document.getElementById('confirmPasswordMessage');

            let isValid = true;

            // 현재 비밀번호 검증
            if (!currentPassword.value.trim()) {
                currentPasswordMessage.textContent = '현재 비밀번호를 입력해주세요.';
                currentPasswordMessage.className = 'validation-message error';
                isValid = false;
            } else {
                currentPasswordMessage.textContent = '';
            }

            // 새 비밀번호 검증
            const newPasswordValue = newPassword.value.trim();
            if (!newPasswordValue) {
                newPasswordMessage.textContent = '새 비밀번호를 입력해주세요.';
                newPasswordMessage.className = 'validation-message error';
                isValid = false;
            } else if (newPasswordValue.length < 8) {
                newPasswordMessage.textContent = '비밀번호는 8자 이상이어야 합니다.';
                newPasswordMessage.className = 'validation-message error';
                isValid = false;
            } else if (!/(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])/.test(newPasswordValue)) {
                newPasswordMessage.textContent = '영문, 숫자, 특수문자를 모두 포함해야 합니다.';
                newPasswordMessage.className = 'validation-message error';
                isValid = false;
            } else {
                newPasswordMessage.textContent = '';
            }

            // 비밀번호 확인 검증
            if (!confirmPassword.value.trim()) {
                confirmPasswordMessage.textContent = '비밀번호 확인을 입력해주세요.';
                confirmPasswordMessage.className = 'validation-message error';
                isValid = false;
            } else if (newPasswordValue !== confirmPassword.value.trim()) {
                confirmPasswordMessage.textContent = '비밀번호가 일치하지 않습니다.';
                confirmPasswordMessage.className = 'validation-message error';
                isValid = false;
            } else {
                confirmPasswordMessage.textContent = '';
            }

            if (isValid) {
                // 실제로는 서버 API 호출
                console.log('비밀번호 변경 요청:', {
                    currentPassword: currentPassword.value,
                    newPassword: newPasswordValue
                });

                // 성공 메시지 표시
                alert('비밀번호가 성공적으로 변경되었습니다.');
                passwordModal.style.display = 'none';
                resetPasswordForm();
            }
        });
    }

    // 프로필 수정 폼 제출
    if (profileEditForm) {
        profileEditForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const nicknameInput = document.getElementById('editNickname');
            const nicknameMessage = document.getElementById('nicknameMessage');

            if (!nicknameInput.value.trim()) {
                nicknameMessage.textContent = '닉네임을 입력해주세요.';
                nicknameMessage.className = 'validation-message error';
                nicknameInput.focus();
                return;
            }

            // 실제로는 서버 API 호출
            console.log('프로필 수정 요청:', {
                nickname: nicknameInput.value,
                profileImage: profileImageInput.files[0]
            });

            // 성공 시 UI 업데이트
            const nicknameValue = document.getElementById('nicknameValue');
            if (nicknameValue) {
                nicknameValue.textContent = nicknameInput.value;
            }

            alert('프로필이 성공적으로 수정되었습니다.');

            // 수정 모드 종료
            profileView.style.display = 'block';
            profileEditForm.style.display = 'none';
            editProfileBtn.style.display = 'flex';
        });
    }

    // 게시물 삭제
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const listItem = this.closest('.list-item');
            const itemTitle = listItem.querySelector('.item-title').textContent;

            if (confirm(`"${itemTitle}" 게시물을 삭제하시겠습니까?\n삭제된 게시물은 복구할 수 없습니다.`)) {
                // 실제로는 서버 API 호출
                console.log('게시물 삭제:', itemTitle);

                // UI에서 제거
                listItem.remove();

                // 카운트 업데이트
                updatePostCount();

                alert('게시물이 삭제되었습니다.');
            }
        });
    });

    // 게시물 수정
    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const listItem = this.closest('.list-item');
            const itemTitle = listItem.querySelector('.item-title').textContent;

            // 실제로는 해당 게시물의 수정 페이지로 이동
            console.log('게시물 수정:', itemTitle);
            alert('게시물 수정 기능은 추후 구현 예정입니다.');
        });
    });

    // 더보기 버튼
    if (loadMorePostsBtn) {
        loadMorePostsBtn.addEventListener('click', function() {
            // 실제로는 서버 API 호출하여 추가 게시물 로드
            console.log('더 많은 게시물 로드');

            // 예시: 더미 게시물 추가
            const postedList = document.getElementById('postedList');
            if (postedList) {
                const newPost = document.createElement('div');
                newPost.className = 'list-item';
                newPost.innerHTML = `
                    <div class="list-item-header">
                        <span class="item-title">새로 로드된 게시물</span>
                        <span class="item-category-tag general">자유주제</span>
                    </div>
                    <div class="item-content">
                        <p>더보기 버튼으로 로드된 게시물입니다.</p>
                    </div>
                    <div class="item-meta">
                        <span class="item-date">2026.01.18 12:00</span>
                        <span class="item-stats">
                            <i class="fas fa-heart"></i> 0
                            <i class="fas fa-comment"></i> 0
                            <i class="fas fa-eye"></i> 1
                        </span>
                    </div>
                    <div class="item-actions">
                        <button class="action-btn edit-btn"><i class="fas fa-edit"></i> 수정</button>
                        <button class="action-btn delete-btn"><i class="fas fa-trash"></i> 삭제</button>
                    </div>
                `;
                postedList.appendChild(newPost);

                // 새로 추가된 버튼에 이벤트 리스너 연결
                newPost.querySelector('.delete-btn').addEventListener('click', function() {
                    const listItem = this.closest('.list-item');
                    const itemTitle = listItem.querySelector('.item-title').textContent;

                    if (confirm(`"${itemTitle}" 게시물을 삭제하시겠습니까?`)) {
                        listItem.remove();
                        updatePostCount();
                    }
                });

                newPost.querySelector('.edit-btn').addEventListener('click', function() {
                    alert('게시물 수정 기능은 추후 구현 예정입니다.');
                });

                updatePostCount();
            }

            // 더보기 버튼 비활성화 (실제 구현에서는 페이지네이션 처리)
            loadMorePostsBtn.disabled = true;
            loadMorePostsBtn.textContent = '더 이상 게시물이 없습니다';
        });
    }

    // 게시물 카운트 업데이트
    function updatePostCount() {
        const postedList = document.getElementById('postedList');
        const postedCount = document.getElementById('postedCount');

        if (postedList && postedCount) {
            const count = postedList.querySelectorAll('.list-item').length;
            postedCount.textContent = count;

            // 빈 리스트 체크
            const emptyList = document.getElementById('emptyReportedList');
            const reportedList = document.getElementById('reportedList');

            if (reportedList && emptyList) {
                const reportedCount = reportedList.querySelectorAll('.list-item').length;
                if (reportedCount === 0) {
                    reportedList.style.display = 'none';
                    emptyList.style.display = 'flex';
                } else {
                    reportedList.style.display = 'flex';
                    emptyList.style.display = 'none';
                }
            }
        }
    }

    // 초기 카운트 업데이트
    updatePostCount();

    // 신고된 게시물 삭제 (관리자 기능)
    document.querySelectorAll('.list-item').forEach(item => {
        const reportedStatus = item.querySelector('.item-status.reported');
        if (reportedStatus) {
            // 신고 처리중인 게시물에 삭제 버튼 추가
            const actions = item.querySelector('.item-actions');
            if (actions) {
                const cancelReportBtn = document.createElement('button');
                cancelReportBtn.className = 'action-btn cancel-btn';
                cancelReportBtn.innerHTML = '<i class="fas fa-times"></i> 신고 취소';
                cancelReportBtn.addEventListener('click', function() {
                    const itemTitle = item.querySelector('.item-title').textContent;
                    if (confirm(`"${itemTitle}" 신고를 취소하시겠습니까?`)) {
                        // 실제로는 서버 API 호출
                        console.log('신고 취소:', itemTitle);
                        item.remove();
                        updatePostCount();
                        alert('신고가 취소되었습니다.');
                    }
                });
                actions.appendChild(cancelReportBtn);
            }
        }
    });

    // 다크모드 초기화
    initializeDarkMode();
});

// 다크모드 초기화 함수
function initializeDarkMode() {
    const darkModeToggle = document.getElementById('darkModeToggle');
    if (!darkModeToggle) return;

    const isDarkMode = localStorage.getItem('darkMode') === 'true';

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

    darkModeToggle.addEventListener('click', function() {
        const isDarkMode = document.body.classList.toggle('dark-mode');

        localStorage.setItem('darkMode', isDarkMode.toString());

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