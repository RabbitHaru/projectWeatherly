/**
 * fine-dust.js - 미세먼지 페이지
 */

document.addEventListener('DOMContentLoaded', function () {
    if (!document.getElementById('fine-dust-location')) return;

    updateCurrentTime();
    setInterval(updateCurrentTime, 60000);

    // 초기 데이터 로드
    loadFineDustPageData();
    setInterval(loadFineDustPageData, 300000);

    // [중요] GPS 버튼 이벤트 바인딩
    // common.js의 bindGpsButton 함수를 활용하여 콜백 연결
    bindGpsButton('fine-dust-gps-sync-btn', async (lat, lng) => {
        await loadFineDustByGPS(lat, lng);
    });
});

async function loadFineDustPageData() {
    // 로딩 오버레이는 전체 로드일 때만 표시 (선택사항)
    // showLoading('대기질 정보를 불러오는 중...');

    try {
        await loadCurrentAirQuality();
    } catch (e) {
        console.error(e);
    }
    try {
        await loadRegionalComparison();
    } catch (e) {
        console.error(e);
    }

    try {
        const locNameEl = document.getElementById('fine-dust-location');
        if (locNameEl) {
            const sido = extractSidoName(locNameEl.textContent);
            await loadForecast(sido);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadCurrentAirQuality() {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) {
            updateFineDustUI(data.data);
        }
    } catch (e) {
        console.error(e);
    }
}

/**
 * [핵심 기능] GPS 좌표로 대기질 정보 조회
 */
async function loadFineDustByGPS(lat, lng) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        // 1. 서버에 좌표 전송
        const res = await fetch(`${baseUrl}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();

        if (data.success && data.data) {
            // 2. UI 업데이트 (지역명 포함)
            updateFineDustUI(data.data);

            // 3. 해당 지역의 예보 데이터도 추가로 로딩
            const sido = extractSidoName(data.data.sidoName);
            await loadForecast(sido);
        } else {
            alert('해당 위치의 대기질 정보를 찾을 수 없습니다.');
        }
    } catch (e) {
        console.error(e);
        alert("서버 연결에 실패했습니다.");
    }
}

async function loadForecast(sido) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) updateFineDustForecast(data.data);
    } catch (e) {
        console.error(e);
    }
}

async function loadRegionalComparison() {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        // [수정] 메인 페이지 날씨 지역과 통일 (5대 광역시)
        const sidos = ['서울', '부산', '대구', '광주', '대전'];

        const params = new URLSearchParams();
        sidos.forEach(sido => params.append('sidoNames', sido));

        const res = await fetch(`${baseUrl}/api/air-quality/compare?${params.toString()}`);
        const data = await res.json();
        if (data.success && data.data) updateRegionalUI(data.data);
    } catch (e) {
        console.error(e);
        // 비교 데이터 실패는 치명적이지 않으므로 UI 초기화만
        const container = document.getElementById('regional-aqi-list');
        if(container) container.innerHTML = '<div class="no-data" style="padding:10px;">정보 로딩 실패</div>';
    }
}

function updateFineDustUI(data) {
    if (!data) return;

    // [중요] 지역명 업데이트
    if (data.sidoName) {
        const fullName = getFullSidoName(data.sidoName);
        document.getElementById('fine-dust-location').textContent = fullName;
    }

    if (data.stationName) {
        const st = document.getElementById('station-name');
        if (st) st.textContent = data.stationName;
    }

    if (data.dataTime) {
        let timeStr = Array.isArray(data.dataTime)
            ? `${data.dataTime[0]}-${String(data.dataTime[1]).padStart(2, '0')}-${String(data.dataTime[2]).padStart(2, '0')} ${String(data.dataTime[3]).padStart(2, '0')}:00`
            : data.dataTime;
        document.getElementById('fine-dust-update-time').textContent = `업데이트: ${timeStr}`;
        const mt = document.getElementById('measurement-time');
        if (mt) mt.textContent = timeStr;
    }

    const badge = document.getElementById('fine-dust-overall-badge');
    if (badge) {
        badge.textContent = data.overallStatus || '--';
        badge.className = 'aqi-badge large-badge ' + getAqiClass(data.overallGrade);
    }

    if (data.healthAdvice) {
        document.getElementById('fine-dust-health-advice').textContent = data.healthAdvice;
        document.getElementById('advice-description').textContent = data.healthAdvice;
    }
    if (data.overallStatus) {
        document.getElementById('advice-title').textContent = `현재 상태: ${data.overallStatus}`;
    }

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
    const val = (item && item.value !== null && item.value !== undefined) ? item.value : '--';
    const status = (item && item.status) ? item.status : '--';
    const grade = (item && item.grade) ? item.grade : '';
    const unit = (item && item.unit) ? item.unit : unitStr;

    valEl.textContent = `${val} ${unit}`;

    if (statEl) {
        statEl.textContent = status;
        statEl.className = 'aqi-detail-status ' + getAqiClass(grade);
    }
}

function updateFineDustForecast(list) {
    const todayDiv = document.getElementById('today-aqi-forecast');
    const tomorrowDiv = document.getElementById('tomorrow-aqi-forecast');

    if (!list || !Array.isArray(list) || list.length === 0) {
        if (todayDiv) todayDiv.innerHTML = '<div class="no-data" style="text-align:center; padding:30px;">예보 정보 없음</div>';
        if (tomorrowDiv) tomorrowDiv.innerHTML = '<div class="no-data" style="text-align:center; padding:30px;">예보 정보 없음</div>';
        return;
    }

    if (todayDiv) todayDiv.innerHTML = renderForecastCard(list[0], '오늘');
    if (tomorrowDiv) {
        if (list.length > 1) tomorrowDiv.innerHTML = renderForecastCard(list[1], '내일');
        else tomorrowDiv.innerHTML = '<div class="no-data" style="text-align:center; padding:30px;">내일 예보 정보 없음</div>';
    }
}

function renderForecastCard(item, label) {
    const gradeClass = getAqiClass(item.overallGrade);
    const statusText = getAqiStatusText(item.overallGrade);
    const dateStr = item.date ? item.date : '';

    // [수정 포인트] 아래 HTML에서 .forecast-advice 태그의 style 속성을 제거했습니다.
    // 이제 CSS 파일의 다크모드 설정이 정상적으로 먹힐 겁니다.
    return `
        <div class="aqi-forecast-day">
            <div class="forecast-header">
                <span class="forecast-label" style="font-size:1.2rem; font-weight:bold;">${label}</span>
                <span class="forecast-date" style="color:#666;">(${dateStr})</span>
            </div>
            <div style="font-size:3.5rem; margin:15px 0; color:var(--primary-color);">
                ${getAqiIcon(item.overallGrade)}
            </div>
            <div class="forecast-overall ${gradeClass}" style="margin-bottom:15px;">${statusText}</div>
            <div class="forecast-details" style="display:flex; justify-content:center; gap:20px; font-size:1.1rem;">
                <div><i class="fas fa-smog"></i> 미세: <strong>${getAqiStatusText(item.pm10Grade)}</strong></div>
                <div><i class="fas fa-wind"></i> 초미세: <strong>${getAqiStatusText(item.pm25Grade)}</strong></div>
            </div>
            <div class="forecast-advice">
                <i class="fas fa-comment-dots"></i> ${item.advice || '상세 예보 정보가 없습니다.'}
            </div>
        </div>
    `;
}

function updateRegionalUI(list) {
    const container = document.getElementById('regional-aqi-list');
    if (!container) return;
    container.innerHTML = '';

    // 데이터 없음 처리
    if (!list || list.length === 0) {
        container.innerHTML = '<div class="no-data" style="text-align:center; padding:20px;">정보 로딩 실패</div>';
        return;
    }

    list.forEach(r => {
        const div = document.createElement('div');
        div.className = 'regional-aqi-item';
        div.innerHTML = `<span class="regional-aqi-name">${r.sidoName}</span><span class="regional-aqi-badge ${getAqiClass(r.overallGrade)}">${r.overallStatus}</span>`;
        container.appendChild(div);
    });
}

function extractSidoName(full) {
    if (!full) return '서울';
    if (full.length === 2) return full;
    const shortName = full.substring(0, 2);
    const mapping = {
        '서울': '서울', '부산': '부산', '대구': '대구', '인천': '인천',
        '광주': '광주', '대전': '대전', '울산': '울산', '세종': '세종',
        '경기': '경기', '강원': '강원', '제주': '제주',
        '충청': full.includes('북') ? '충북' : '충남',
        '전라': full.includes('북') ? '전북' : '전남',
        '경상': full.includes('북') ? '경북' : '경남'
    };
    return mapping[shortName] || shortName;
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

