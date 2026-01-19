/**
 * fine-dust.js - 미세먼지 페이지 전용 스크립트 (최종 수정)
 */

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(updateCurrentTime, 60000);

    loadFineDustPageData();
    setInterval(loadFineDustPageData, 300000);

    bindGpsButton('fine-dust-gps-sync-btn', async (lat, lng) => {
        await loadFineDustByGPS(lat, lng);
    });
});

async function loadFineDustPageData() {
    try {
        showLoading('대기질 정보를 불러오는 중...');
        await Promise.all([
            loadCurrentAirQuality(),
            loadRegionalComparison()
        ]);

        const locNameEl = document.getElementById('fine-dust-location');
        if (locNameEl) {
            const sido = extractSidoName(locNameEl.textContent);
            await loadForecast(sido);
        }
        hideLoading();
    } catch (error) {
        console.error('데이터 로드 실패', error);
        hideLoading();
    }
}

// [API 호출]
async function loadCurrentAirQuality() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) updateFineDustUI(data.data);
    } catch (e) {
        console.error(e);
    }
}

async function loadFineDustByGPS(lat, lng) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();
        if (data.success) {
            updateFineDustUI(data.data);
            const sido = extractSidoName(data.data.sidoName);
            await loadForecast(sido);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadForecast(sido) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) updateFineDustForecast(data.data);
    } catch (e) {
        console.error(e);
    }
}

async function loadRegionalComparison() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/compare?sidoNames=서울,부산,대구,인천,광주,대전,울산,경기,제주`);
        const data = await res.json();
        if (data.success) updateRegionalUI(data.data);
    } catch (e) {
        console.error(e);
    }
}

// [UI 업데이트]
function updateFineDustUI(data) {
    if (!data) return;

    if (data.sidoName) document.getElementById('fine-dust-location').textContent = getFullSidoName(data.sidoName);
    if (data.stationName) document.getElementById('station-name').textContent = data.stationName;

    if (data.dataTime) {
        let timeStr = data.dataTime;
        if (Array.isArray(data.dataTime)) {
            timeStr = `${data.dataTime[0]}-${String(data.dataTime[1]).padStart(2, '0')}-${String(data.dataTime[2]).padStart(2, '0')} ${String(data.dataTime[3]).padStart(2, '0')}:00`;
        }
        document.getElementById('fine-dust-update-time').textContent = `업데이트: ${timeStr}`;
        document.getElementById('measurement-time').textContent = timeStr;
    }

    const badge = document.getElementById('fine-dust-overall-badge');
    if (badge) {
        badge.textContent = data.overallStatus || '정보없음';
        badge.className = 'aqi-badge large-badge ' + getAqiClass(data.overallGrade);
    }

    if (data.healthAdvice) {
        document.getElementById('fine-dust-health-advice').textContent = data.healthAdvice;
        document.getElementById('advice-description').textContent = data.healthAdvice;
    }
    if (data.overallStatus) {
        document.getElementById('advice-title').textContent = `현재 상태: ${data.overallStatus}`;
    }

    // [중요 Fix] HTML ID와 정확히 매칭
    updateDetailCard('khai', data.khai, '');
    updateDetailCard('pm10-detail', data.pm10, 'µg/m³'); // id="pm10-detail-value"
    updateDetailCard('pm25-detail', data.pm25, 'µg/m³'); // id="pm25-detail-value"
    updateDetailCard('o3-detail', data.o3, 'ppm');       // id="o3-detail-value"
    updateDetailCard('no2', data.no2, 'ppm');            // id="no2-value"
    updateDetailCard('co', data.co, 'ppm');              // id="co-value"
}

function updateDetailCard(prefix, item, unitStr) {
    const valEl = document.getElementById(`${prefix}-value`);
    if (!valEl) return;

    const statEl = document.getElementById(`${prefix}-status`);

    const val = (item && item.value !== null) ? item.value : '--';
    const status = (item && item.status) ? item.status : '--';
    const grade = (item && item.grade) ? item.grade : '';
    const unit = (item && item.unit) ? item.unit : unitStr;

    valEl.textContent = `${val} ${unit}`;

    if (statEl) {
        statEl.textContent = status;
        statEl.className = 'aqi-detail-status ' + getAqiClass(grade);
    }
}

// [중요] 가로 카드 형태로 예보 렌더링
function updateFineDustForecast(list) {
    const todayDiv = document.getElementById('today-aqi-forecast');
    const tomorrowDiv = document.getElementById('tomorrow-aqi-forecast');
    const weeklyDiv = document.getElementById('weekly-aqi-forecast');

    if (!list || !Array.isArray(list) || list.length === 0) {
        const noData = '<div class="no-data">예보 정보 없음</div>';
        if (todayDiv) todayDiv.innerHTML = noData;
        if (tomorrowDiv) tomorrowDiv.innerHTML = noData;
        if (weeklyDiv) weeklyDiv.innerHTML = noData;
        return;
    }

    // 오늘
    if (todayDiv) todayDiv.innerHTML = renderForecastCard(list[0], '오늘');
    // 내일
    if (tomorrowDiv) {
        if (list.length > 1) tomorrowDiv.innerHTML = renderForecastCard(list[1], '내일');
        else tomorrowDiv.innerHTML = '<div class="no-data">내일 예보 없음</div>';
    }
    // 주간
    if (weeklyDiv) {
        weeklyDiv.innerHTML = '';
        list.forEach(item => {
            weeklyDiv.innerHTML += renderForecastCard(item, item.date);
        });
    }
}

// 가로 카드 HTML 생성
function renderForecastCard(item, label) {
    const gradeClass = getAqiClass(item.overallGrade);
    const statusText = getAqiStatusText(item.overallGrade);

    return `
        <div class="aqi-forecast-item">
            <div class="aqi-forecast-header"><h5>${label} (${item.date || ''})</h5></div>
            <div style="font-size:3rem; margin:10px 0; color:var(--primary-color);">
                ${getAqiIcon(item.overallGrade)}
            </div>
            <div class="aqi-forecast-value" style="font-size:1.2rem; font-weight:bold; margin-bottom:5px;">
                ${statusText}
            </div>
            <div class="aqi-forecast-status ${gradeClass}" style="padding:4px 10px; border-radius:15px; color:white; font-size:0.8rem;">
                통합 ${item.overallGrade}등급
            </div>
            <div style="margin-top:10px; font-size:0.8rem; color:#666; white-space:normal; overflow:hidden; text-overflow:ellipsis; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical;">
                ${item.advice || ''}
            </div>
        </div>
    `;
}

function updateRegionalUI(list) {
    const container = document.getElementById('regional-aqi-list');
    if (!container) return;
    container.innerHTML = '';

    list.forEach(r => {
        const div = document.createElement('div');
        div.className = 'regional-aqi-item';
        div.innerHTML = `
            <span class="regional-aqi-name">${r.sidoName}</span>
            <span class="regional-aqi-badge ${getAqiClass(r.overallGrade)}">${r.overallStatus}</span>
        `;
        container.appendChild(div);
    });
}

function extractSidoName(full) {
    if (!full) return '서울';
    return full.substring(0, 2);
}

function getFullSidoName(short) {
    const map = {
        '서울': '서울특별시',
        '부산': '부산광역시',
        '대구': '대구광역시',
        '인천': '인천광역시',
        '광주': '광주광역시',
        '대전': '대전광역시',
        '울산': '울산광역시',
        '경기': '경기도'
    };
    return map[short] || short;
}