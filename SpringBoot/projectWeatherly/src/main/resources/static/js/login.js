document.addEventListener('DOMContentLoaded', function() {

    // [1] 서버 메시지 처리 (HTML에서 th:text로 처리하므로 alert는 제거하거나 성공 메시지만 남김)
    // 에러 메시지는 login.html의 #loginErrorMsg div가 자동으로 보여줌

    const successDiv = document.getElementById('serverSuccessMessage');
    if (successDiv) {
        const successMsg = successDiv.textContent.trim();
        if (successMsg) {
            alert(successMsg); // 성공 메시지(로그아웃 등)는 팝업으로 유지
        }
    }

    // [2] 비밀번호 입력 감지
    const passwordInput = document.getElementById('user_password');
    const passwordMessage = document.getElementById('passwordMessage');

    if (passwordInput && passwordMessage) {
        passwordInput.addEventListener('input', function() {
            const password = passwordInput.value;
            if (password.length === 0) {
                passwordMessage.textContent = '';
                passwordMessage.className = 'validation-message';
            }
        });
    }

    // [3] 로그인 폼 제출 전 검증 (수정됨: Alert 대신 박스에 표시)
    const loginForm = document.getElementById('loginForm');
    const errorBox = document.getElementById('loginErrorMsg');
    const errorText = errorBox ? errorBox.querySelector('span') : null;

    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            const userIdInput = document.getElementById('user_id');
            const userPwInput = document.getElementById('user_password');

            let msg = '';

            if (!userIdInput.value.trim()) {
                msg = '이메일을 입력해주세요.';
                userIdInput.focus();
            } else if (!userPwInput.value.trim()) {
                msg = '비밀번호를 입력해주세요.';
                userPwInput.focus();
            }

            if (msg) {
                e.preventDefault(); // 제출 막기
                if (errorBox && errorText) {
                    errorText.textContent = msg;
                    errorBox.classList.add('show'); // 빨간 박스 보이기
                } else {
                    alert(msg); // 박스가 없으면 기존대로 alert
                }
            }
        });
    }

    // [4] 다크모드 (공통)
    const darkModeToggle = document.getElementById('darkModeToggle');
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

        if (localStorage.getItem('darkMode') === 'enabled') {
            document.body.classList.add('dark-mode');
            const icon = darkModeToggle.querySelector('i');
            if(icon) {
                icon.classList.remove('fa-moon');
                icon.classList.add('fa-sun');
            }
        }
    }
});