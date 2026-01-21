/**
 * common.js - Weatherly 공통 유틸리티 (수정: 변수 중복 오류 방지)
 */

var API_BASE_URL = window.location.origin;

// [수정] const -> var 로 변경하여 중복 선언 에러 방지
var CITY_COORDINATES = {
    '서울': {lat: 37.5665, lng: 126.9780},
    '부산': {lat: 35.1796, lng: 129.0756},
    '대구': {lat: 35.8714, lng: 128.6014},
    '인천': {lat: 37.4563, lng: 126.7052},
    '광주': {lat: 35.1595, lng: 126.8526},
    '대전': {lat: 36.3504, lng: 127.3845},
    '울산': {lat: 35.5384, lng: 129.3114},
    '세종': {lat: 36.4800, lng: 127.2890},
    '제주': {lat: 33.4996, lng: 126.5312},
    '경기': {lat: 37.4138, lng: 127.5183},
    '강원': {lat: 37.8228, lng: 128.1555},
    '충북': {lat: 36.6350, lng: 127.4914},
    '충남': {lat: 36.6588, lng: 126.6728},
    '전북': {lat: 35.7175, lng: 127.1530},
    '전남': {lat: 34.8163, lng: 126.4629},
    '경북': {lat: 36.5760, lng: 128.5056},
    '경남': {lat: 35.2383, lng: 128.6924}
};

document.addEventListener('DOMContentLoaded', function () {
    initCommonFeatures();
    setTimeout(initCommonFeatures, 100);
});

function initCommonFeatures() {
    try {
        setupDarkMode();
    } catch (e) {
        console.error(e);
    }
    try {
        setupTabSwitching();
    } catch (e) {
        console.error(e);
    }
    try {
        updateCurrentTime();
    } catch (e) {
        console.error(e);
    }
    try {
        setupScrollHints();
    } catch (e) {
        console.error(e);
    }
}

function setupDarkMode() {
    let toggleBtn = document.getElementById('darkmode-toggle') || document.querySelector('.darkmode-toggle');
    const body = document.body;
    if (toggleBtn && toggleBtn.dataset.eventAttached === 'true') return;
    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    if (isDarkMode) {
        body.classList.add('dark-mode');
        updateDarkModeIcon(true, toggleBtn);
    }
    if (!toggleBtn) return;
    toggleBtn.addEventListener('click', (e) => {
        e.preventDefault();
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
    toggleBtn.dataset.eventAttached = 'true';
}

function updateDarkModeIcon(isDarkMode, btnElement) {
    const btn = btnElement || document.getElementById('darkmode-toggle') || document.querySelector('.darkmode-toggle');
    if (!btn) return;
    const icon = btn.querySelector('i');
    if (icon) icon.className = isDarkMode ? 'fas fa-sun' : 'fas fa-moon';
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

function getFullSidoName(short) {
    const map = {
        '서울': '서울특별시', '부산': '부산광역시', '대구': '대구광역시', '인천': '인천광역시',
        '광주': '광주광역시', '대전': '대전광역시', '울산': '울산광역시', '세종': '세종특별자치시',
        '경기': '경기도', '강원': '강원특별자치도', '충북': '충청북도', '충남': '충청남도',
        '전북': '전북특별자치도', '전남': '전라남도', '경북': '경상북도', '경남': '경상남도',
        '제주': '제주특별자치도'
    };
    return map[short] || short;
}