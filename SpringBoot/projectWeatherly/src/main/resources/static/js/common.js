/**
 * common.js - Weatherly 공통 유틸리티 및 전역 상태 관리
 * (RegionManager: 위치 데이터 중앙 관리)
 */

var API_BASE_URL = window.location.origin;

// 1. 전역 지역 데이터
const ALL_REGIONS = [
    { name: '서울', lat: 37.5665, lng: 126.9780, code: '1100000000' },
    { name: '부산', lat: 35.1796, lng: 129.0756, code: '2600000000' },
    { name: '대구', lat: 35.8714, lng: 128.6014, code: '2700000000' },
    { name: '인천', lat: 37.4563, lng: 126.7052, code: '2800000000' },
    { name: '광주', lat: 35.1595, lng: 126.8526, code: '2900000000' },
    { name: '대전', lat: 36.3504, lng: 127.3845, code: '3000000000' },
    { name: '울산', lat: 35.5384, lng: 129.3114, code: '3100000000' },
    { name: '세종', lat: 36.4800, lng: 127.2890, code: '3600000000' },
    { name: '경기', lat: 37.4138, lng: 127.5183, code: '4100000000' },
    { name: '강원', lat: 37.8228, lng: 128.1555, code: '4200000000' },
    { name: '충북', lat: 36.6350, lng: 127.4914, code: '4300000000' },
    { name: '충남', lat: 36.6588, lng: 126.6728, code: '4400000000' },
    { name: '전북', lat: 35.7175, lng: 127.1530, code: '4500000000' },
    { name: '전남', lat: 34.8163, lng: 126.4629, code: '4600000000' },
    { name: '경북', lat: 36.5760, lng: 128.5056, code: '4700000000' },
    { name: '경남', lat: 35.2383, lng: 128.6924, code: '4800000000' },
    { name: '제주', lat: 33.4996, lng: 126.5312, code: '5000000000' },
    { name: '독도', lat: 37.2429, lng: 131.8669, code: '' }
];

// 2. 지역 저장소 관리자
const RegionManager = {
    KEY: 'fixedRegion',
    save: function (rawName, lat, lng) {
        // '내 위치'가 서울로 변하지 않도록 처리
        const stdName = (rawName === '내 위치') ? '내 위치' : extractSidoName(rawName);

        if (!lat || !lng) {
            const found = ALL_REGIONS.find(r => r.name === stdName);
            if (found) {
                lat = found.lat;
                lng = found.lng;
            }
        }

        const data = { name: stdName, lat, lng };
        sessionStorage.setItem(this.KEY, JSON.stringify(data));
        console.log(`💾 [전역 저장] ${stdName} (${lat}, ${lng})`);
    },
    load: function () {
        const data = sessionStorage.getItem(this.KEY);
        return data ? JSON.parse(data) : null;
    },
    clear: function () {
        sessionStorage.removeItem(this.KEY);
    }
};

// ⭐ 페이지 로드 시 실행 (이 부분이 문제 해결의 열쇠!)
document.addEventListener('DOMContentLoaded', function () {
    initCommonFeatures();
    setupDarkMode();
    updateCurrentTime();
    setupTabSwitching();
});

function initCommonFeatures() {
    const urlParams = new URLSearchParams(window.location.search);
    const lat = urlParams.get('lat') || urlParams.get('latitude');
    const lon = urlParams.get('lon') || urlParams.get('longitude');

    if (lat && lon) {
        const saved = RegionManager.load();

        // 🚨 [핵심 수정] 이미 저장된 위치와 좌표가 같다면, 굳이 '내 위치'로 덮어쓰지 않음!
        // (예: 부산으로 저장돼있는데 URL에 좌표가 있다고 해서 '내 위치'로 바꾸지 않음)
        if (saved && isSameLocation(saved.lat, saved.lng, lat, lon)) {
            if (saved.name !== '내 위치') {
                console.log(`📍 기존 위치명 유지: ${saved.name}`);
                return;
            }
        }

        // 저장된 게 없거나 좌표가 바뀌었으면 일단 저장
        RegionManager.save('내 위치', lat, lon);

        // 🚀 만약 이름이 '내 위치'라면, 서버에 물어봐서 진짜 이름으로 고쳐놓기 (Self-Healing)
        fetchRealRegionName(lat, lon);
    }
}

// 좌표가 같은지 확인하는 헬퍼 함수
function isSameLocation(lat1, lon1, lat2, lon2) {
    if (!lat1 || !lon1 || !lat2 || !lon2) return false;
    return Math.abs(lat1 - lat2) < 0.0001 && Math.abs(lon1 - lon2) < 0.0001;
}

// '내 위치' -> '부산광역시'로 자동 변환하는 함수
async function fetchRealRegionName(lat, lon) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${lat}&longitude=${lon}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        const data = await res.json();
        if (data.success && data.data.regionName) {
            console.log(`✨ 위치명 자동 업데이트: 내 위치 -> ${data.data.regionName}`);

            // 세션 스토리지 업데이트
            RegionManager.save(data.data.regionName, lat, lon);

            // 화면에 '내 위치'라고 떠있는 글자가 있으면 바로 바꿔줌!
            const locEls = document.querySelectorAll('#current-location, .location-name, #display-region-name, #fine-dust-location');
            locEls.forEach(el => {
                if (el.textContent.includes('내 위치') || el.textContent.includes('위치 확인 중')) {
                    el.textContent = getFullSidoName(data.data.regionName);
                }
            });
        }
    } catch (e) {
        console.error("위치명 변환 실패", e);
    }
}

// --- 기타 유틸리티 함수들 (기존 유지) ---

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

function bindGpsButton(btnId, onSuccessCallback) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.addEventListener('click', async () => {
        RegionManager.clear(); // 새 위치 찾을 땐 초기화
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
            { enableHighAccuracy: true, timeout: 5000 }
        );

        function resetBtn() {
            btn.innerHTML = originalHTML;
            btn.disabled = false;
        }
    });
}

function extractSidoName(full) {
    if (!full) return '서울';
    const cleanFull = full.trim();
    if (cleanFull === '내 위치') return '내 위치'; // 예외 처리 필수

    // 주요 시/도 맵핑
    const mapping = {
        '서울': '서울', '부산': '부산', '대구': '대구', '인천': '인천',
        '광주': '광주', '대전': '대전', '울산': '울산', '세종': '세종',
        '경기': '경기', '강원': '강원', '제주': '제주',
        '충청북도': '충북', '충청남도': '충남', '충북': '충북', '충남': '충남',
        '전라북도': '전북', '전라남도': '전남', '전북': '전북', '전남': '전남',
        '경상북도': '경북', '경상남도': '경남', '경북': '경북', '경남': '경남',
        '서울특별시': '서울', '부산광역시': '부산', '대전광역시': '대전',
        '대구광역시': '대구', '인천광역시': '인천', '광주광역시': '광주', '울산광역시': '울산',
        '세종특별자치시': '세종', '제주특별자치도': '제주', '강원특별자치도': '강원', '강원도': '강원'
    };

    // 1. 전체 이름이 맵핑 테이블에 있으면 바로 반환
    if (mapping[cleanFull]) return mapping[cleanFull];

    // 2. 앞 2글자로도 매핑 테이블 검사
    const prefix2 = cleanFull.substring(0, 2);
    if (mapping[prefix2]) return mapping[prefix2];

    // 3. '충청', '전라', '경상' 기반 특별 규칙 (북/남 구분)
    if (prefix2 === '충청') return cleanFull.includes('북') ? '충북' : '충남';
    if (prefix2 === '전라') return cleanFull.includes('북') ? '전북' : '전남';
    if (prefix2 === '경상') return cleanFull.includes('북') ? '경북' : '경남';

    // 4. 그래도 못 찾는 동네이름(예: 마라도) 이면 그대로 반환해서 다른 로직이 쓰게 둠 (무조건 서울로 해버리면 덮어씌워짐)
    return cleanFull;
}

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

// AQI 관련 함수들 (기존 유지)
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

function setupTabSwitching() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    // 버튼이 없으면 함수 종료 (에러 방지)
    if (tabBtns.length === 0) return;

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            // 1. 클릭된 버튼의 타겟 ID 가져오기
            const tabId = this.getAttribute('data-tab');

            // 2. 모든 버튼과 콘텐츠의 active 클래스 제거 (초기화)
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            // 3. 클릭된 버튼 활성화
            this.classList.add('active');

            // 4. 연결된 콘텐츠 활성화
            const targetTab = document.getElementById(`tab-${tabId}`);
            if (targetTab) {
                targetTab.classList.add('active');
            }
        });
    });
}