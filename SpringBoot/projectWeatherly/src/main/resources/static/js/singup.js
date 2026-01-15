// 회원가입 페이지 기능
document.addEventListener('DOMContentLoaded', function() {
    // 이메일 중복 확인
    const emailCheckBtn = document.getElementById('emailCheckBtn');
    const emailMessage = document.getElementById('emailMessage');

    emailCheckBtn.addEventListener('click', function() {
        const email = document.getElementById('user_email').value;

        if (!email) {
            emailMessage.textContent = '이메일을 입력해주세요.';
            emailMessage.className = 'validation-message error';
            return;
        }

        // 이메일 형식 검증
        const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!emailPattern.test(email)) {
            emailMessage.textContent = '올바른 이메일 형식이 아닙니다.';
            emailMessage.className = 'validation-message error';
            return;
        }

        // 여기서는 실제 중복 확인 API 호출 대신 시뮬레이션
        emailMessage.textContent = '이메일 중복 확인 중...';
        emailMessage.className = 'validation-message info';

        setTimeout(() => {
            // 시뮬레이션: 랜덤으로 성공/실패
            const isAvailable = Math.random() > 0.5;

            if (isAvailable) {
                emailMessage.textContent = '사용 가능한 이메일입니다.';
                emailMessage.className = 'validation-message success';
                emailCheckBtn.disabled = true;
                emailCheckBtn.textContent = '확인완료';
                emailCheckBtn.style.background = '#ccc';
            } else {
                emailMessage.textContent = '이미 사용 중인 이메일입니다.';
                emailMessage.className = 'validation-message error';
            }
        }, 1000);
    });

    // 닉네임 중복 확인
    const nameCheckBtn = document.getElementById('nameCheckBtn');
    const nameMessage = document.getElementById('nameMessage');

    nameCheckBtn.addEventListener('click', function() {
        const nickname = document.getElementById('user_name').value;

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

        // 닉네임 형식 검증 (한글, 영문, 숫자만)
        const nicknamePattern = /^[가-힣a-zA-Z0-9]+$/;
        if (!nicknamePattern.test(nickname)) {
            nameMessage.textContent = '닉네임은 한글, 영문, 숫자만 사용 가능합니다.';
            nameMessage.className = 'validation-message error';
            return;
        }

        nameMessage.textContent = '닉네임 중복 확인 중...';
        nameMessage.className = 'validation-message info';

        setTimeout(() => {
            // 시뮬레이션
            const isAvailable = Math.random() > 0.5;

            if (isAvailable) {
                nameMessage.textContent = '사용 가능한 닉네임입니다.';
                nameMessage.className = 'validation-message success';
                nameCheckBtn.disabled = true;
                nameCheckBtn.textContent = '확인완료';
                nameCheckBtn.style.background = '#ccc';
            } else {
                nameMessage.textContent = '이미 사용 중인 닉네임입니다.';
                nameMessage.className = 'validation-message error';
            }
        }, 1000);
    });

    // 비밀번호 유효성 검사
    const passwordInput = document.getElementById('user_password');
    const passwordMessage = document.getElementById('passwordMessage');

    passwordInput.addEventListener('input', function() {
        const password = passwordInput.value;

        if (password.length === 0) {
            passwordMessage.textContent = '';
            passwordMessage.className = 'validation-message';
            return;
        }

        // 비밀번호 강도 검증
        let strength = 0;
        let message = '';

        if (password.length >= 8) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[a-z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[^A-Za-z0-9]/.test(password)) strength++;

        if (strength <= 2) {
            message = '비밀번호가 너무 약합니다. 영문 대소문자, 숫자, 특수문자를 포함해주세요.';
            passwordMessage.className = 'validation-message error';
        } else if (strength <= 4) {
            message = '비밀번호 보안 수준: 보통';
            passwordMessage.className = 'validation-message info';
        } else {
            message = '안전한 비밀번호입니다.';
            passwordMessage.className = 'validation-message success';
        }

        passwordMessage.textContent = message;
    });

    // 비밀번호 확인
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

    // 프로필 이미지 업로드
    const profileImageInput = document.getElementById('profile_image');
    const profilePreview = document.getElementById('profilePreview');
    const removeImageBtn = document.getElementById('removeImageBtn');

    profileImageInput.addEventListener('change', function(e) {
        const file = e.target.files[0];

        if (file) {
            // 파일 크기 제한 (5MB)
            if (file.size > 5 * 1024 * 1024) {
                alert('파일 크기는 5MB 이하여야 합니다.');
                profileImageInput.value = '';
                return;
            }

            // 이미지 파일인지 확인
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

    // 약관 보기 버튼
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

    // 전체 동의안함 체크박스
    // const boardNotificationCheckbox = document.getElementById('board_notification_agree');
    // const weatherAlertAgreeCheckbox = document.getElementById('weather_alert_agree');
    //
    // boardNotificationCheckbox.addEventListener('change', function() {
    //     if (this.checked) {
    //         boardNotificationCheckbox.checked = false;
    //     }
    // });
    //
    // // 선택 동의 체크박스가 변경될 때
    // weatherAlertAgreeCheckbox.addEventListener('change', function() {
    //     if (this.checked) {
    //         weatherAlertAgreeCheckbox.checked = false;
    //     }
    // });

    // 폼 제출
    const signupForm = document.getElementById('signupForm');

    signupForm.addEventListener('submit', function(e) {
        e.preventDefault();

        // 필수 동의 확인
        const agreeTerms = document.getElementById('terms_of_service_agree').checked;
        const agreePrivacy = document.getElementById('privacy_policy_agree').checked;

        if (!agreeTerms || !agreePrivacy) {
            alert('필수 약관에 동의해주세요.');
            return;
        }

        // 비밀번호 확인
        const password = passwordInput.value;
        const confirmPassword = passwordConfirmInput.value;

        if (password !== confirmPassword) {
            alert('비밀번호가 일치하지 않습니다.');
            passwordConfirmInput.focus();
            return;
        }

        // 이메일 중복 확인 여부 확인 (실제 구현에서는 서버에서 확인)
        // 닉네임 중복 확인 여부 확인

        // 폼 데이터 수집
        const formData = new FormData(signupForm);

        // 프로필 이미지 파일 추가
        const profileFile = profileImageInput.files[0];
        if (profileFile) {
            formData.append('profile_image_file', profileFile);
        }

        // 추가 필드 추가
        formData.append('user_role', 'USER');
        formData.append('auth_provider', 'local');
        formData.append('is_active', 'true');

        // 회원가입 처리 시뮬레이션
        const submitBtn = document.querySelector('.submit-btn');
        const originalText = submitBtn.innerHTML;

        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';
        submitBtn.disabled = true;

        // 실제 구현에서는 fetch API를 사용하여 서버로 데이터 전송
        setTimeout(() => {
            // 성공 시뮬레이션
            alert('회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.');
            window.location.href = '/login';
        }, 2000);

        // 실제 구현 시 아래 코드를 사용

        axios.post('/auth/signup', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
            .then(response => {
                const data = response.data;
                if (data.success) {
                    alert('회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.');
                    window.location.href = '/login';
                } else {
                    alert(data.message || '회원가입에 실패했습니다.');
                    submitBtn.innerHTML = originalText;
                    submitBtn.disabled = false;
                }
            })
            .catch(error => {
                console.error('Error:', error);

                // 에러 응답 구조에 따라 다르게 처리
                if (error.response) {
                    // 서버가 2xx 외의 상태 코드로 응답한 경우
                    console.error('Response status:', error.response.status);
                    console.error('Response data:', error.response.data);

                    // 서버에서 제공한 에러 메시지 사용
                    const errorMessage = error.response.data?.message ||
                        error.response.data?.error ||
                        '서버 오류가 발생했습니다.';
                    alert(errorMessage);
                } else if (error.request) {
                    // 요청이 전송되었지만 응답이 없는 경우
                    console.error('No response received:', error.request);
                    alert('서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.');
                } else {
                    // 요청 설정 중에 에러가 발생한 경우
                    console.error('Request setup error:', error.message);
                    alert('요청을 처리하는 중 오류가 발생했습니다.');
                }

                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
            });

    });

    // 입력 필드 자동 저장 (선택사항)
    const inputsToSave = ['user_email', 'user_name'];

    inputsToSave.forEach(id => {
        const input = document.getElementById(id);

        // 페이지 로드 시 저장된 값 복원
        const savedValue = localStorage.getItem(`signup_${id}`);
        if (savedValue) {
            input.value = savedValue;
        }

        // 입력 시 값 저장
        input.addEventListener('input', function() {
            localStorage.setItem(`signup_${id}`, this.value);
        });
    });

    // 페이지 떠날 때 저장된 데이터 정리 (선택사항)
    window.addEventListener('beforeunload', function() {
        if (window.location.pathname.includes('signup')) {
            // 회원가입 페이지를 떠날 때만 데이터 유지
            return;
        }

        // 다른 페이지로 이동 시 저장된 데이터 삭제
        inputsToSave.forEach(id => {
            localStorage.removeItem(`signup_${id}`);
        });
    });
});