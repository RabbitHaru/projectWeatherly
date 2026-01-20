/**
 * main.js - 메인 대시보드 (기존 HTML 구조 호환 패치)
 */

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

    if (document.getElementById('current-temp')) {
        loadDashboardData();
        setInterval(loadDashboardData, 300000);
    }

    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', async (lat, lng) => {
            await loadWeatherDataByGPS(lat, lng);
        });
    }
});

async function loadDashboardData() {
    try {
        await Promise.all([
            loadWeatherData(),
            loadAirQualitySummary(),
            loadRegionalWeatherData(),
            loadCommunityData()
        ]);
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    }
}

async function loadWeatherData() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await res.json();
        if (data.success) {
            updateWeatherUI(data.data);
            // 날씨 로드 시 지역명 기반으로 미세먼지 예보도 로드
            if (data.data.regionName) loadAirQualityForecast(data.data.regionName);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadWeatherDataByGPS(lat, lng, forcedRegionName = null) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${lat}&longitude=${lng}`, {
            method: 'POST', headers: {'Content-Type': 'application/json'}
        });
        const data = await res.json();
        if (data.success) {
            if (forcedRegionName) data.data.regionName = forcedRegionName;

            updateWeatherUI(data.data);
            loadAirQualitySummaryByGPS(lat, lng);

            // GPS 위치의 미세먼지 예보 로드
            const sido = data.data.regionName ? extractSidoName(data.data.regionName) : '서울';
            loadAirQualityForecast(sido);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadAirQualitySummary() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) {
            updateAqiSummaryUI(data.data);
            // 실시간 데이터 로드 시에도 예보 로드 시도 (보완)
            if (data.data.sidoName) loadAirQualityForecast(data.data.sidoName);
        }
    } catch (e) {
        console.error("대기질 조회 실패:", e);
    }
}

async function loadAirQualitySummaryByGPS(lat, lng) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();
        if (data.success && data.data) updateAqiSummaryUI(data.data);
    } catch (e) {
        console.error("GPS 대기질 조회 실패:", e);
    }
}

// [핵심] 미세먼지 예보 로드 (기존 HTML ID인 aqi-forecast-details 사용)
async function loadAirQualityForecast(sido) {
    if (!sido || sido.includes('?')) sido = '서울';
    sido = extractSidoName(sido);

    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) {
            updateMainPageAqiForecast(data.data);
        }
    } catch (e) {
        console.error("예보 로딩 실패:", e);
    }
}

// [수정됨] 메인 페이지 HTML 구조에 맞춰 렌더링
function updateMainPageAqiForecast(list) {
    // 1. 이미 존재하는 컨테이너 찾기
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;

    container.innerHTML = ''; // 초기화

    // [스타일 강제 적용] 부모 컨테이너가 꽉 차게 설정
    container.style.display = 'flex';
    container.style.width = '100%';
    container.style.gap = '15px'; // 사이 간격
    container.style.justifyContent = 'space-between';

    if (!list || !Array.isArray(list) || list.length === 0) {
        container.innerHTML = '<div class="no-data" style="padding:10px; width:100%; text-align:center;">정보 없음</div>';
        return;
    }

    // 2. 오늘/내일 데이터를 카드 형태로 생성
    const labels = ['오늘 예보', '내일 예보'];

    list.forEach((item, index) => {
        if (index > 1) return; // 최대 2개까지만 표시

        const gradeClass = getAqiClass(item.overallGrade);
        const statusText = getAqiStatusText(item.overallGrade);
        const iconHtml = getAqiIcon(item.overallGrade);
        const label = labels[index] || '예보';

        // [핵심 수정] style="flex: 1;" 추가하여 공간을 균등하게 차지하도록 함
        const html = `
            <div class="aqi-forecast-item" style="flex: 1; padding: 20px 15px; background: white; border-radius: 12px; text-align: center; box-shadow: 0 2px 5px rgba(0,0,0,0.05); border: 1px solid #eee;">
                <div style="font-weight: bold; font-size: 1rem; color: #555; margin-bottom: 8px;">${label}</div>
                <div style="font-size: 2.5rem; margin: 10px 0;">${iconHtml}</div>
                <div class="aqi-badge ${gradeClass}" style="font-size: 0.9rem; padding: 5px 12px; margin-bottom: 5px;">${statusText}</div>
                <div style="font-size: 0.85rem; color: #999; margin-top: 8px;">${item.date}</div>
            </div>
        `;
        container.innerHTML += html;
    });
}

function updateAqiSummaryUI(aqi) {
    if (!aqi) return;
    const badge = document.getElementById('aqi-overall');
    if (badge) {
        badge.textContent = aqi.overallStatus || '--';
        badge.className = 'aqi-badge ' + getAqiClass(aqi.overallGrade);
    }
    const updateItem = (key, unit) => {
        if (aqi[key]) {
            const elVal = document.getElementById(`${key}-value`);
            const elStat = document.getElementById(`${key}-status`);
            if (elVal) elVal.textContent = `${aqi[key].value || '-'} ${unit}`;
            if (elStat) {
                elStat.textContent = aqi[key].status || '-';
                elStat.className = 'aqi-status ' + getAqiClass(aqi[key].grade);
            }
        }
    };
    updateItem('pm10', 'µg/m³');
    updateItem('pm25', 'µg/m³');
    updateItem('o3', 'ppm');
}

function updateWeatherUI(weather) {
    if (!weather) return;
    const txt = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };
    const html = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = val;
    };

    if (weather.regionName) txt('current-location', getFullSidoName(weather.regionName));

    if (weather.current) {
        html('current-temp', `${Math.round(weather.current.temperature)}<span class="temp-unit">°C</span>`);
        txt('weather-condition', weather.current.weatherCondition || '맑음');
        txt('feels-like', `${Math.round(weather.current.feelsLike)}°C`);
        txt('wind-speed', `${weather.current.windSpeed?.toFixed(1) || '0'} m/s`);
        txt('humidity', `${Math.round(weather.current.humidity) || '0'}%`);
        txt('precipitation', `${weather.current.precipitation || '0'} mm`);
    }
    if (weather.hourly) renderHourlyForecast(weather.hourly);
    if (weather.tomorrowHourly) renderTomorrowForecast(weather.tomorrowHourly);
    if (weather.daily) renderWeeklyForecast(weather.daily);
    updateForecastSummaries(weather);
}

function updateForecastSummaries(weather) {
    const txt = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };
    if (weather.hourly && weather.hourly.length > 0) {
        txt('ultra-short-summary', `현재 ${weather.hourly[0].weatherCondition}, 기온 ${Math.round(weather.hourly[0].temperature)}°C 입니다.`);
        txt('ultra-short-temp', `${Math.round(weather.hourly[0].temperature)}°C`);
        txt('ultra-short-humidity', `${Math.round(weather.hourly[0].humidity)}%`);
    }
    if (weather.daily && weather.daily.length > 1) {
        const tmr = weather.daily[1];
        txt('short-term-summary', `내일은 ${tmr.weatherCondition || '맑음'}이 예상됩니다.`);
        txt('short-term-max-temp', `${Math.round(tmr.maxTemp)}°C`);
        txt('short-term-min-temp', `${Math.round(tmr.minTemp)}°C`);
    }
    if (weather.daily && weather.daily.length > 2) {
        const wk = weather.daily[2];
        txt('mid-term-summary', `주간 기온은 ${Math.round(wk.minTemp)}~${Math.round(wk.maxTemp)}°C 사이를 유지하겠습니다.`);
        txt('weekly-precipitation', `${wk.precipitationProbability || 0}%`);
        txt('temp-trend', '평년 비슷');
    }
}

function renderHourlyForecast(data) {
    const container = document.getElementById('hourly-forecast');
    if (!container) return;
    container.innerHTML = '';
    data.slice(0, 24).forEach(item => {
        container.innerHTML += `<div class="hour-item"><div class="hour-time">${item.time}</div><div class="hour-icon"><i class="${item.weatherIcon || 'fas fa-sun'}"></i></div><div class="hour-temp">${Math.round(item.temperature)}°</div></div>`;
    });
}

function renderTomorrowForecast(data) {
    const container = document.getElementById('tomorrow-forecast');
    if (!container) return;
    container.innerHTML = '';
    data.filter((d, i) => i % 2 === 0).slice(0, 12).forEach(item => {
        container.innerHTML += `<div class="tomorrow-hour-item"><div class="tomorrow-time">${item.time}</div><div class="tomorrow-icon"><i class="${item.weatherIcon || 'fas fa-sun'}"></i></div><div class="tomorrow-temp">${Math.round(item.temperature)}°</div></div>`;
    });
}

function renderWeeklyForecast(data) {
    const container = document.getElementById('weekly-forecast');
    if (!container) return;
    container.innerHTML = '';
    data.slice(0, 7).forEach(day => {
        const amIcon = (day.nightIcon && day.nightIcon.trim()) ? day.nightIcon : 'fas fa-moon';
        const pmIcon = (day.dayIcon && day.dayIcon.trim()) ? day.dayIcon : 'fas fa-sun';
        container.innerHTML += `<div class="day-item"><div class="day-header"><div class="day-name">${day.dayOfWeek}</div><div class="day-date">${day.date}</div></div><div class="day-temps"><div class="temp-am"><span class="temp-label">오전</span><div class="temp-icon"><i class="${amIcon}"></i></div><span class="temp-value">${Math.round(day.minTemp)}°</span></div><div class="temp-pm"><span class="temp-label">오후</span><div class="temp-icon"><i class="${pmIcon}"></i></div><span class="temp-value">${Math.round(day.maxTemp)}°</span></div></div></div>`;
    });
}

async function loadRegionalWeatherData() {
    const container = document.getElementById('regional-weather');
    if (!container) return;
    const regions = [{name: '서울', code: '1100000000'}, {name: '부산', code: '2600000000'}, {
        name: '대구',
        code: '2700000000'
    }, {name: '광주', code: '2900000000'}, {name: '대전', code: '3000000000'}];
    const codes = regions.map(r => r.code).join(',');
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/compare?regionCodes=${codes}`);
        const data = await res.json();
        if (data.success && data.data) {
            container.innerHTML = '';
            regions.forEach(r => {
                const item = data.data.find(d => d.regionCode === r.code);
                if (item) {
                    const div = document.createElement('div');
                    div.className = 'region-weather';
                    div.innerHTML = `<div class="region-info"><span class="region-name">${r.name}</span><span class="region-weather-desc">${item.current.weatherCondition}</span></div><div class="region-temp">${Math.round(item.current.temperature)}°</div>`;

                    div.addEventListener('click', () => {
                        document.querySelectorAll('.region-weather').forEach(el => el.classList.remove('selected'));
                        div.classList.add('selected');
                        const coords = CITY_COORDINATES[r.name];
                        if (coords) loadWeatherDataByGPS(coords.lat, coords.lng, getFullSidoName(r.name));
                    });

                    container.appendChild(div);
                }
            });
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadCommunityData() {
    const container = document.getElementById('community-posts');
    if (container) container.innerHTML = `<div class="post-item"><h4 class="post-title">오늘 날씨 정말 좋네요!</h4></div><div class="post-item"><h4 class="post-title">주말 등산 가실 분?</h4></div>`;
}

function extractSidoName(full) {
    if (!full) return '서울';
    if (full.length === 2) return full;
    const shortName = full.substring(0, 2);
    return shortName;
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