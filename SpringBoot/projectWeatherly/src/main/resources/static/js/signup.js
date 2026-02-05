document.addEventListener('DOMContentLoaded', function() {

    // [상태 관리] 중복 확인 완료 여부를 저장하는 변수
    let isEmailChecked = false;
    let isNicknameChecked = false;

    // ==========================================
    // 1. 이메일 중복 확인 기능 (Real Server Check)
    // ==========================================
    const emailCheckBtn = document.getElementById('emailCheckBtn');
    const emailMessage = document.getElementById('emailMessage');
    const emailInput = document.getElementById('user_email');

    // 사용자가 입력값을 변경하면 중복 확인 상태 초기화
    emailInput.addEventListener('input', function() {
        isEmailChecked = false;
        emailCheckBtn.disabled = false;
        emailCheckBtn.style.background = ''; // 원래 색으로 복구
        emailCheckBtn.textContent = '중복확인';
        emailMessage.textContent = '';
        emailMessage.className = 'validation-message';
    });

    emailCheckBtn.addEventListener('click', function() {
        const email = emailInput.value;

        if (!email) {
            emailMessage.textContent = '이메일을 입력해주세요.';
            emailMessage.className = 'validation-message error';
            return;
        }

        // 이메일 정규식 검사
        const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!emailPattern.test(email)) {
            emailMessage.textContent = '올바른 이메일 형식이 아닙니다.';
            emailMessage.className = 'validation-message error';
            return;
        }

        // 서버 통신 시작 메시지
        emailMessage.textContent = '이메일 중복 확인 중...';
        emailMessage.className = 'validation-message info';

        // Axios를 이용한 비동기 요청
        axios.get('/auth/api/check-email', {
            params: { email: email }
        })
            .then(function(response) {
                const isDuplicate = response.data; // true: 중복, false: 사용 가능

                if (isDuplicate) {
                    emailMessage.textContent = '이미 사용 중인 이메일입니다.';
                    emailMessage.className = 'validation-message error';
                    isEmailChecked = false;
                } else {
                    emailMessage.textContent = '사용 가능한 이메일입니다.';
                    emailMessage.className = 'validation-message success';

                    // 확인 완료 처리
                    isEmailChecked = true;
                    emailCheckBtn.disabled = true;
                    emailCheckBtn.textContent = '확인완료';
                    emailCheckBtn.style.background = '#ccc';
                }
            })
            .catch(function(error) {
                console.error('Email check error:', error);
                emailMessage.textContent = '서버 연결 실패. 다시 시도해주세요.';
                emailMessage.className = 'validation-message error';
                isEmailChecked = false;
            });
    });

    // ==========================================
    // 2. 닉네임 중복 확인 기능 (Real Server Check)
    // ==========================================
    const nameCheckBtn = document.getElementById('nameCheckBtn');
    const nameMessage = document.getElementById('nameMessage');
    const nameInput = document.getElementById('user_name');

    // 사용자가 입력값을 변경하면 중복 확인 상태 초기화
    nameInput.addEventListener('input', function() {
        isNicknameChecked = false;
        nameCheckBtn.disabled = false;
        nameCheckBtn.style.background = '';
        nameCheckBtn.textContent = '중복확인';
        nameMessage.textContent = '';
        nameMessage.className = 'validation-message';
    });

    nameCheckBtn.addEventListener('click', function() {
        const nickname = nameInput.value;

        if (!nickname) {
            nameMessage.textContent = '닉네임을 입력해주세요.';
            nameMessage.className = 'validation-message error';
            return;
        }

        if (nickname.length < 2 || nickname.length > 10) {
            nameMessage.textContent = '닉네임은 2~10자 사이로 입력해주세요.';
            nameMessage.className = 'validation-message error';
            return;
        }

        const nicknamePattern = /^[가-힣a-zA-Z0-9]+$/;
        if (!nicknamePattern.test(nickname)) {
            nameMessage.textContent = '닉네임은 한글, 영문, 숫자만 사용 가능합니다.';
            nameMessage.className = 'validation-message error';
            return;
        }

        nameMessage.textContent = '닉네임 중복 확인 중...';
        nameMessage.className = 'validation-message info';

        // Axios 요청
        axios.get('/auth/api/check-nickname', {
            params: { nickname: nickname }
        })
            .then(function(response) {
                const isDuplicate = response.data; // true: 중복

                if (isDuplicate) {
                    nameMessage.textContent = '이미 사용 중인 닉네임입니다.';
                    nameMessage.className = 'validation-message error';
                    isNicknameChecked = false;
                } else {
                    nameMessage.textContent = '사용 가능한 닉네임입니다.';
                    nameMessage.className = 'validation-message success';

                    // 확인 완료 처리
                    isNicknameChecked = true;
                    nameCheckBtn.disabled = true;
                    nameCheckBtn.textContent = '확인완료';
                    nameCheckBtn.style.background = '#ccc';
                }
            })
            .catch(function(error) {
                console.error('Nickname check error:', error);
                nameMessage.textContent = '서버 연결 실패. 다시 시도해주세요.';
                nameMessage.className = 'validation-message error';
                isNicknameChecked = false;
            });
    });

    // ==========================================
    // 3. 비밀번호 유효성 검사 (기존 유지)
    // ==========================================
    const passwordInput = document.getElementById('user_password');
    const passwordMessage = document.getElementById('passwordMessage');

    passwordInput.addEventListener('input', function() {
        const password = passwordInput.value;

        if (password.length === 0) {
            passwordMessage.textContent = '';
            passwordMessage.className = 'validation-message';
            return;
        }

        let strength = 0;
        let message = '';

        if (password.length >= 8) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[a-z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[^A-Za-z0-9]/.test(password)) strength++;

        if (strength <= 2) {
            message = '비밀번호가 너무 약합니다. 영문 대소문자, 숫자, 특수문자를 포함시키는 걸 권장합니다.';
            passwordMessage.className = 'validation-message warning';
        } else if (strength <= 4) {
            message = '비밀번호 보안 수준: 보통';
            passwordMessage.className = 'validation-message info';
        } else {
            message = '안전한 비밀번호입니다.';
            passwordMessage.className = 'validation-message success';
        }

        passwordMessage.textContent = message;
    });

    // ==========================================
    // 4. 비밀번호 확인 (기존 유지)
    // ==========================================
    const passwordConfirmInput = document.getElementById('password_confirm');
    const passwordConfirmMessage = document.getElementById('passwordConfirmMessage');

    passwordConfirmInput.addEventListener('input', function() {
        const password = passwordInput.value;
        const confirmPassword = passwordConfirmInput.value;

        if (confirmPassword.length === 0) {
            passwordConfirmMessage.textContent = '';
            passwordConfirmMessage.className = 'validation-message';
            return;
        }

        if (password === confirmPassword) {
            passwordConfirmMessage.textContent = '비밀번호가 일치합니다.';
            passwordConfirmMessage.className = 'validation-message success';
        } else {
            passwordConfirmMessage.textContent = '비밀번호가 일치하지 않습니다.';
            passwordConfirmMessage.className = 'validation-message error';
        }
    });

    // ==========================================
    // 5. 프로필 이미지 업로드 (기존 유지)
    // ==========================================
    const profileImageInput = document.getElementById('profile_image');
    const profilePreview = document.getElementById('profilePreview');
    const removeImageBtn = document.getElementById('removeImageBtn');

    profileImageInput.addEventListener('change', function(e) {
        const file = e.target.files[0];

        if (file) {
            if (file.size > 5 * 1024 * 1024) {
                alert('파일 크기는 5MB 이하여야 합니다.');
                profileImageInput.value = '';
                return;
            }

            if (!file.type.match('image.*')) {
                alert('이미지 파일만 업로드 가능합니다.');
                profileImageInput.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = function(event) {
                profilePreview.src = event.target.result;
            };
            reader.readAsDataURL(file);
        }
    });

    removeImageBtn.addEventListener('click', function() {
        profilePreview.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='40' r='20' fill='%233498db'/%3E%3Cpath d='M30,85 Q50,65 70,85' fill='none' stroke='%233498db' stroke-width='5'/%3E%3C/svg%3E";
        profileImageInput.value = '';
    });

    // ==========================================
    // 6. 약관 보기 (기존 유지)
    // ==========================================
    const viewAgreementBtns = document.querySelectorAll('.view-agreement-btn');

    viewAgreementBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const targetId = this.getAttribute('data-target');
            const content = document.getElementById(targetId);

            content.classList.toggle('active');

            if (content.classList.contains('active')) {
                this.textContent = '접기';
            } else {
                this.textContent = '보기';
            }
        });
    });

    // ==========================================
    // 7. 폼 제출 (유효성 검사 강화 + 약관 필수 체크)
    // ==========================================
    const signupForm = document.getElementById('signupForm');

    signupForm.addEventListener('submit', function(e) {
        e.preventDefault(); // 일단 제출 막기

        // [A. 필수 약관 동의 확인 (메시지 기능 추가)]
        const agreeTerms = document.getElementById('terms_of_service_agree');
        const agreePrivacy = document.getElementById('privacy_policy_agree');

        // 둘 중 하나라도 체크가 안 되어 있다면
        if (!agreeTerms.checked || !agreePrivacy.checked) {
            // [요청하신 부분] 메시지 띄우기
            alert('이용약관과 개인정보 수집 및 이용에 동의해주세요.');

            // 체크 안 된 곳으로 포커스 이동 (사용자 편의)
            if (!agreeTerms.checked) {
                agreeTerms.focus();
            } else {
                agreePrivacy.focus();
            }
            return; // 여기서 함수를 끝내서 서버로 전송되지 않게 함!
        }

        // [B. 중복 확인 수행 여부 확인]
        if (!isEmailChecked) {
            alert('이메일 중복 확인을 해주세요.');
            document.getElementById('user_email').focus();
            return;
        }

        if (!isNicknameChecked) {
            alert('닉네임 중복 확인을 해주세요.');
            document.getElementById('user_name').focus();
            return;
        }

        // [C. 비밀번호 유효성 및 일치 확인]
        const password = passwordInput.value;
        const confirmPassword = passwordConfirmInput.value;

        // ★ [추가됨] 비밀번호 길이 검사 (8자 미만 차단)
        if (password.length < 8) {
            alert('비밀번호는 최소 8자 이상이어야 합니다.');
            passwordInput.focus();
            return; // 제출 중단
        }

        if (password !== confirmPassword) {
            alert('비밀번호가 일치하지 않습니다.');
            passwordConfirmInput.focus();
            return;
        }

        // [D. 제출 전처리]
        const roleInput = document.createElement('input');
        roleInput.type = 'hidden';
        roleInput.name = 'user_role';
        roleInput.value = 'USER';
        signupForm.appendChild(roleInput);

        // [E. 최종 제출]
        e.target.action = "/signup";
        e.target.submit();
    });

    // ==========================================
    // 8. 입력 필드 자동 저장/복원 (기존 유지)
    // ==========================================
    const inputsToSave = ['user_email', 'user_name'];

    inputsToSave.forEach(id => {
        const input = document.getElementById(id);
        const savedValue = localStorage.getItem(`signup_${id}`);
        if (savedValue) {
            input.value = savedValue;
        }
        input.addEventListener('input', function() {
            localStorage.setItem(`signup_${id}`, this.value);
        });
    });

    window.addEventListener('beforeunload', function() {
        if (window.location.pathname.includes('signup')) {
            return;
        }
        inputsToSave.forEach(id => {
            localStorage.removeItem(`signup_${id}`);
        });
    });

});