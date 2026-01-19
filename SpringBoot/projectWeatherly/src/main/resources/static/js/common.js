/**
 * common.js - Weatherly 공통 유틸리티
 * (Dark Mode Fix & Robust Initialization Version)
 */

var API_BASE_URL = window.location.origin;

document.addEventListener('DOMContentLoaded', function () {
    // 1. 즉시 실행 시도
    initCommonFeatures();

    // 2. 혹시 헤더(Fragment)가 늦게 로딩될 경우를 대비해 0.1초 뒤 재시도 (이중 안전장치)
    setTimeout(initCommonFeatures, 100);
});

function initCommonFeatures() {
    try {
        setupDarkMode();
    } catch (e) {
        console.error("다크모드 설정 실패:", e);
    }
    try {
        setupTabSwitching();
    } catch (e) {
        console.error("탭 설정 실패:", e);
    }
    try {
        updateCurrentTime();
    } catch (e) {
        console.error("시간 업데이트 실패:", e);
    }
    try {
        setupScrollHints();
    } catch (e) {
        console.error("스크롤 힌트 실패:", e);
    }
}

function setupDarkMode() {
    // [중요] ID가 다를 경우를 대비해 클래스로도 찾아봄
    let toggleBtn = document.getElementById('darkmode-toggle');

    // ID로 못 찾으면 클래스로 찾기 (이게 핵심!)
    if (!toggleBtn) {
        toggleBtn = document.querySelector('.darkmode-toggle');
    }

    const body = document.body;

    // 이미 이벤트가 걸려있는지 확인 (중복 방지)
    if (toggleBtn && toggleBtn.dataset.eventAttached === 'true') {
        return;
    }

    // 1. 로컬 스토리지 확인 & 초기화
    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    if (isDarkMode) {
        body.classList.add('dark-mode');
        updateDarkModeIcon(true, toggleBtn);
    }

    if (!toggleBtn) {
        // 아직 버튼이 로딩되지 않았을 수 있으므로 경고는 생략하거나 debug 레벨로 낮춤
        // console.debug("다크모드 버튼을 아직 찾지 못했습니다. (재시도 예정)");
        return;
    }

    // 2. 이벤트 리스너 부착
    toggleBtn.addEventListener('click', (e) => {
        e.preventDefault(); // 중요: a 태그일 경우 튀는 현상 방지

        const isCurrentlyDark = body.classList.contains('dark-mode');

        if (isCurrentlyDark) {
            body.classList.remove('dark-mode');
            localStorage.setItem('darkMode', 'false');
            updateDarkModeIcon(false, toggleBtn);
        } else {
            body.classList.add('dark-mode');
            localStorage.setItem('darkMode', 'true');
            updateDarkModeIcon(true, toggleBtn);
        }
    });

    // 이벤트 부착 완료 표시 (재실행 시 중복 방지용)
    toggleBtn.dataset.eventAttached = 'true';
    console.log("✅ 다크모드 버튼 연결 성공!");
}

function updateDarkModeIcon(isDarkMode, btnElement) {
    // 버튼 요소를 인자로 받거나 다시 찾음
    const btn = btnElement || document.getElementById('darkmode-toggle') || document.querySelector('.darkmode-toggle');
    if (!btn) return;

    const icon = btn.querySelector('i');
    if (icon) {
        // [수정] 아이콘 클래스 확실하게 변경
        icon.className = isDarkMode ? 'fas fa-sun' : 'fas fa-moon';
    }
    btn.title = isDarkMode ? '라이트모드로 전환' : '다크모드로 전환';
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
            if (c.scrollWidth > c.clientWidth) c.classList.add('has-scroll');
            else c.classList.remove('has-scroll');
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