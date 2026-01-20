/**
 * main.js - 메인 대시보드 (카카오맵 연동 최종 버전)
 */

// 전역 변수: 지도와 오버레이 객체 저장
let kakaoMap = null;
let mapOverlays = {};

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

    // 날씨 데이터 자동 갱신
    if (document.getElementById('current-temp')) {
        loadDashboardData();
        setInterval(loadDashboardData, 300000); // 5분마다
    }

    // GPS 버튼 이벤트
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', async (lat, lng) => {
            await loadWeatherDataByGPS(lat, lng);
        });
    }

    // [중요] 카카오맵 스크립트 로드 확인 후 지도 초기화
    checkKakaoMapLoop();
});

// 카카오맵 스크립트가 완전히 로드될 때까지 기다렸다가 실행
function checkKakaoMapLoop() {
    if (window.kakao && window.kakao.maps) {
        kakao.maps.load(() => initKakaoMap());
    } else {
        // 아직 로드 안 됐으면 0.5초 뒤에 다시 확인
        setTimeout(checkKakaoMapLoop, 500);
    }
}

// [1] 대시보드 전체 데이터 로드
async function loadDashboardData() {
    try {
        showLoading();
        await Promise.all([
            loadWeatherData(),
            loadAirQualitySummary(),
            loadRegionalWeatherData(), // 여기서 지도가 그려집니다
            loadCommunityData(),
            loadAirQualityForecast()
        ]);
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    } finally {
        hideLoading();
    }
}

// [2] 카카오맵 초기화 함수
function initKakaoMap() {
    const container = document.getElementById('kakao-map');
    if (!container) return; // HTML에 지도가 없으면 중단

    const options = {
        center: new kakao.maps.LatLng(36.5, 127.8), // 대한민국 중심 좌표
        level: 13, // 줌 레벨
        draggable: true,
        scrollwheel: true
    };

    // 지도 생성
    kakaoMap = new kakao.maps.Map(container, options);

    // 지도가 만들어진 후, 날씨 마커(오버레이) 표시
    loadRegionalWeatherData();
}

// [3] 지역별 날씨 로드 + 지도 마커 표시 (핵심 기능)
async function loadRegionalWeatherData() {
    const listContainer = document.getElementById('regional-weather');

    // 주요 도시 좌표 (API 연동용)
    const regions = [
        {name: '서울', code: '1100000000', lat: 37.5665, lng: 126.9780},
        {name: '강원', code: '4200000000', lat: 37.8228, lng: 128.1555},
        {name: '대전', code: '3000000000', lat: 36.3504, lng: 127.3845},
        {name: '광주', code: '2900000000', lat: 35.1595, lng: 126.8526},
        {name: '대구', code: '2700000000', lat: 35.8714, lng: 128.6014},
        {name: '부산', code: '2600000000', lat: 35.1796, lng: 129.0756},
        {name: '제주', code: '5000000000', lat: 33.4996, lng: 126.5312},
        {name: '독도', code: '', lat: 37.2429, lng: 131.8669}
    ];

    const regionCodes = regions.filter(r => r.code).map(r => r.code).join(',');

    try {
        // API 호출
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

            // 독도 예외 처리
            if (region.name === '독도') {
                temp = '15'; cond = '맑음'; iconClass = 'fas fa-flag';
            } else {
                // API 데이터 매칭
                const data = weatherData.find(d => d.regionCode === region.code);
                if (data && data.current) {
                    temp = Math.round(data.current.temperature);
                    cond = data.current.weatherCondition;
                    iconClass = getWeatherIconClass(cond);
                }
            }

            // (A) 지도 위에 오버레이(말풍선) 생성
            if (kakaoMap) {
                const content = `
                    <div class="customoverlay">
                        <a href="javascript:void(0);">
                            <span class="title">${region.name}</span>
                            <div class="weather-content">
                                <i class="${iconClass}" style="color:${getIconColor(iconClass)}"></i>
                                <span class="temp">${temp}°</span>
                            </div>
                        </a>
                    </div>`;

                const position = new kakao.maps.LatLng(region.lat, region.lng);

                // 기존 오버레이 삭제 (중복 방지)
                if (mapOverlays[region.name]) {
                    mapOverlays[region.name].setMap(null);
                }

                // 새 오버레이 추가
                const customOverlay = new kakao.maps.CustomOverlay({
                    map: kakaoMap,
                    position: position,
                    content: content,
                    yAnchor: 1
                });

                mapOverlays[region.name] = customOverlay;
            }

            // (B) 사이드바 리스트 추가
            if (listContainer && region.code) {
                listContainer.innerHTML += `
                    <div class="region-weather">
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

// 지도 새로고침 버튼 기능
function refreshMap() {
    loadRegionalWeatherData();
    const btn = document.querySelector('.map-control-btn i');
    if(btn) {
        btn.classList.add('fa-spin');
        setTimeout(() => btn.classList.remove('fa-spin'), 1000);
    }
}

// 아이콘 색상 결정
function getIconColor(iconClass) {
    if (iconClass.includes('sun')) return '#f39c12';
    if (iconClass.includes('rain') || iconClass.includes('umbrella')) return '#3498db';
    if (iconClass.includes('cloud')) return '#7f8c8d';
    if (iconClass.includes('flag')) return '#e74c3c';
    return '#333';
}

// 날씨 상태 텍스트 -> 아이콘 변환
function getWeatherIconClass(condition) {
    if (!condition) return 'fas fa-question';
    if (condition.includes('맑음')) return 'fas fa-sun';
    if (condition.includes('구름') || condition.includes('흐림')) return 'fas fa-cloud';
    if (condition.includes('비')) return 'fas fa-umbrella';
    if (condition.includes('눈')) return 'fas fa-snowflake';
    return 'fas fa-cloud-sun';
}

// [4] 기타 데이터 로드 (기존 유지)
async function loadWeatherData() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await res.json();
        if (data.success) updateWeatherUI(data.data);
    } catch (e) { console.error(e); }
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
    } catch (e) { console.error(e); }
}

function updateWeatherUI(weather) {
    if (!weather) return;
    const txt = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
    const html = (id, val) => { const el = document.getElementById(id); if (el) el.innerHTML = val; };

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
    const txt = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
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
    } catch (e) { console.error(e); }
}

async function loadAirQualitySummaryByGPS(lat, lng) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();
        if (data.success && data.data) updateAqiSummaryUI(data.data);
    } catch (e) { console.error(e); }
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

async function loadAirQualityForecast() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/air-quality/forecast/서울`);
        const data = await res.json();
        if (data.success && data.data) renderAqiForecast(data.data);
    } catch (e) { console.error(e); }
}

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
                <div style="font-size:0.85rem; color:#666; padding:0 10px;">${item.advice ? item.advice.substring(0, 30) + '...' : '정보 없음'}</div>
            </div>`;
    });
}

async function loadCommunityData() {
    const container = document.getElementById('community-posts');
    if (container) container.innerHTML = `<div class="post-item"><h4 class="post-title">오늘 날씨 정말 좋네요!</h4></div><div class="post-item"><h4 class="post-title">주말 등산 가실 분?</h4></div>`;
}