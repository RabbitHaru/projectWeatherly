/**
 * common.js - Weatherly 공통 유틸리티
 */

const API_BASE_URL = window.location.origin;

document.addEventListener('DOMContentLoaded', function () {
    setupDarkMode();
    setupTabSwitching();
    updateCurrentTime();
});

function setupDarkMode() {
    const toggleBtn = document.getElementById('darkmode-toggle');
    const body = document.body;
    const isDarkMode = localStorage.getItem('darkMode') === 'true';

    if (isDarkMode) {
        body.classList.add('dark-mode');
        updateDarkModeIcon(true);
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const isCurrentlyDark = body.classList.contains('dark-mode');
            if (isCurrentlyDark) {
                body.classList.remove('dark-mode');
                localStorage.setItem('darkMode', 'false');
                updateDarkModeIcon(false);
            } else {
                body.classList.add('dark-mode');
                localStorage.setItem('darkMode', 'true');
                updateDarkModeIcon(true);
            }
        });
    }
}

function updateDarkModeIcon(isDarkMode) {
    const icon = document.querySelector('#darkmode-toggle i');
    const btn = document.getElementById('darkmode-toggle');
    if (icon && btn) {
        icon.className = isDarkMode ? 'fas fa-sun' : 'fas fa-moon';
        btn.title = isDarkMode ? '라이트모드로 전환' : '다크모드로 전환';
    }
}

function updateCurrentTime(targetIds = ['current-time', 'fine-dust-current-time']) {
    const now = new Date();
    const longOptions = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    };

    targetIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = now.toLocaleDateString('ko-KR', longOptions);
    });
}

function showLoading(msg = '로딩중...') {
    let el = document.getElementById('loading-overlay');
    if (!el) {
        el = document.createElement('div');
        el.id = 'loading-overlay';
        el.innerHTML = `<div class="loading-spinner"><i class="fas fa-spinner fa-spin fa-3x"></i><p>${msg}</p></div>`;
        el.style.cssText = `position: fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:flex; justify-content:center; align-items:center; z-index:9999; color:white; text-align:center; flex-direction:column; gap:10px;`;
        document.body.appendChild(el);
    }
}

function hideLoading() {
    const el = document.getElementById('loading-overlay');
    if (el) el.remove();
}

function setupScrollHints() {
    const containers = document.querySelectorAll('.horizontal-scroll-container');
    containers.forEach(c => {
        const check = () => {
            if (c.scrollWidth > c.clientWidth) c.classList.add('has-scroll'); else c.classList.remove('has-scroll');
        };
        check();
        window.addEventListener('resize', check);
    });
}

function setupTabSwitching() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            const tabId = this.getAttribute('data-tab');
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            this.classList.add('active');
            const targetTab = document.getElementById(`tab-${tabId}`);
            if (targetTab) targetTab.classList.add('active');
        });
    });
}

function getAqiClass(grade) {
    switch (String(grade).trim()) {
        case '1':
            return 'aqi-good';
        case '2':
            return 'aqi-moderate';
        case '3':
            return 'aqi-bad';
        case '4':
            return 'aqi-very-bad';
        default:
            return 'aqi-moderate';
    }
}

function getAqiStatusText(grade) {
    switch (String(grade).trim()) {
        case '1':
            return '좋음';
        case '2':
            return '보통';
        case '3':
            return '나쁨';
        case '4':
            return '매우나쁨';
        default:
            return '보통';
    }
}

function getAqiIcon(grade) {
    switch (String(grade)) {
        case '1':
            return '<i class="fas fa-smile" style="color:#2ecc71"></i>';
        case '2':
            return '<i class="fas fa-meh" style="color:#f39c12"></i>';
        case '3':
            return '<i class="fas fa-frown" style="color:#e74c3c"></i>';
        case '4':
            return '<i class="fas fa-dizzy" style="color:#8e44ad"></i>';
        default:
            return '<i class="fas fa-meh"></i>';
    }
}

function bindGpsButton(btnId, onSuccessCallback) {
    const btn = document.getElementById(btnId);
    if (!btn) return;

    btn.addEventListener('click', async () => {
        const originalHTML = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 위치 확인 중...';
        btn.disabled = true;

        if (!navigator.geolocation) {
            alert("GPS 미지원 브라우저입니다.");
            resetBtn();
            return;
        }

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                try {
                    await onSuccessCallback(position.coords.latitude, position.coords.longitude);
                    btn.innerHTML = '<i class="fas fa-check-circle"></i> 완료';
                    btn.classList.add('sync-success');
                    setTimeout(resetBtn, 2000);
                } catch (error) {
                    console.error("GPS Logic Error:", error);
                    failBtn();
                }
            },
            (error) => {
                console.error("Geolocation Error:", error);
                alert("위치 정보를 가져올 수 없습니다.");
                failBtn();
            },
            {enableHighAccuracy: true, timeout: 10000, maximumAge: 0}
        );

        function resetBtn() {
            btn.innerHTML = originalHTML;
            btn.disabled = false;
            btn.classList.remove('sync-success', 'sync-error');
        }

        function failBtn() {
            btn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 실패';
            btn.classList.add('sync-error');
            setTimeout(resetBtn, 2000);
        }
    });
}