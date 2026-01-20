/**
 * main.js - 메인 대시보드 (통합 버전: 지도 기능 + 디자인 개선)
 */

// 전역 변수: 지도와 오버레이 객체 저장
let kakaoMap = null;
let mapOverlays = {};

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

    if (document.getElementById('current-temp')) {
        loadDashboardData();
        setInterval(loadDashboardData, 300000);
    }

    // GPS 버튼 이벤트
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', async (lat, lng) => {
            await loadWeatherDataByGPS(lat, lng);
        });
    }

    // 카카오맵 스크립트 로드 확인 후 지도 초기화
    checkKakaoMapLoop();
});

// [기능 유지] 지역 클릭 시 메인 대시보드 변경 함수 (전역 함수)
window.changeDashboardLocation = async function (lat, lng, name) {
    console.log(`지역 변경: ${name} (${lat}, ${lng})`);

    // 1. 로딩 표시
    const locationTitle = document.getElementById('current-location');
    if (locationTitle) locationTitle.innerText = `${name}로 이동 중...`;

    // 2. 해당 좌표로 날씨 데이터 새로고침
    await loadWeatherDataByGPS(lat, lng, name); // name 전달하여 강제 지역명 설정

    // 3. 화면 최상단으로 부드럽게 스크롤 이동
    window.scrollTo({top: 0, behavior: 'smooth'});
};

// 카카오맵 스크립트 로딩 대기 함수
function checkKakaoMapLoop() {
    if (window.kakao && window.kakao.maps) {
        kakao.maps.load(() => initKakaoMap());
    } else {
        setTimeout(checkKakaoMapLoop, 500);
    }
}

// [1] 대시보드 전체 데이터 로드
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

// [2] 카카오맵 초기화 함수
function initKakaoMap() {
    const container = document.getElementById('kakao-map');
    if (!container) return;

    const options = {
        center: new kakao.maps.LatLng(36.3, 127.8), // 대한민국 중심
        level: 13, // 줌 레벨
        draggable: true,
        scrollwheel: true
    };

    kakaoMap = new kakao.maps.Map(container, options);

    // 지도 생성 후 마커 표시
    loadRegionalWeatherData();
}

// [3] 지역별 날씨 로드 + 지도 마커 표시 + 클릭 이벤트 (기존 기능 유지)
async function loadRegionalWeatherData() {
    const listContainer = document.getElementById('regional-weather');

    // 주요 도시 좌표
    const regions = [
        {name: '서울', code: '1100000000', lat: 37.5665, lng: 126.9780},
        {name: '대전', code: '3000000000', lat: 36.3504, lng: 127.3845},
        {name: '광주', code: '2900000000', lat: 35.1595, lng: 126.8526},
        {name: '대구', code: '2700000000', lat: 35.8714, lng: 128.6014},
        {name: '부산', code: '2600000000', lat: 35.1796, lng: 129.0756},
        {name: '제주', code: '5000000000', lat: 33.4996, lng: 126.5312},
        {name: '독도', code: '', lat: 37.2429, lng: 131.8669}
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

            // (A) 지도 오버레이
            if (kakaoMap) {
                const content = `
                    <div class="customoverlay" ${clickAction} style="cursor: pointer; background:white; padding:5px; border-radius:5px; border:1px solid #ccc; text-align:center;">
                        <a href="javascript:void(0);" style="text-decoration:none; color:#333;">
                            <span class="title" style="display:block; font-weight:bold;">${region.name}</span>
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

            // (B) 사이드바 리스트
            if (listContainer && region.code) {
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

// 헬퍼 함수들
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

// [4] 날씨 데이터 로드 (문법 오류 수정됨)
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

// [디자인 적용] 메인 페이지 미세먼지 예보 렌더링 (CSS 클래스 기반)
function updateMainPageAqiForecast(list) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;

    container.innerHTML = '';

    // 레이아웃: 반반 채우기
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

        const cardDiv = document.createElement('div');
        // 날씨 카드와 동일한 스타일 적용을 위해 클래스 부여 (main.css에서 제어)
        cardDiv.className = 'aqi-forecast-card';

        cardDiv.innerHTML = `
            <div class="aqi-card-header">
                <span class="aqi-label">${label}</span>
                <span class="aqi-date">${item.date}</span>
            </div>
            
            <div class="aqi-card-body">
                <div class="aqi-icon">${iconHtml}</div>
                <div class="aqi-status-badge ${gradeClass}">${statusText}</div>
            </div>
        `;

        container.appendChild(cardDiv);
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
        container.innerHTML += `<div class="day-item"><div class="day-header"><div class="day-name">${day.dayOfWeek}</div><div class="day-date">${day.date}</div></div><div class="day-temps"><div class="temp-am"><span class="temp-label">오전</span><div class="temp-icon"><i class="${amIcon}"></i></div><span class="temp-value">${Math.round(day.minTemp)}°</span></div><div class="temp-pm"><span class="temp-label">오후</span><div class="temp-icon"><i class="${pmIcon}"></i></div><span class="temp-value">${Math.round(day.maxTemp)}°</span></div></div></div>`;
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