/**
 * fine-dust.js - 미세먼지 페이지 (제주/독도 제거 완료)
 */

document.addEventListener('DOMContentLoaded', function () {
    if (!document.getElementById('fine-dust-location')) return;

    updateCurrentTime();
    setInterval(updateCurrentTime, 60000);

    // DB가 있으니 안심하고 로딩
    loadFineDustPageData();
    setInterval(loadFineDustPageData, 300000); // 5분마다 DB 데이터 확인

    if (typeof bindGpsButton === 'function') {
        bindGpsButton('fine-dust-gps-sync-btn', async (lat, lng) => {
            await loadFineDustByGPS(lat, lng);
        });
    }
});

async function loadFineDustPageData() {
    try {
        await loadCurrentAirQuality();
        await loadRegionalComparison();

        const locNameEl = document.getElementById('fine-dust-location');
        if (locNameEl) {
            const sido = extractSidoName(locNameEl.textContent);
            await loadForecast(sido);
        }
    } catch (e) {
        console.error(e);
    }
}

// 1. 상단 배너
async function loadCurrentAirQuality() {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) {
            updateFineDustUI(data.data);
            if (data.data.sidoName) loadForecast(data.data.sidoName);
        }
    } catch (e) {
        console.error(e);
    }
}

// 2. 지역별 리스트 (제주/독도 제거됨)
async function loadRegionalComparison() {
    const listContainer = document.getElementById('regional-aqi-list') || document.getElementById('nationwide-list');
    if (!listContainer) return;

    // [수정] 메인 페이지와 동일한 17개 광역 자치단체 리스트 (제주 포함)
    const targetRegions = [
        '서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종',
        '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주'
    ];

    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;

    try {
        // 전국의 데이터를 한 번에 가져와서 리스트 구성
        const res = await fetch(`${baseUrl}/api/air-quality/sido/전국`);
        const result = await res.json();
        let allData = [];
        if (result.success) allData = result.data;

        listContainer.innerHTML = '';

        targetRegions.forEach(r => {
            // 데이터 중에서 해당 시도 이름이 포함된 첫 번째 데이터를 찾음
            const data = allData.find(d => d.sidoName.includes(r));
            if (data) {
                renderSimpleItem(listContainer, data);
            }
        });

    } catch (e) {
        console.error("리스트 로드 실패:", e);
    }
}

function renderSimpleItem(container, r) {
    const div = document.createElement('div');
    div.className = 'region-weather';

    // 서버에서 이미 통합지수로 계산해서 보낸 overallStatus를 우선 사용
    const statusText = r.overallStatus ? r.overallStatus : getAqiStatusText(r.overallGrade);
    const colorClass = getAqiClass(r.overallGrade); // 등급 숫자에 맞는 색상 클래스 반환
    const testBadge = r.isMock ? '<span style="color:#e74c3c; font-size:0.7em; margin-left:5px;">[TEST]</span>' : '';

    div.innerHTML = `
        <div class="region-info">
            <span class="region-name">${r.sidoName}${testBadge}</span>
        </div>
        <span class="aqi-badge ${colorClass}">${statusText}</span>
    `;

    div.addEventListener('click', () => {
        document.querySelectorAll('.region-weather').forEach(el => el.classList.remove('selected'));
        div.classList.add('selected');
        loadFineDustBySido(r.sidoName); // 클릭 시 상세 정보도 해당 지역 데이터로 변경
    });

    container.appendChild(div);
}

// 3. 예보
async function loadForecast(sido) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    if (!sido || sido.includes('?')) sido = '서울';
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) updateFineDustForecast(data.data);
    } catch (e) {
        console.error("예보 로드 실패:", e);
    }
}

function updateFineDustForecast(list) {
    const todayDiv = document.getElementById('today-aqi-forecast');
    const tomorrowDiv = document.getElementById('tomorrow-aqi-forecast');

    if (!list || !Array.isArray(list) || list.length === 0) {
        const noData = '<div class="no-data" style="padding:30px; text-align:center;">예보 정보가 없습니다.</div>';
        if (todayDiv) todayDiv.innerHTML = noData;
        if (tomorrowDiv) tomorrowDiv.innerHTML = noData;
        return;
    }
    if (todayDiv && list.length > 0) todayDiv.innerHTML = renderForecastCard(list[0], '오늘');
    if (tomorrowDiv) {
        if (list.length > 1) tomorrowDiv.innerHTML = renderForecastCard(list[1], '내일');
        else tomorrowDiv.innerHTML = '<div class="no-data" style="padding:30px; text-align:center;">내일 예보 준비 중</div>';
    }
}

function renderForecastCard(item, label) {
    const gradeClass = getAqiClass(item.overallGrade);
    const statusText = getAqiStatusText(item.overallGrade);
    const dateStr = item.date ? item.date : '';
    let adviceText = item.advice || '상세 예보 정보가 없습니다.';
    let causeText = item.cause ? `<br><br><strong><i class="fas fa-search-plus"></i> 원인:</strong><br>${item.cause}` : '';

    return `
        <div class="aqi-forecast-day">
            <div class="forecast-header">
                <span class="forecast-label" style="font-size:1.2rem; font-weight:bold;">${label}</span>
                <span class="forecast-date" style="color:#666; font-size:0.9rem;">(${dateStr})</span>
            </div>
            <div style="font-size:3.5rem; margin:15px 0; color:var(--primary-color);">
                ${getAqiIcon(item.overallGrade)}
            </div>
            <div class="forecast-overall ${gradeClass}" style="margin-bottom:15px; font-weight:bold;">${statusText}</div>
            <div class="forecast-advice" style="text-align: center; word-break: keep-all; line-height: 1.6;">
                <i class="fas fa-quote-left" style="color:#ddd; margin-right:5px;"></i>
                ${adviceText}
                ${causeText}
                <i class="fas fa-quote-right" style="color:#ddd; margin-left:5px;"></i>
            </div>
        </div>
    `;
}

function updateFineDustUI(data) {
    if (!data) return;
    const locNameEl = document.getElementById('fine-dust-location');

    if (locNameEl && data.sidoName) {
        let name = getFullSidoName(data.sidoName);
        if (data.isMock) {
            name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px; vertical-align:middle; margin-left: 5px;">TEST MODE</span>';
            locNameEl.innerHTML = name;
        } else {
            locNameEl.textContent = name;
        }
    }

    if (data.stationName) {
        const st = document.getElementById('station-name');
        if (st) st.textContent = data.stationName;
    }

    if (data.dataTime) {
        let timeStr = Array.isArray(data.dataTime) ?
            `${data.dataTime[0]}-${String(data.dataTime[1]).padStart(2, '0')}-${String(data.dataTime[2]).padStart(2, '0')} ${String(data.dataTime[3]).padStart(2, '0')}:00`
            : data.dataTime;
        document.getElementById('fine-dust-update-time').textContent = `업데이트: ${timeStr}`;
        const mt = document.getElementById('measurement-time');
        if (mt) mt.textContent = timeStr;
    }
    const badge = document.getElementById('fine-dust-overall-badge');
    if (badge) {
        const statusText = data.overallStatus ? data.overallStatus : getAqiStatusText(data.overallGrade);
        badge.textContent = statusText;
        badge.className = 'aqi-badge large-badge ' + getAqiClass(data.overallGrade);
    }
    if (data.healthAdvice) {
        document.getElementById('fine-dust-health-advice').textContent = data.healthAdvice;
        document.getElementById('advice-description').textContent = data.healthAdvice;
    }
    if (data.overallStatus) document.getElementById('advice-title').textContent = `현재 상태: ${data.overallStatus}`;

    updateDetailCard('khai', data.khai, '');
    updateDetailCard('pm10-detail', data.pm10, 'µg/m³');
    updateDetailCard('pm25-detail', data.pm25, 'µg/m³');
    updateDetailCard('o3-detail', data.o3, 'ppm');
    updateDetailCard('no2', data.no2, 'ppm');
    updateDetailCard('co', data.co, 'ppm');
}

function updateDetailCard(prefix, item, unitStr) {
    const valEl = document.getElementById(`${prefix}-value`);
    if (!valEl) return;
    let statEl = document.getElementById(`${prefix}-status`);
    const val = (item && item.value !== null) ? item.value : '--';
    const status = (item && item.status) ? item.status : '--';
    const grade = (item && item.grade) ? item.grade : '';
    valEl.textContent = `${val} ${item && item.unit ? item.unit : unitStr}`;
    if (statEl) {
        statEl.textContent = status;
        statEl.className = 'aqi-detail-status ' + getAqiClass(grade);
    }
}

async function loadFineDustByGPS(lat, lng) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();
        if (data.success && data.data) {
            updateFineDustUI(data.data);
            const sido = extractSidoName(data.data.sidoName);
            await loadForecast(sido);
        } else {
            alert('위치 정보를 찾을 수 없습니다.');
        }
    } catch (e) {
        console.error(e);
        alert("서버 연결 실패");
    }
}

async function loadFineDustBySido(sidoName) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/sido/${encodeURIComponent(sidoName)}`);
        const data = await res.json();
        if (data.success && data.data && data.data.length > 0) {
            updateFineDustUI(data.data[0]);
            loadForecast(sidoName);
        }
    } catch (e) {
        console.error("지역 로드 실패:", e);
    }
}

function extractSidoName(full) {
    if (!full) return '서울';
    const mapping = {
        '서울': '서울', '부산': '부산', '대구': '대구', '인천': '인천',
        '광주': '광주', '대전': '대전', '울산': '울산', '세종': '세종',
        '경기': '경기', '강원': '강원', '제주': '제주', // 제주 추가
        '충청': full.includes('북') ? '충북' : '충남',
        '전라': full.includes('북') ? '전북' : '전남',
        '경상': full.includes('북') ? '경북' : '경남',
        '서울특별시': '서울', '부산광역시': '부산'
    };
    if (full.length === 2) return full;
    const shortName = full.substring(0, 2);
    return mapping[shortName] || mapping[full] || '서울';
}

function getFullSidoName(short) {
    const map = {
        '서울': '서울특별시', '부산': '부산광역시', '대구': '대구광역시', '인천': '인천광역시',
        '광주': '광주광역시', '대전': '대전광역시', '울산': '울산광역시', '세종': '세종특별자치시',
        '경기': '경기도', '강원': '강원특별자치도', '충북': '충청북도', '충남': '충청남도',
        '전북': '전북특별자치도', '전남': '전라남도', '경북': '경상북도', '경남': '경상남도',
        '제주': '제주특별자치도' // 제주 추가
    };
    return map[short] || short;
}

function getAqiClass(grade) {
    switch (String(grade)) {
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
            return '매우나쁨'; // '매우 나쁨'에서 공백 제거하여 통일
        default:
            return '보통'; // 정보 없을 시 '보통'으로 기본값 설정
    }
}

function getAqiIcon(grade) {
    switch (String(grade).trim()) {
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