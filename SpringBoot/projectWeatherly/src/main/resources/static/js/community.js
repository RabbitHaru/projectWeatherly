// 페이지 로드 시 다크모드 상태 복원
document.addEventListener('DOMContentLoaded', function() {
    const darkMode = localStorage.getItem('darkMode');
    const toggleBtn = document.getElementById('darkmode-toggle');

    if (darkMode === 'enabled') {
        enableDarkMode();
    } else {
        disableDarkMode();
    }

    // 다크모드 토글 버튼 이벤트 리스너
    if (toggleBtn) {
        toggleBtn.addEventListener('click', toggleDarkMode);
    }
});

// 다크모드 활성화 함수
function enableDarkMode() {
    document.body.classList.add('dark-mode');
    const toggleBtn = document.getElementById('darkmode-toggle');
    if (toggleBtn) {
        toggleBtn.querySelector('i').className = 'fas fa-sun';
        toggleBtn.title = '라이트모드 전환';
    }
    localStorage.setItem('darkMode', 'enabled');

    // 모든 이미지에 어두운 효과 적용
    document.querySelectorAll('img').forEach(img => {
        if (!img.classList.contains('no-dark-filter')) {
            img.style.filter = 'brightness(0.7) contrast(1.1)';
        }
    });
}

// 다크모드 비활성화 함수
function disableDarkMode() {
    document.body.classList.remove('dark-mode');
    const toggleBtn = document.getElementById('darkmode-toggle');
    if (toggleBtn) {
        toggleBtn.querySelector('i').className = 'fas fa-moon';
        toggleBtn.title = '다크모드 전환';
    }
    localStorage.setItem('darkMode', 'disabled');

    // 이미지 효과 제거
    document.querySelectorAll('img').forEach(img => {
        img.style.filter = '';
    });
}

// 다크모드 토글 함수
function toggleDarkMode() {
    if (document.body.classList.contains('dark-mode')) {
        disableDarkMode();
    } else {
        enableDarkMode();
    }
}

// 모든 페이지에서 공통으로 사용할 다크모드 초기화 함수
window.initializeDarkMode = function() {
    const darkMode = localStorage.getItem('darkMode');
    const toggleBtn = document.getElementById('darkmode-toggle');

    if (darkMode === 'enabled') {
        document.body.classList.add('dark-mode');
        if (toggleBtn) {
            toggleBtn.querySelector('i').className = 'fas fa-sun';
            toggleBtn.title = '라이트모드 전환';
        }
    } else {
        document.body.classList.remove('dark-mode');
        if (toggleBtn) {
            toggleBtn.querySelector('i').className = 'fas fa-moon';
            toggleBtn.title = '다크모드 전환';
        }
    }

    // 토글 버튼 이벤트 리스너
    if (toggleBtn) {
        toggleBtn.removeEventListener('click', toggleDarkMode);
        toggleBtn.addEventListener('click', toggleDarkMode);
    }
};

// 현재 페이지에서도 초기화 실행
window.initializeDarkMode();

// 기존 이벤트 리스너들은 유지하면서 다크모드 토글만 제거
// 검색 기능 (엔터키로 검색)
document.querySelector('.community-search').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        this.form.submit();
    }
});

// 게시글 클릭 시 상세페이지로 이동
document.querySelectorAll('.post-item').forEach(item => {
    item.addEventListener('click', function(e) {
        if (!e.target.closest('a') && !e.target.closest('button')) {
            const link = this.querySelector('.post-title a');
            if (link) {
                window.location.href = link.href;
            }
        }
    });
});

// 모바일에서 더 나은 사용자 경험을 위한 처리
if (window.innerWidth <= 768) {
    document.querySelectorAll('.post-item').forEach(item => {
        item.style.cursor = 'pointer';
    });
}

// 드롭다운 메뉴 토글
document.querySelector('.user-menu-btn').addEventListener('click', function(e) {
    e.stopPropagation();
    const dropdown = this.nextElementSibling;
    dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
});

// 문서 클릭 시 드롭다운 닫기
document.addEventListener('click', function() {
    document.querySelectorAll('.dropdown-content').forEach(dropdown => {
        dropdown.style.display = 'none';
    });
});

// 알림 버튼 클릭 시
document.querySelector('.notification-btn').addEventListener('click', function() {
    alert('알림 기능은 준비 중입니다.');
});

// 즐겨찾기 버튼 클릭 시
document.querySelector('.favorites-btn').addEventListener('click', function() {
    alert('즐겨찾기 기능은 준비 중입니다.');
});

// 게시글 작성 버튼 (비로그인 시)
const writeBtn = document.querySelector('.write-btn');
if (writeBtn && !writeBtn.getAttribute('href')) {
    writeBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) {
            window.location.href = '/login?redirect=/community/boards/write';
        }
    });
}

// 페이지네이션 효과
document.querySelectorAll('.page-link').forEach(link => {
    link.addEventListener('click', function(e) {
        if (this.parentElement.classList.contains('disabled')) {
            e.preventDefault();
            return;
        }
    });
});

// 글쓰기 버튼 비로그인 시 처리
const writeBtns = document.querySelectorAll('.write-btn');
writeBtns.forEach(btn => {
    if (!btn.getAttribute('href')) {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            if (confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) {
                window.location.href = '/login?redirect=/community/boards/write';
            }
        });
    }
});

