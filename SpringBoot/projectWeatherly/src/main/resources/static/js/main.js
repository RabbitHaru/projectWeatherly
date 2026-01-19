/**
 * main.js - 메인 대시보드 전용 스크립트
 */

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

    // current-temp 요소가 있을 때만 실행 (메인 페이지인지 확인)
    if (document.getElementById('current-temp')) {
        loadDashboardData();
        setInterval(loadDashboardData, 300000);
    }

    bindGpsButton('gps-sync-btn', async (lat, lng) => {
        await loadWeatherDataByGPS(lat, lng);
    });
});

async function loadDashboardData() {
    try {
        showLoading();
        await Promise.all([
            loadWeatherData(),
            loadAirQualitySummary(),
            loadRegionalWeatherData(),
            loadCommunityData()
        ]);
        hideLoading();
    } catch (error) {
        console.error('대시보드 로드 실패:', error);
        hideLoading();
    }
}

// [1] 날씨 API
async function loadWeatherData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await response.json();
        if (data.success) updateWeatherUI(data.data);
    } catch (error) {
        console.error('날씨 로드 오류', error);
        document.getElementById('current-temp').innerHTML = '--';
    }
}

async function loadWeatherDataByGPS(lat, lng) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${lat}&longitude=${lng}`, {
            method: 'POST', headers: {'Content-Type': 'application/json'}
        });
        const data = await response.json();
        if (data.success) {
            updateWeatherUI(data.data);
            loadAirQualitySummaryByGPS(lat, lng);
        }
    } catch (e) {
        console.error(e);
        throw e;
    }
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

    if (weather.regionName) txt('current-location', weather.regionName);

    if (weather.current) {
        const cur = weather.current;
        html('current-temp', `${Math.round(cur.temperature)}<span class="temp-unit">°C</span>`);
        txt('weather-condition', cur.weatherCondition || '--');
        txt('feels-like', `${Math.round(cur.feelsLike)}°C`);
        txt('wind-speed', `${cur.windSpeed?.toFixed(1) || '--'} m/s`);
        txt('humidity', `${Math.round(cur.humidity) || '--'}%`);
        txt('precipitation', `${cur.precipitation || '0'} mm`);
    }

    if (weather.hourly) renderHourlyForecast(weather.hourly);
    if (weather.tomorrowHourly) renderTomorrowForecast(weather.tomorrowHourly);
    if (weather.daily) renderWeeklyForecast(weather.daily);

    updateForecastSummaries(weather);
}

// [2] 예보 렌더링
function renderHourlyForecast(data) {
    const container = document.getElementById('hourly-forecast');
    if (!container) return;
    container.innerHTML = '';

    data.slice(0, 24).forEach(item => {
        const div = document.createElement('div');
        div.className = 'hour-item';
        let pop = '';
        if (item.precipitationProbability > 0) pop = `<div class="hour-pop">${Math.round(item.precipitationProbability)}%</div>`;

        div.innerHTML = `
            <div class="hour-time">${item.time}</div>
            <div class="hour-icon"><i class="${item.weatherIcon || 'fas fa-sun'}"></i></div>
            <div class="hour-temp">${Math.round(item.temperature)}°</div>
            ${pop}
            <div class="hour-humidity">${Math.round(item.humidity)}%</div>
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

function renderTomorrowForecast(data) {
    const container = document.getElementById('tomorrow-forecast');
    if (!container) return;
    container.innerHTML = '';

    const filtered = data.filter((d, i) => i % 2 === 0).slice(0, 12);
    filtered.forEach(item => {
        const div = document.createElement('div');
        div.className = 'tomorrow-hour-item';
        let pop = '';
        if (item.precipitationProbability > 0) pop = `<div class="tomorrow-pop">${Math.round(item.precipitationProbability)}%</div>`;

        div.innerHTML = `
            <div class="tomorrow-time">${item.time}</div>
            <div class="tomorrow-icon"><i class="${item.weatherIcon || 'fas fa-sun'}"></i></div>
            <div class="tomorrow-temp">${Math.round(item.temperature)}°</div>
            ${pop}
            <div class="tomorrow-wind">${item.windSpeed?.toFixed(1) || '-'}m/s</div>
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

function renderWeeklyForecast(data) {
    const container = document.getElementById('weekly-forecast');
    if (!container) return;
    container.innerHTML = '';

    data.slice(0, 7).forEach(day => {
        const div = document.createElement('div');
        div.className = 'day-item';
        let pop = '';
        if (day.precipitationProbability > 0) pop = `<div class="day-pop"><i class="fas fa-umbrella"></i> ${Math.round(day.precipitationProbability)}%</div>`;

        div.innerHTML = `
            <div class="day-header">
                <div class="day-name">${day.dayOfWeek}</div>
                <div class="day-date">${day.date}</div>
            </div>
            <div class="day-temps">
                <div class="temp-am">
                    <span class="temp-label">오전</span>
                    <div class="temp-icon"><i class="${day.nightIcon || 'fas fa-moon'}"></i></div>
                    <span class="temp-value">${Math.round(day.minTemp)}°</span>
                </div>
                <div class="temp-pm">
                    <span class="temp-label">오후</span>
                    <div class="temp-icon"><i class="${day.dayIcon || 'fas fa-sun'}"></i></div>
                    <span class="temp-value">${Math.round(day.maxTemp)}°</span>
                </div>
            </div>
            ${pop}
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

// [3] 요약
function updateForecastSummaries(weather) {
    const txt = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };

    if (weather.hourly && weather.hourly.length > 0) {
        const next = weather.hourly[0];
        txt('ultra-short-summary', `앞으로 6시간 동안 ${next.weatherCondition} 날씨가 이어질 전망입니다.`);
        txt('ultra-short-temp', `${Math.round(next.temperature)}°C`);
        txt('ultra-short-humidity', `${Math.round(next.humidity)}%`);
    }

    if (weather.daily && weather.daily.length > 1) {
        const tomorrow = weather.daily[1];
        txt('short-term-summary', `내일은 최고 ${Math.round(tomorrow.maxTemp)}도까지 오르겠습니다.`);
        txt('short-term-max-temp', `${Math.round(tomorrow.maxTemp)}°C`);
        txt('short-term-min-temp', `${Math.round(tomorrow.minTemp)}°C`);
    }

    txt('mid-term-summary', '당분간 큰 기온 변화 없이 평년과 비슷하겠습니다.');
    txt('weekly-precipitation', '보통');
    txt('temp-trend', '유지');
}

// [4] 대기질 (메인용)
async function loadAirQualitySummary() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) {
            updateAqiSummaryUI(data.data);
            const sido = data.data.sidoName || '서울';
            loadAirQualityForecast(sido);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadAirQualitySummaryByGPS(lat, lng) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();
        if (data.success && data.data) {
            updateAqiSummaryUI(data.data);
            const sido = data.data.sidoName || '서울';
            loadAirQualityForecast(sido);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadAirQualityForecast(sido) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) renderAirQualityForecast(data.data);
    } catch (e) {
        console.error(e);
    }
}

function updateAqiSummaryUI(aqi) {
    const badge = document.getElementById('aqi-overall');
    if (badge) {
        badge.textContent = aqi.overallStatus;
        badge.className = 'aqi-badge ' + getAqiClass(aqi.overallGrade);
    }
    const units = {'pm10': 'µg/m³', 'pm25': 'µg/m³', 'o3': 'ppm'};
    ['pm10', 'pm25', 'o3'].forEach(key => {
        if (aqi[key]) {
            const elVal = document.getElementById(`${key}-value`);
            const elStat = document.getElementById(`${key}-status`);
            if (elVal) elVal.textContent = `${aqi[key].value} ${units[key]}`;
            if (elStat) {
                elStat.textContent = aqi[key].status;
                elStat.className = 'aqi-status ' + getAqiClass(aqi[key].grade);
            }
        }
    });
}

function renderAirQualityForecast(list) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;
    container.innerHTML = '';

    if (!list || list.length === 0) {
        container.innerHTML = '<div class="no-data">예보 정보 없음</div>';
        return;
    }

    list.forEach(item => {
        const div = document.createElement('div');
        div.className = 'aqi-forecast-item';
        div.innerHTML = `
            <div class="aqi-forecast-header" style="margin-bottom:10px;"><h5>${item.date}</h5></div>
            <div class="aqi-forecast-icon" style="font-size:2rem; margin:10px 0;">${getAqiIcon(item.overallGrade)}</div>
            <div class="aqi-forecast-value" style="font-size:1.1rem; font-weight:bold; margin-bottom:5px;">${getAqiStatusText(item.overallGrade)}</div>
            <div class="aqi-forecast-status ${getAqiClass(item.overallGrade)}" style="padding:4px 10px; border-radius:15px; color:white; font-size:0.8rem; display:inline-block;">통합 ${item.overallGrade}등급</div>
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

// [5] 지역별 날씨
async function loadRegionalWeatherData() {
    const container = document.getElementById('regional-weather');
    if (!container) return;

    container.innerHTML = '<div style="text-align:center; padding:20px;"><i class="fas fa-spinner fa-spin"></i> 로딩중...</div>';

    const regions = [
        {name: '서울', code: '1100000000'}, {name: '부산', code: '2600000000'},
        {name: '대구', code: '2700000000'}, {name: '인천', code: '2800000000'},
        {name: '광주', code: '2900000000'}, {name: '대전', code: '3000000000'},
        {name: '울산', code: '3100000000'}
    ];
    const regionCodes = regions.map(r => r.code).join(',');

    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/compare?regionCodes=${regionCodes}`);
        const result = await response.json();

        if (result.success && result.data) {
            container.innerHTML = '';
            regions.forEach(target => {
                const data = result.data.find(d => d.regionCode === target.code);
                if (data) {
                    const div = document.createElement('div');
                    div.className = 'region-weather';
                    const temp = data.current ? Math.round(data.current.temperature) : '--';
                    const cond = data.current ? data.current.weatherCondition : '--';
                    div.innerHTML = `
                        <div class="region-info"><span class="region-name">${target.name}</span><span class="region-weather-desc">${cond}</span></div>
                        <div class="region-temp">${temp}°C</div>
                    `;
                    container.appendChild(div);
                }
            });
        }
    } catch (e) {
        console.error('지역 날씨 실패', e);
        container.innerHTML = '<div class="no-data">로드 실패</div>';
    }
}

// [6] 커뮤니티
async function loadCommunityData() {
    const container = document.getElementById('community-posts');
    if (!container) return;

    const posts = [
        {title: '오늘 날씨 정말 좋네요! 산책하기 딱 좋아요', cat: 'weather-talk', time: '10분 전', author: '날씨매니아'},
        {title: '이번 주말에 캠핑 갈 건데 옷차림 조언 부탁드려요', cat: 'outfit', time: '30분 전', author: '캠핑러버'},
        {title: '미세먼지 심한 날 실내 운동 추천합니다', cat: 'dust', time: '1시간 전', author: '건강관리'},
        {title: '내일 비온다는데 우산 꼭 챙기세요!', cat: 'weather-talk', time: '2시간 전', author: '우산챙기자'}
    ];

    container.innerHTML = '';
    posts.forEach(p => {
        const div = document.createElement('div');
        div.className = 'post-item';
        div.innerHTML = `
            <div class="post-header"><span class="post-category" style="background:#eee; padding:3px 8px; border-radius:10px; font-size:0.8rem;">${p.cat}</span><span class="post-time" style="font-size:0.8rem; color:#888;">${p.time}</span></div>
            <h4 class="post-title" style="font-size:1rem; margin:5px 0;">${p.title}</h4>
            <div class="post-meta" style="font-size:0.8rem; color:#888;"><i class="fas fa-user"></i> ${p.author}</div>
        `;
        container.appendChild(div);
    });
}

function zoomInMap() {
    alert('지도 확대');
}

function zoomOutMap() {
    alert('지도 축소');
}

function refreshMap() {
    alert('지도 새로고침');
}