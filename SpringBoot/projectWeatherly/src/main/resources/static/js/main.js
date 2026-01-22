/**
 * main.js - 메인 대시보드 로직 (통합 최종본)
 * - AQI 등급 타입 불일치 버그 수정 적용
 * - 가상 데이터 배지 및 선택 지역별 기상특보 업데이트 적용
 * - 지도 이동 및 전체 행정구역 오버레이 로직 적용
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

// [기능] 지역 변경 (클릭 시 실행)
window.changeDashboardLocation = async function (lat, lng, name) {
    console.log(`지역 변경: ${name} (${lat}, ${lng})`);
    const locationTitle = document.getElementById('current-location');
    if (locationTitle) locationTitle.innerText = `${name}로 이동 중...`;

    // 1. 지도 이동
    if (kakaoMap) {
        const moveLatLon = new kakao.maps.LatLng(lat, lng);
        kakaoMap.panTo(moveLatLon);
    }

    // 2. 해당 좌표로 날씨 데이터 새로고침
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

    const options = {
        center: new kakao.maps.LatLng(36.3, 127.8),
        level: 13,
        draggable: true,
        scrollwheel: true
    };

    kakaoMap = new kakao.maps.Map(container, options);
    loadRegionalWeatherData();
}

async function loadRegionalWeatherData() {
    const listContainer = document.getElementById('regional-weather');

    // [설정] 17개 전체 행정구역 정의 (독도 포함 시 18개)
    const regions = [
        // [지도 O] 주요 거점 도시 (지도에 표시됨)
        {name: '서울', code: '1100000000', lat: 37.5665, lng: 126.9780, showOnMap: true},
        {name: '부산', code: '2600000000', lat: 35.1796, lng: 129.0756, showOnMap: true},
        {name: '대구', code: '2700000000', lat: 35.8714, lng: 128.6014, showOnMap: true},
        {name: '광주', code: '2900000000', lat: 35.1595, lng: 126.8526, showOnMap: true},
        {name: '대전', code: '3000000000', lat: 36.3504, lng: 127.3845, showOnMap: true},
        {name: '강원', code: '4200000000', lat: 37.8228, lng: 128.1555, showOnMap: true},
        {name: '제주', code: '5000000000', lat: 33.4996, lng: 126.5312, showOnMap: true},

        // [지도 X] 사이드바 목록에는 나오지만 지도에서는 숨김 (겹침 방지)
        {name: '인천', code: '2800000000', lat: 37.4563, lng: 126.7052, showOnMap: false}, // 서울과 겹침
        {name: '울산', code: '3100000000', lat: 35.5384, lng: 129.3114, showOnMap: false}, // 부산과 겹침
        {name: '세종', code: '3600000000', lat: 36.4800, lng: 127.2890, showOnMap: false}, // 대전과 겹침
        {name: '경기', code: '4100000000', lat: 37.4138, lng: 127.5183, showOnMap: false}, // 서울과 겹침
        {name: '충북', code: '4300000000', lat: 36.6350, lng: 127.4914, showOnMap: false},
        {name: '충남', code: '4400000000', lat: 36.6588, lng: 126.6728, showOnMap: false},
        {name: '전북', code: '4500000000', lat: 35.7175, lng: 127.1530, showOnMap: false},
        {name: '전남', code: '4600000000', lat: 34.8163, lng: 126.4629, showOnMap: false},
        {name: '경북', code: '4700000000', lat: 36.5760, lng: 128.5056, showOnMap: false},
        {name: '경남', code: '4800000000', lat: 35.2383, lng: 128.6924, showOnMap: false}
    ];

    const regionCodes = regions.filter(r => r.code).map(r => r.code).join(',');

    try {
        let weatherData = [];
        // [핵심] 한 번의 호출로 모든 지역 데이터 요청 (1 Traffic)
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

            // (A) 지도 오버레이 (showOnMap이 true인 지역만 표시)
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

            // (B) 사이드바 리스트 (모든 지역 표시)
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
            // 요약 정보 로드 시 예보도 함께 로드 (기본 지역)
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

// [기능] 메인 페이지 대기질 예보 업데이트 (날짜 정렬 및 매칭)
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

    // 1. 날짜 오름차순 정렬 (과거->미래)
    list.sort((a, b) => a.date.localeCompare(b.date));

    // 2. 오늘 날짜 구하기 (한국 시간 기준)
    const today = new Date();
    const krNow = new Date(today.getTime() + (9 * 60 * 60 * 1000));
    const todayStr = krNow.toISOString().split('T')[0];

    const tomorrow = new Date(krNow);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];

    // 3. 오늘과 내일 데이터 매칭
    const todayData = list.find(item => item.date === todayStr) || list[0];
    const tomorrowData = list.find(item => item.date === tomorrowStr) || list[1];

    const targetItems = [];
    if (todayData) targetItems.push({label: '오늘 예보', data: todayData});
    if (tomorrowData) targetItems.push({label: '내일 예보', data: tomorrowData});

    // 4. 카드 렌더링
    targetItems.forEach(item => {
        const data = item.data;
        const gradeClass = getAqiClass(data.overallGrade);
        const statusText = getAqiStatusText(data.overallGrade);
        const iconHtml = getAqiIcon(data.overallGrade);

        container.innerHTML += `
            <div class="aqi-forecast-card">
                <div class="aqi-card-header">
                    <span class="aqi-label">${item.label}</span>
                    <span class="aqi-date">${data.date}</span>
                </div>
                <div class="aqi-card-body">
                    <div class="aqi-icon">${iconHtml}</div>
                    <div class="aqi-status-badge ${gradeClass}">${statusText}</div>
                </div>
            </div>
        `;
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

// [핵심] 날씨 UI 및 특보 업데이트 (통합본)
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

    // 1. 지역명 & TEST MODE 배지 처리
    if (weather.regionName) {
        const locationEl = document.getElementById('current-location');
        if (locationEl) {
            let name = getFullSidoName(weather.regionName);
            if (weather.isMock) name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px; vertical-align:middle; margin-left: 5px;">TEST MODE</span>';
            locationEl.innerHTML = name;
        }
    }

    // 2. 기상특보 카드 업데이트 (특보 데이터가 없으면 '특보 없음' 표시)
    const alertTitle = document.getElementById('weather-alert-title');
    const alertDesc = document.getElementById('weather-alert-desc');
    const iconEl = document.querySelector('.warning-status .status-icon');

    // (A) weather.warnings 배열 처리 (첫 번째 코드 스타일)
    if (weather.warnings && weather.warnings.length > 0) {
        const activeWarnings = weather.warnings.filter(w => w.active);

        if (activeWarnings.length > 0) {
            if(alertTitle) alertTitle.textContent = activeWarnings.map(w => w.title).join(', ');
            if(alertDesc) alertDesc.textContent = `${weather.regionName} 지역에 기상특보가 발령 중입니다.`;

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
    }
    // (B) weather.warning 단일 객체 처리 (두 번째 코드 스타일) - 호환성 유지
    else if (weather.warning) {
        if(alertTitle) alertTitle.textContent = weather.warning.title;
        if(alertDesc) alertDesc.textContent = weather.warning.description;

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
    }
    // (C) 특보 없음 처리
    else {
        setNoWarningUI(alertTitle, alertDesc, iconEl);
    }

    // 3. 현재 날씨
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

function setNoWarningUI(titleEl, descEl, iconEl) {
    if(titleEl) titleEl.textContent = "특보 없음";
    if(descEl) descEl.textContent = "현재 발효된 특보가 없습니다.";
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
        '서울': '서울', '부산': '부산', '대구': '대구', '인천': '인천', '광주': '광주', '대전': '대전', '울산': '울산', '세종': '세종',
        '경기': '경기', '강원': '강원', '제주': '제주', '충청': full.includes('북') ? '충북' : '충남',
        '전라': full.includes('북') ? '전북' : '전남', '경상': full.includes('북') ? '경북' : '경남',
        '서울특별시': '서울', '부산광역시': '부산'
    };
    if (full.length === 2) return full;
    const shortName = full.substring(0, 2);
    return mapping[shortName] || mapping[full] || '서울';
}

function getFullSidoName(shortName) {
    if (!shortName) return '대한민국';
    if (shortName.length > 2) return shortName;
    const map = {
        '서울': '서울특별시', '부산': '부산광역시', '대구': '대구광역시', '인천': '인천광역시', '광주': '광주광역시', '대전': '대전광역시',
        '울산': '울산광역시', '세종': '세종특별자치시', '경기': '경기도', '강원': '강원도', '충북': '충청북도', '충남': '충청남도',
        '전북': '전라북도', '전남': '전라남도', '경북': '경상북도', '경남': '경상남도', '제주': '제주특별자치도'
    };
    return map[shortName] || shortName;
}

// ⭐ [AQI 헬퍼] 타입 불일치 버그 수정 적용 (String 변환 및 공백 제거)
function getAqiClass(grade) {
    switch (String(grade).trim()) {
        case '1': return 'good';
        case '2': return 'normal';
        case '3': return 'bad';
        default: return 'very-bad';
    }
}

function getAqiStatusText(grade) {
    switch (String(grade).trim()) {
        case '1': return '좋음';
        case '2': return '보통';
        case '3': return '나쁨';
        default: return '매우나쁨';
    }
}

function getAqiIcon(grade) {
    switch (String(grade).trim()) {
        case '1': return '<i class="fas fa-smile" style="color:#2ecc71"></i>';
        case '2': return '<i class="fas fa-meh" style="color:#f39c12"></i>';
        case '3': return '<i class="fas fa-frown" style="color:#e74c3c"></i>';
        default: return '<i class="fas fa-dizzy" style="color:#e74c3c"></i>';
    }
}