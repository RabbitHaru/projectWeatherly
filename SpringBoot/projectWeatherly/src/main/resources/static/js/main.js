/**
 * main.js - 메인 대시보드 로직 (Full Version)
 * - 시간별 예보 48시간 통합 (오늘+내일)
 * - 날짜 구분선 표시 (오늘/내일/모레)
 * - 기상특보 로직 포함
 * - 탭 정렬 및 위치 저장(sessionStorage) 적용
 */

let kakaoMap = null;
let mapOverlays = {};

document.addEventListener('DOMContentLoaded', function () {
    // 0. 날짜 구분선 스타일 동적 추가 (CSS 파일 수정 없이도 작동하게 안전장치)
    addHourlyMarkerStyle();

    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

    // 저장된 위치 확인 및 로드
    initializeLocation();

    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', async (lat, lng) => {
            await loadWeatherDataByGPS(lat, lng);
        });
    }

    checkKakaoMapLoop();
});

// 구분선 스타일 추가
function addHourlyMarkerStyle() {
    const style = document.createElement('style');
    style.innerHTML = `
        .hourly-date-marker {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            min-width: 60px;
            margin: 0 8px;
            padding: 0 10px;
            background: rgba(128, 128, 128, 0.1);
            border-radius: 12px;
            font-weight: bold;
            color: #333;
            font-size: 0.9rem;
            text-align: center;
            border: 1px solid rgba(0,0,0,0.05);
            flex-shrink: 0;
        }
        body.dark-mode .hourly-date-marker {
            background: rgba(255, 255, 255, 0.1);
            color: #eee;
        }
        .hourly-date-marker .marker-day { font-size: 1.1em; margin-bottom: 2px; color: var(--primary-color, #3498db); }
        .hourly-date-marker .marker-date { font-size: 0.75em; opacity: 0.7; }
    `;
    document.head.appendChild(style);
}

// 초기화
async function initializeLocation() {
    if (!document.getElementById('current-temp')) return;
    const saved = RegionManager.load();
    if (saved) {
        console.log(`📍 저장된 위치 로드: ${saved.name}`);
        await loadWeatherDataByGPS(saved.lat, saved.lng, saved.name);
        loadAirQualitySummaryByGPS(saved.lat, saved.lng);
        loadRegionalWeatherData();
        loadCommunityData();
    } else {
        loadDashboardData();
    }
}

window.changeDashboardLocation = async function (lat, lng, name) {
    console.log(`지역 변경: ${name} (${lat}, ${lng})`);
    RegionManager.save(name, lat, lng);
    const locationTitle = document.getElementById('current-location');
    if (locationTitle) locationTitle.innerText = `${name}로 이동 중...`;
    if (kakaoMap) {
        const moveLatLon = new kakao.maps.LatLng(lat, lng);
        kakaoMap.panTo(moveLatLon);
    }
    await loadWeatherDataByGPS(lat, lng, name);
    window.scrollTo({top: 0, behavior: 'smooth'});
};

function checkKakaoMapLoop() {
    if (window.kakao && window.kakao.maps) {
        kakao.maps.load(() => initKakaoMap());
    } else {
        setTimeout(checkKakaoMapLoop, 500);
    }
}

async function loadDashboardData() {
    try {
        if (typeof showLoading === 'function') showLoading();
        await Promise.all([
            loadWeatherData(),
            loadAirQualitySummary(),
            loadRegionalWeatherData(),
            loadCommunityData()
        ]);
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    } finally {
        if (typeof hideLoading === 'function') hideLoading();
    }
}

function initKakaoMap() {
    const container = document.getElementById('kakao-map');
    if (!container) return;
    let centerLat = 36.3, centerLng = 127.8;
    const saved = RegionManager.load();
    if (saved && saved.lat && saved.lng) {
        centerLat = saved.lat;
        centerLng = saved.lng;
    }
    const options = {
        center: new kakao.maps.LatLng(centerLat, centerLng),
        level: 13, draggable: true, scrollwheel: true
    };
    kakaoMap = new kakao.maps.Map(container, options);
    loadRegionalWeatherData();
}

async function loadRegionalWeatherData() {
    const listContainer = document.getElementById('regional-weather');
    const regions = ALL_REGIONS.map(r => ({
        ...r,
        showOnMap: ['서울', '부산', '대구', '광주', '대전', '강원', '제주', '독도'].includes(r.name)
    }));
    const regionCodes = regions.filter(r => r.code).map(r => r.code).join(',');

    try {
        let weatherData = [];
        if (regionCodes) {
            const res = await fetch(`${API_BASE_URL}/api/weather/compare?regionCodes=${regionCodes}`);
            const result = await res.json();
            if (result.success) weatherData = result.data;
        }
        if (listContainer) listContainer.innerHTML = '';
        regions.forEach(region => {
            let temp = '--';
            let cond = '로딩중';
            let iconClass = 'fas fa-spinner fa-spin';
            if (region.name === '독도') {
                temp = '15';
                cond = '맑음';
                iconClass = 'fas fa-flag';
            } else {
                const data = weatherData.find(d => d.regionCode === region.code);
                if (data && data.current) {
                    temp = Math.round(data.current.temperature);
                    cond = data.current.weatherCondition;
                    iconClass = getWeatherIconClass(cond);
                }
            }
            const clickAction = `onclick="changeDashboardLocation(${region.lat}, ${region.lng}, '${region.name}')"`;
            if (kakaoMap && region.showOnMap) {
                const content = `<div class="customoverlay" ${clickAction} style="cursor: pointer;"><a href="javascript:void(0);"><span class="title">${region.name}</span><div class="weather-content"><i class="${iconClass}" style="color:${getIconColor(iconClass)}"></i><span class="temp">${temp}°</span></div></a></div>`;
                const position = new kakao.maps.LatLng(region.lat, region.lng);
                if (mapOverlays[region.name]) mapOverlays[region.name].setMap(null);
                const customOverlay = new kakao.maps.CustomOverlay({
                    map: kakaoMap,
                    position: position,
                    content: content,
                    yAnchor: 1
                });
                mapOverlays[region.name] = customOverlay;
            }
            if (listContainer && region.code) {
                listContainer.innerHTML += `<div class="region-weather" ${clickAction} style="cursor: pointer;"><div class="region-info"><span class="region-name">${region.name}</span><span class="region-weather-desc">${cond}</span></div><div class="region-temp">${temp}°</div></div>`;
            }
        });
    } catch (e) {
        console.error('지역 날씨 로드 실패', e);
    }
}

function getIconColor(iconClass) {
    if (iconClass.includes('sun')) return '#f39c12';
    if (iconClass.includes('rain') || iconClass.includes('umbrella')) return '#3498db';
    if (iconClass.includes('cloud')) return '#7f8c8d';
    if (iconClass.includes('flag')) return '#e74c3c';
    return '#333';
}

function getWeatherIconClass(condition) {
    if (!condition) return 'fas fa-question';
    if (condition.includes('맑음')) return 'fas fa-sun';
    if (condition.includes('구름') || condition.includes('흐림')) return 'fas fa-cloud';
    if (condition.includes('비')) return 'fas fa-umbrella';
    if (condition.includes('눈')) return 'fas fa-snowflake';
    return 'fas fa-cloud-sun';
}

async function loadWeatherData() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await res.json();
        if (data.success) {
            updateWeatherUI(data.data);
            if (data.data.regionName) loadAirQualityForecast(data.data.regionName);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadWeatherDataByGPS(lat, lng, forcedRegionName = null) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${lat}&longitude=${lng}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });
        const data = await res.json();
        if (data.success) {
            if (forcedRegionName) data.data.regionName = forcedRegionName;
            updateWeatherUI(data.data);
            loadAirQualitySummaryByGPS(lat, lng);
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

async function loadAirQualityForecast(sido) {
    if (!sido || sido.includes('?')) sido = '서울';
    sido = extractSidoName(sido);
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) updateMainPageAqiForecast(data.data);
    } catch (e) {
        console.error("예보 로딩 실패:", e);
    }
}

function updateMainPageAqiForecast(list) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;
    container.innerHTML = '';

    if (!list || !Array.isArray(list) || list.length === 0) {
        container.innerHTML = '<div class="no-data" style="padding:20px; width:100%; text-align:center; color:var(--light-text);">예보 정보 없음</div>';
        return;
    }
    list.sort((a, b) => a.date.localeCompare(b.date));
    const today = new Date();
    const krNow = new Date(today.getTime() + (9 * 60 * 60 * 1000));
    const todayStr = krNow.toISOString().split('T')[0];
    const tomorrow = new Date(krNow);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    const todayData = list.find(item => item.date === todayStr) || list[0];
    const tomorrowData = list.find(item => item.date === tomorrowStr) || list[1];
    const targetItems = [];
    if (todayData) targetItems.push({label: '오늘 예보', data: todayData});
    if (tomorrowData) targetItems.push({label: '내일 예보', data: tomorrowData});
    targetItems.forEach(item => {
        const data = item.data;
        const gradeClass = getAqiClass(data.overallGrade);
        const statusText = getAqiStatusText(data.overallGrade);
        const iconHtml = getAqiIcon(data.overallGrade);
        container.innerHTML += `<div class="aqi-forecast-card"><div class="aqi-card-header"><span class="aqi-label">${item.label}</span><span class="aqi-date">${data.date}</span></div><div class="aqi-card-body"><div class="aqi-icon">${iconHtml}</div><div class="aqi-status-badge ${gradeClass}">${statusText}</div></div></div>`;
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

    if (weather.regionName) {
        const titleName = getFullSidoName(weather.regionName) || '실시간 날씨';
        document.title = `${titleName} - Weatherly`;
        const locationEl = document.getElementById('current-location');
        if (locationEl) {
            let name = getFullSidoName(weather.regionName);
            if (weather.isMock) name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px; vertical-align:middle; margin-left: 5px;">TEST MODE</span>';
            locationEl.innerHTML = name;
        }
    }

    // ⭐ [기상특보 로직 복구]
    const alertTitle = document.getElementById('weather-alert-title');
    const alertDesc = document.getElementById('weather-alert-desc');
    const iconEl = document.querySelector('.warning-status .status-icon');

    if (weather.warnings && weather.warnings.length > 0) {
        const activeWarnings = weather.warnings.filter(w => w.active);
        if (activeWarnings.length > 0) {
            if (alertTitle) alertTitle.textContent = activeWarnings.map(w => w.title).join(', ');
            if (alertDesc) alertDesc.textContent = `${weather.regionName} 지역에 기상특보가 발령 중입니다.`;

            const isDanger = activeWarnings.some(w => w.level === 'danger');
            if (iconEl) {
                iconEl.className = `fas status-icon ${isDanger ? 'fa-exclamation-circle' : 'fa-exclamation-triangle'}`;
                if (isDanger) {
                    iconEl.classList.add('danger');
                    iconEl.style.color = '#e74c3c';
                } else {
                    iconEl.classList.add('caution');
                    iconEl.style.color = '#f1c40f';
                }
            }
        } else {
            setNoWarningUI(alertTitle, alertDesc, iconEl);
        }
    } else if (weather.warning) {
        if (alertTitle) alertTitle.textContent = weather.warning.title;
        if (alertDesc) alertDesc.textContent = weather.warning.description;
        if (iconEl) {
            iconEl.className = 'fas status-icon';
            if (weather.warning.level === 'danger') {
                iconEl.classList.add('fa-exclamation-circle', 'danger');
                iconEl.style.color = '#e74c3c';
            } else if (weather.warning.level === 'caution') {
                iconEl.classList.add('fa-exclamation-triangle', 'caution');
                iconEl.style.color = '#f1c40f';
            } else {
                iconEl.classList.add('fa-check-circle', 'safe');
                iconEl.style.color = '#2ecc71';
            }
        }
    } else {
        setNoWarningUI(alertTitle, alertDesc, iconEl);
    }

    if (weather.current) {
        html('current-temp', `${Math.round(weather.current.temperature)}<span class="temp-unit">°C</span>`);
        txt('weather-condition', weather.current.weatherCondition || '맑음');
        txt('feels-like', `${Math.round(weather.current.feelsLike)}°C`);
        txt('wind-speed', `${weather.current.windSpeed?.toFixed(1) || '0'} m/s`);
        txt('humidity', `${Math.round(weather.current.humidity) || '0'}%`);
        txt('precipitation', `${weather.current.precipitation || '0'} mm`);
    }

    // ⭐ [핵심] 오늘과 내일 데이터를 합치면서 '구분 태그' 강제 주입
    let combinedHourly = [];

    // 오늘 데이터 처리
    if (weather.hourly && Array.isArray(weather.hourly)) {
        weather.hourly.forEach(item => {
            item.targetDate = '오늘'; // 강제 태그
            combinedHourly.push(item);
        });
    }

    // 내일 데이터 처리
    if (weather.tomorrowHourly && Array.isArray(weather.tomorrowHourly)) {
        weather.tomorrowHourly.forEach(item => {
            item.targetDate = '내일'; // 강제 태그
            combinedHourly.push(item);
        });
    }

    // 렌더링 (최대 48개)
    renderHourlyForecast(combinedHourly.slice(0, 48));

    // 내일 탭 섹션 숨김
    const tomorrowContainer = document.getElementById('tomorrow-forecast');
    if (tomorrowContainer) {
        const card = tomorrowContainer.closest('.horizontal-forecast-card') || tomorrowContainer.closest('.tab-content');
        if (card) card.style.display = 'none';
    }

    if (weather.daily) renderWeeklyForecast(weather.daily);
    updateForecastSummaries(weather);

    const aqiCard = document.querySelector('.air-quality-summary');
    if (aqiCard && weather.regionName) {
        aqiCard.style.cursor = 'pointer';
        aqiCard.onclick = function () {
            location.href = `/fine-dust?region=${encodeURIComponent(weather.regionName)}`;
        };
    }
}

function setNoWarningUI(titleEl, descEl, iconEl) {
    if (titleEl) titleEl.textContent = "특보 없음";
    if (descEl) descEl.textContent = "현재 발효된 특보가 없습니다.";
    if (iconEl) {
        iconEl.className = 'fas fa-check-circle status-icon safe';
        iconEl.style.color = '#2ecc71';
    }
}

function updateForecastSummaries(weather) {
    const txt = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };
    if (weather.summary) {
        txt('ultra-short-summary', weather.summary.ultraShortSummary || '정보 없음');
        txt('short-term-summary', weather.summary.shortSummary || '정보 없음');
        txt('mid-term-summary', weather.summary.midSummary || '정보 없음');
        if (weather.hourly && weather.hourly.length > 0) {
            txt('ultra-short-temp', `${Math.round(weather.hourly[0].temperature)}°C`);
            txt('ultra-short-humidity', `${Math.round(weather.hourly[0].humidity)}%`);
        }
        if (weather.daily && weather.daily.length > 1) {
            const tmr = weather.daily[1];
            txt('short-term-max-temp', `${Math.round(tmr.maxTemp)}°C`);
            txt('short-term-min-temp', `${Math.round(tmr.minTemp)}°C`);
        }
        if (weather.daily && weather.daily.length > 2) {
            const wk = weather.daily[2];
            txt('weekly-precipitation', `${wk.precipitationProbability || 0}%`);
            txt('temp-trend', '평년 비슷');
        }
    }
}

// ⭐ [수정됨] 날짜 구분선 렌더링 함수 (targetDate 사용)
function renderHourlyForecast(data) {
    const container = document.getElementById('hourly-forecast');
    if (!container) return;
    container.innerHTML = '';

    if (!data || data.length === 0) {
        container.innerHTML = '<div class="no-data">예보 정보가 없습니다.</div>';
        return;
    }

    let lastTargetDate = null; // 마지막으로 찍은 태그 (오늘/내일)

    data.forEach((item, index) => {
        // item.targetDate ('오늘' 또는 '내일')가 바뀌는 시점에 구분선 추가
        if (item.targetDate && item.targetDate !== lastTargetDate) {
            container.innerHTML += `
                <div class="hourly-date-marker">
                    <div class="marker-day">${item.targetDate}</div>
                </div>
            `;
            lastTargetDate = item.targetDate;
        }

        container.innerHTML += `
            <div class="hour-item">
                <div class="hour-time">${item.time}</div>
                <div class="hour-icon"><i class="${item.weatherIcon || 'fas fa-sun'}"></i></div>
                <div class="hour-temp">${Math.round(item.temperature)}°</div>
            </div>
        `;
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

async function loadCommunityData() {
    const container = document.getElementById('community-posts');
    if (container) container.innerHTML = `<div class="post-item"><h4 class="post-title">오늘 날씨 정말 좋네요!</h4></div><div class="post-item"><h4 class="post-title">주말 등산 가실 분?</h4></div>`;
}