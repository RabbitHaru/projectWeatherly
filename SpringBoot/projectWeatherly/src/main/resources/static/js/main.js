/**
 * main.js - 메인 대시보드 로직
 */

let kakaoMap = null;
let mapOverlays = {};

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

    checkKakaoMapLoop();
});

window.changeDashboardLocation = async function (lat, lng, name) {
    console.log(`지역 변경: ${name} (${lat}, ${lng})`);
    const locationTitle = document.getElementById('current-location');
    if (locationTitle) locationTitle.innerText = `${name}로 이동 중...`;

    // 클릭 시 해당 좌표로 날씨 새로고침
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

    // 중심 좌표를 대전 쯤으로 잡아서 전국이 보이게
    const options = {
        center: new kakao.maps.LatLng(36.35, 127.38),
        level: 13,
        draggable: true,
        scrollwheel: true
    };

    kakaoMap = new kakao.maps.Map(container, options);
    loadRegionalWeatherData();
}

// [핵심 수정] 제주도, 독도 제거한 리스트
async function loadRegionalWeatherData() {
    const listContainer = document.getElementById('regional-weather');

    const regions = [
        {name: '서울', code: '1100000000', lat: 37.5665, lng: 126.9780},
        {name: '대전', code: '3000000000', lat: 36.3504, lng: 127.3845},
        {name: '광주', code: '2900000000', lat: 35.1595, lng: 126.8526},
        {name: '대구', code: '2700000000', lat: 35.8714, lng: 128.6014},
        {name: '부산', code: '2600000000', lat: 35.1796, lng: 129.0756}
        // 제주, 독도 삭제함
    ];

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

            const data = weatherData.find(d => d.regionCode === region.code);

            if (data && data.current) {
                temp = Math.round(data.current.temperature);
                cond = data.current.weatherCondition;
                iconClass = getWeatherIconClass(cond);
            }

            const clickAction = `onclick="changeDashboardLocation(${region.lat}, ${region.lng}, '${region.name}')"`;

            // 지도 오버레이
            if (kakaoMap) {
                const content = `
                    <div class="customoverlay" ${clickAction} style="cursor: pointer;">
                        <a href="javascript:void(0);">
                            <span class="title">${region.name}</span>
                            <div class="weather-content">
                                <i class="${iconClass}" style="color:${getIconColor(iconClass)}"></i>
                                <span class="temp">${temp}°</span>
                            </div>
                        </a>
                    </div>`;

                const position = new kakao.maps.LatLng(region.lat, region.lng);

                if (mapOverlays[region.name]) {
                    mapOverlays[region.name].setMap(null);
                }

                const customOverlay = new kakao.maps.CustomOverlay({
                    map: kakaoMap,
                    position: position,
                    content: content,
                    yAnchor: 1
                });

                mapOverlays[region.name] = customOverlay;
            }

            // 리스트
            if (listContainer) {
                listContainer.innerHTML += `
                    <div class="region-weather" ${clickAction} style="cursor: pointer;">
                        <div class="region-info">
                            <span class="region-name">${region.name}</span>
                            <span class="region-weather-desc">${cond}</span>
                        </div>
                        <div class="region-temp">${temp}°</div>
                    </div>`;
            }
        });

    } catch (e) {
        console.error('지역 날씨 로드 실패', e);
    }
}

// 아이콘 색상
function getIconColor(iconClass) {
    if (iconClass.includes('sun')) return '#f39c12';
    if (iconClass.includes('rain') || iconClass.includes('umbrella')) return '#3498db';
    if (iconClass.includes('cloud')) return '#7f8c8d';
    if (iconClass.includes('flag')) return '#e74c3c';
    return '#333';
}

// 날씨 상태별 아이콘
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
            method: 'POST', headers: {'Content-Type': 'application/json'}
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

// ... (대기질 관련 함수들은 기존과 동일, 생략 가능하지만 파일 전체를 요청했으니 유지) ...
async function loadAirQualitySummary() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) {
            updateAqiSummaryUI(data.data);
            if (data.data.sidoName) loadAirQualityForecast(data.data.sidoName);
        }
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

async function loadAirQualityForecast(sido) {
    if (!sido || sido.includes('?')) sido = '서울';
    sido = extractSidoName(sido);
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) updateMainPageAqiForecast(data.data);
    } catch (e) {
        console.error(e);
    }
}

function updateMainPageAqiForecast(list) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;
    container.innerHTML = '';
    container.style.display = 'flex';
    container.style.width = '100%';
    container.style.gap = '20px';
    container.style.justifyContent = 'space-between';

    if (!list || !Array.isArray(list) || list.length === 0) {
        container.innerHTML = '<div class="no-data" style="padding:20px; width:100%; text-align:center; color:var(--light-text);">예보 정보 없음</div>';
        return;
    }
    const labels = ['오늘 예보', '내일 예보'];
    list.forEach((item, index) => {
        if (index > 1) return;
        const gradeClass = getAqiClass(item.overallGrade);
        const statusText = getAqiStatusText(item.overallGrade);
        const iconHtml = getAqiIcon(item.overallGrade);
        const label = labels[index] || '예보';
        container.innerHTML += `
            <div class="aqi-forecast-card">
                <div class="aqi-card-header"><span class="aqi-label">${label}</span><span class="aqi-date">${item.date}</span></div>
                <div class="aqi-card-body"><div class="aqi-icon">${iconHtml}</div><div class="aqi-status-badge ${gradeClass}">${statusText}</div></div>
            </div>`;
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
        const locationEl = document.getElementById('current-location');
        if (locationEl) {
            let name = getFullSidoName(weather.regionName);
            if (weather.isMock) name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px;">TEST MODE</span>';
            locationEl.innerHTML = name;
        }
    }

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
    if (weather.daily) renderWeeklyForecast(weather.daily); // 7일 데이터 렌더링

    updateForecastSummaries(weather);
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
            txt('weekly-precipitation', `예보확인`); // 중기예보는 강수확률 필드가 다름
            txt('temp-trend', '평년 비슷');
        }
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

    // [확인] 데이터가 2개여도, 7개여도 있는 만큼 다 그림
    data.forEach(day => {
        const amIcon = (day.nightIcon && day.nightIcon.trim()) ? day.nightIcon : 'fas fa-moon';
        const pmIcon = (day.dayIcon && day.dayIcon.trim()) ? day.dayIcon : 'fas fa-sun';
        container.innerHTML += `
            <div class="day-item">
                <div class="day-header"><div class="day-name">${day.dayOfWeek}</div><div class="day-date">${day.date}</div></div>
                <div class="day-temps">
                    <div class="temp-am"><span class="temp-label">최저</span><div class="temp-icon"><i class="${amIcon}"></i></div><span class="temp-value">${Math.round(day.minTemp)}°</span></div>
                    <div class="temp-pm"><span class="temp-label">최고</span><div class="temp-icon"><i class="${pmIcon}"></i></div><span class="temp-value">${Math.round(day.maxTemp)}°</span></div>
                </div>
            </div>`;
    });
}

async function loadCommunityData() {
    const container = document.getElementById('community-posts');
    if (container) container.innerHTML = `<div class="post-item"><h4 class="post-title">오늘 날씨 정말 좋네요!</h4></div><div class="post-item"><h4 class="post-title">주말 등산 가실 분?</h4></div>`;
}

function extractSidoName(full) {
    if (!full) return '서울';
    const mapping = {
        '서울': '서울', '부산': '부산', '대구': '대구', '인천': '인천',
        '광주': '광주', '대전': '대전', '울산': '울산', '세종': '세종',
        '경기': '경기', '강원': '강원', '제주': '제주',
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
        '제주': '제주특별자치도'
    };
    return map[short] || short;
}

// ... (setupDarkMode 등 common.js 함수들)
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