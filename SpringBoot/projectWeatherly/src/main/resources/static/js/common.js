/**
 * common.js - Weatherly 공통 유틸리티 및 전역 상태 관리
 * (RegionManager를 sessionStorage로 변경하여 휘발성으로 만듦)
 */

var API_BASE_URL = window.location.origin;

// 1. 전역 지역 데이터
const ALL_REGIONS = [
    {name: '서울', lat: 37.5665, lng: 126.9780, code: '1100000000'},
    {name: '부산', lat: 35.1796, lng: 129.0756, code: '2600000000'},
    {name: '대구', lat: 35.8714, lng: 128.6014, code: '2700000000'},
    {name: '인천', lat: 37.4563, lng: 126.7052, code: '2800000000'},
    {name: '광주', lat: 35.1595, lng: 126.8526, code: '2900000000'},
    {name: '대전', lat: 36.3504, lng: 127.3845, code: '3000000000'},
    {name: '울산', lat: 35.5384, lng: 129.3114, code: '3100000000'},
    {name: '세종', lat: 36.4800, lng: 127.2890, code: '3600000000'},
    {name: '경기', lat: 37.4138, lng: 127.5183, code: '4100000000'},
    {name: '강원', lat: 37.8228, lng: 128.1555, code: '4200000000'},
    {name: '충북', lat: 36.6350, lng: 127.4914, code: '4300000000'},
    {name: '충남', lat: 36.6588, lng: 126.6728, code: '4400000000'},
    {name: '전북', lat: 35.7175, lng: 127.1530, code: '4500000000'},
    {name: '전남', lat: 34.8163, lng: 126.4629, code: '4600000000'},
    {name: '경북', lat: 36.5760, lng: 128.5056, code: '4700000000'},
    {name: '경남', lat: 35.2383, lng: 128.6924, code: '4800000000'},
    {name: '제주', lat: 33.4996, lng: 126.5312, code: '5000000000'},
    {name: '독도', lat: 37.2429, lng: 131.8669, code: ''}
];

// 2. 지역 저장소 관리자 (Region Manager)
// ⭐ localStorage -> sessionStorage 로 변경! (브라우저 닫으면 초기화됨)
const RegionManager = {
    KEY: 'fixedRegion',

    save: function (rawName, lat, lng) {
        const stdName = extractSidoName(rawName);
        if (!lat || !lng) {
            const found = ALL_REGIONS.find(r => r.name === stdName);
            if (found) {
                lat = found.lat;
                lng = found.lng;
            }
        }
        const data = {name: stdName, lat, lng};

        // ⭐ 여기가 핵심 변경점!
        sessionStorage.setItem(this.KEY, JSON.stringify(data));
        console.log(`💾 지역 임시 저장됨(탭 닫으면 삭제): ${stdName}`);
    },

    load: function () {
        // ⭐ 불러올 때도 sessionStorage에서
        const data = sessionStorage.getItem(this.KEY);
        return data ? JSON.parse(data) : null;
    },

    clear: function () {
        // ⭐ 지울 때도 sessionStorage에서
        sessionStorage.removeItem(this.KEY);
        console.log('🗑️ 임시 저장된 지역 초기화됨');
    }
};

document.addEventListener('DOMContentLoaded', function () {
    initCommonFeatures();
});

function initCommonFeatures() {
    try {
        setupDarkMode();
    } catch (e) {
    }
    try {
        setupTabSwitching();
    } catch (e) {
    }
    try {
        updateCurrentTime();
    } catch (e) {
    }
    try {
        setupScrollHints();
    } catch (e) {
    }
}

// --- 공통 기능 함수들 ---

function setupDarkMode() {
    let toggleBtn = document.getElementById('darkmode-toggle');
    const body = document.body;
    if (localStorage.getItem('darkMode') === 'true') {
        body.classList.add('dark-mode');
        if (toggleBtn) updateDarkModeIcon(true, toggleBtn);
    }
    if (!toggleBtn || toggleBtn.dataset.eventAttached === 'true') return;
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

function updateDarkModeIcon(isDarkMode, btn) {
    const icon = btn.querySelector('i');
    if (icon) icon.className = isDarkMode ? 'fas fa-sun' : 'fas fa-moon';
}

function updateCurrentTime(targetIds = ['current-time', 'fine-dust-current-time']) {
    const now = new Date();
    const options = {
        year: 'numeric', month: 'long', day: 'numeric', weekday: 'long',
        hour: '2-digit', minute: '2-digit', hour12: false
    };
    const timeStr = now.toLocaleDateString('ko-KR', options);
    targetIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = timeStr;
    });
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

function bindGpsButton(btnId, onSuccessCallback) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.addEventListener('click', async () => {
        RegionManager.clear();
        const originalHTML = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 확인 중...';
        btn.disabled = true;

        if (!navigator.geolocation) {
            alert("GPS 미지원 브라우저입니다.");
            resetBtn();
            return;
        }
        navigator.geolocation.getCurrentPosition(
            async (position) => {
                await onSuccessCallback(position.coords.latitude, position.coords.longitude);
                btn.innerHTML = '<i class="fas fa-check-circle"></i> 완료';
                setTimeout(resetBtn, 2000);
            },
            (error) => {
                console.error(error);
                alert("위치 정보를 가져올 수 없습니다.");
                resetBtn();
            },
            {enableHighAccuracy: true, timeout: 5000}
        );

        function resetBtn() {
            btn.innerHTML = originalHTML;
            btn.disabled = false;
        }
    });
}

// 헬퍼 함수들
function getFullSidoName(name) {
    if (!name) return '대한민국';
    const cleanName = name.trim();
    const map = {
        '서울': '서울특별시', '부산': '부산광역시', '대구': '대구광역시', '인천': '인천광역시',
        '광주': '광주광역시', '대전': '대전광역시', '울산': '울산광역시', '세종': '세종특별자치시',
        '경기': '경기도',
        '강원': '강원특별자치도', '강원도': '강원특별자치도',
        '충북': '충청북도', '충남': '충청남도',
        '전북': '전북특별자치도', '전남': '전라남도', '경북': '경상북도', '경남': '경상남도',
        '제주': '제주특별자치도'
    };
    if (map[cleanName]) return map[cleanName];
    if (cleanName.length > 2) return cleanName;
    return cleanName;
}

function extractSidoName(full) {
    if (!full) return '서울';
    const cleanFull = full.trim();
    const mapping = {
        '서울': '서울', '부산': '부산', '대구': '대구', '인천': '인천',
        '광주': '광주', '대전': '대전', '울산': '울산', '세종': '세종',
        '경기': '경기', '강원': '강원', '제주': '제주',
        '충청': cleanFull.includes('북') ? '충북' : '충남',
        '전라': cleanFull.includes('북') ? '전북' : '전남',
        '경상': cleanFull.includes('북') ? '경북' : '경남',
        '서울특별시': '서울', '부산광역시': '부산', '대전광역시': '대전',
        '대구광역시': '대구', '인천광역시': '인천', '광주광역시': '광주', '울산광역시': '울산',
        '세종특별자치시': '세종', '제주특별자치도': '제주', '강원특별자치도': '강원', '강원도': '강원'
    };
    if (cleanFull.length === 2) return cleanFull;
    const shortName = cleanFull.substring(0, 2);
    return mapping[shortName] || mapping[cleanFull] || '서울';
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
            return '<i class="fas fa-dizzy" style="color:#e74c3c"></i>';
        default:
            return '<i class="fas fa-meh"></i>';
    }
}