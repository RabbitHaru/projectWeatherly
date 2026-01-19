/**
 * main.js - 메인 대시보드
 */

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

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
            loadCommunityData(),
            loadAirQualityForecast() // [추가] 미세먼지 예보 로딩
        ]);
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    } finally {
        hideLoading();
    }
}

async function loadWeatherData() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await res.json();
        if (data.success) updateWeatherUI(data.data);
    } catch (e) {
        console.error(e);
    }
}

async function loadWeatherDataByGPS(lat, lng) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${lat}&longitude=${lng}`, {
            method: 'POST', headers: {'Content-Type': 'application/json'}
        });
        const data = await res.json();
        if (data.success) {
            updateWeatherUI(data.data);
            loadAirQualitySummaryByGPS(lat, lng);
        }
    } catch (e) {
        console.error(e);
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
    } else {
        txt('short-term-summary', '정보 준비 중');
    }

    if (weather.daily && weather.daily.length > 2) {
        const wk = weather.daily[2];
        txt('mid-term-summary', `주간 기온은 ${Math.round(wk.minTemp)}~${Math.round(wk.maxTemp)}°C 사이를 유지하겠습니다.`);
        txt('weekly-precipitation', `${wk.precipitationProbability || 0}%`);
        txt('temp-trend', '평년 비슷');
    } else {
        txt('mid-term-summary', '정보 준비 중');
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
        container.innerHTML += `
            <div class="day-item">
                <div class="day-header"><div class="day-name">${day.dayOfWeek}</div><div class="day-date">${day.date}</div></div>
                <div class="day-temps">
                    <div class="temp-am"><span class="temp-label">오전</span><div class="temp-icon"><i class="${amIcon}"></i></div><span class="temp-value">${Math.round(day.minTemp)}°</span></div>
                    <div class="temp-pm"><span class="temp-label">오후</span><div class="temp-icon"><i class="${pmIcon}"></i></div><span class="temp-value">${Math.round(day.maxTemp)}°</span></div>
                </div>
            </div>`;
    });
}

async function loadAirQualitySummary() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) updateAqiSummaryUI(data.data);
    } catch (e) {
        console.error(e);
    }
}

async function loadAirQualitySummaryByGPS(lat, lng) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();
        if (data.success && data.data) updateAqiSummaryUI(data.data);
    } catch (e) {
        console.error(e);
    }
}

function updateAqiSummaryUI(aqi) {
    const badge = document.getElementById('aqi-overall');
    if (badge) {
        badge.textContent = aqi.overallStatus || '--';
        badge.className = 'aqi-badge ' + getAqiClass(aqi.overallGrade);
    }
    ['pm10', 'pm25', 'o3'].forEach(key => {
        if (aqi[key]) {
            const elVal = document.getElementById(`${key}-value`);
            const elStat = document.getElementById(`${key}-status`);
            const unit = key === 'o3' ? 'ppm' : 'µg/m³';
            if (elVal) elVal.textContent = `${aqi[key].value || '--'} ${unit}`;
            if (elStat) {
                elStat.textContent = aqi[key].status || '--';
                elStat.className = 'aqi-status ' + getAqiClass(aqi[key].grade);
            }
        }
    });
}

// [추가] 미세먼지 예보 로딩 함수
async function loadAirQualityForecast() {
    try {
        // 지역명은 기본 '서울'로 요청 (예보 API는 전국 데이터를 주므로 파라미터는 형식상 필요)
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/서울`);
        const data = await res.json();
        if (data.success && data.data) {
            renderAqiForecast(data.data);
        }
    } catch (e) {
        console.error("미세먼지 예보 로드 실패:", e);
    }
}

// [추가] 미세먼지 예보 렌더링 함수
function renderAqiForecast(list) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;
    container.innerHTML = '';

    list.forEach(item => {
        container.innerHTML += `
            <div class="aqi-forecast-item">
                <div class="forecast-date" style="font-weight:bold; margin-bottom:5px;">${item.date}</div>
                <div style="font-size:2rem; margin:10px 0;">${getAqiIcon(item.overallGrade)}</div>
                <div class="aqi-badge ${getAqiClass(item.overallGrade)}" style="margin-bottom:10px;">${getAqiStatusText(item.overallGrade)}</div>
                <div style="font-size:0.85rem; color:#666; padding:0 10px;">
                    ${item.advice ? item.advice.substring(0, 30) + '...' : '정보 없음'}
                </div>
            </div>
        `;
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
                if (item) container.innerHTML += `<div class="region-weather"><div class="region-info"><span class="region-name">${r.name}</span><span class="region-weather-desc">${item.current.weatherCondition}</span></div><div class="region-temp">${Math.round(item.current.temperature)}°</div></div>`;
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