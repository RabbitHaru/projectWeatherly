document.addEventListener('DOMContentLoaded', function() {

    // ==========================================
    // 1. 다크모드 로직 (login.js와 동일)
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
    // 2. 커뮤니티 기능 (기존 기능 유지)
    // ==========================================

    // 검색 기능 (엔터키로 검색)
    const searchInput = document.querySelector('.community-search');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault(); // 폼 중복 제출 방지
                this.closest('form').submit();
            }
        });
    }

    // 게시글 클릭 시 상세페이지로 이동
    document.querySelectorAll('.post-item').forEach(item => {
        item.addEventListener('click', function(e) {
            // 링크나 버튼 클릭 시에는 이동하지 않음
            if (!e.target.closest('a') && !e.target.closest('button')) {
                const link = this.querySelector('.post-title a');
                if (link) {
                    window.location.href = link.href;
                }
            }
        });
    });

    // 모바일 터치 커서 스타일
    if (window.innerWidth <= 768) {
        document.querySelectorAll('.post-item').forEach(item => {
            item.style.cursor = 'pointer';
        });
    }

    // 드롭다운 메뉴 토글
    const userMenuBtn = document.querySelector('.user-menu-btn');
    if (userMenuBtn) {
        userMenuBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            const dropdown = this.nextElementSibling;
            dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
        });
    }

    // 문서 클릭 시 드롭다운 닫기
    document.addEventListener('click', function() {
        document.querySelectorAll('.dropdown-content').forEach(dropdown => {
            dropdown.style.display = 'none';
        });
    });

    // 알림 및 즐겨찾기 (준비 중 메시지)
    const notiBtn = document.querySelector('.notification-btn');
    if (notiBtn) notiBtn.addEventListener('click', () => alert('알림 기능은 준비 중입니다.'));

    const favBtn = document.querySelector('.favorites-btn');
    if (favBtn) favBtn.addEventListener('click', () => alert('즐겨찾기 기능은 준비 중입니다.'));

    // 글쓰기 버튼 비로그인 처리
    const writeBtns = document.querySelectorAll('.write-btn');
    writeBtns.forEach(btn => {
        // href가 없거나 '#'인 경우만 처리 (로그인 상태면 href가 있으므로 동작 안함)
        if (!btn.getAttribute('href') || btn.getAttribute('href') === '#') {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                if (confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) {
                    window.location.href = '/login?redirect=/community/boards/write';
                }
            });
        }
    });

    // 페이지네이션 비활성화 링크 처리
    document.querySelectorAll('.page-link').forEach(link => {
        link.addEventListener('click', function(e) {
            if (this.parentElement.classList.contains('disabled')) {
                e.preventDefault();
            }
        });
    });
});