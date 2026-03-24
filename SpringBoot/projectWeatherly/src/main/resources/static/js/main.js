/**
 * main.js - 메인 대시보드 로직 (Full Version)
 */

let kakaoMap = null;
let mapOverlays = {};

document.addEventListener('DOMContentLoaded', function () {
    updateCurrentTime();
    setInterval(() => updateCurrentTime(), 60000);

    initializeLocation();

    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', async (lat, lng) => {
            await loadWeatherDataByGPS(lat, lng);
        });
    }

    checkKakaoMapLoop();
    updateRecentRegionsUI(); // [신규] 최근 방문 지역 칩 렌더링
});



async function initializeLocation() {
    if (!document.getElementById('current-temp')) return;
    const saved = RegionManager.load();
    if (saved) {
        console.log(`📍 저장된 위치 로드: ${saved.name}`);
        await loadWeatherDataByGPS(saved.lat, saved.lng, saved.name);
        loadAirQualitySummaryByGPS(saved.lat, saved.lng);
        loadRegionalWeatherData();
    } else {
        loadDashboardData();
    }
}

window.changeDashboardLocation = async function (lat, lng, name) {
    console.log(`지역 변경: ${name} (${lat}, ${lng})`);
    RegionManager.save(name, lat, lng);
    
    // [신규] 최근 방문 지역 목록 업데이트 (중복 제거 및 최신화)
    addToRecentRegions(name, lat, lng);
    updateRecentRegionsUI();

    const locationTitle = document.getElementById('current-location');
    if (locationTitle) locationTitle.innerText = `${name}로 이동 중...`;
    if (kakaoMap) {
        const moveLatLon = new kakao.maps.LatLng(lat, lng);
        kakaoMap.panTo(moveLatLon);
    }
    await loadWeatherDataByGPS(lat, lng, name);
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
            loadRegionalWeatherData()
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
                    map: kakaoMap, position: position, content: content, yAnchor: 1
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
            headers: { 'Content-Type': 'application/json' }
        });
        const data = await res.json();

        if (data.success) {
            // ⭐ [핵심 수정] 사용자가 직접 선택했거나 세션에 명확히 저장된 이름(forcedRegionName)이 있다면,
            // 백엔드의 GPS 결과보다 먼저 이 이름을 적용해야 지역이 엉뚱하게 바뀌거나 오류 시 서울로 초기화되는 것을 막습니다.
            const realName = forcedRegionName || data.data.regionName || '내 위치';

            // 1. 세션 스토리지에 "부산광역시" 같은 실제 이름으로 저장
            RegionManager.save(realName, lat, lng);

            console.log(`📍 위치 확정: ${realName} (${lat}, ${lng})`);

            // 2. 화면 데이터 갱신
            // 여기서 data.data.regionName이 UI에 '부산광역시'를 찍어줌
            updateWeatherUI(data.data);

            // 3. 대기질 정보도 해당 좌표로 로드
            loadAirQualitySummaryByGPS(lat, lng);

            // 4. 예보 로드
            const sido = realName ? extractSidoName(realName) : '서울';
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
        const res = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, { method: 'POST' });
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
    if (todayData) targetItems.push({ label: '오늘 예보', data: todayData });
    if (tomorrowData) targetItems.push({ label: '내일 예보', data: tomorrowData });
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
        const statusText = aqi.overallStatus ? aqi.overallStatus : getAqiStatusText(aqi.overallGrade);
        badge.textContent = statusText || '--';
        badge.className = 'aqi-badge ' + getAqiClass(aqi.overallGrade);
    }
    const updateItem = (key, unit) => {
        if (aqi[key]) {
            const elVal = document.getElementById(`${key}-value`);
            const elStat = document.getElementById(`${key}-status`);
            if (elVal) elVal.textContent = `${aqi[key].value !== undefined && aqi[key].value !== null ? aqi[key].value : '-'} ${unit}`;
            if (elStat) {
                const itemStatus = aqi[key].status ? aqi[key].status : getAqiStatusText(aqi[key].grade);
                elStat.textContent = itemStatus || '-';
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
            if (weather.isMock) name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px; vertical-align:middle; margin-left: 5px;">더미 데이터</span>';
            locationEl.innerHTML = name;
        }
    }

    // 기상특보 로직
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
        } else setNoWarningUI(alertTitle, alertDesc, iconEl);
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
    } else setNoWarningUI(alertTitle, alertDesc, iconEl);

    if (weather.current) {
        html('current-temp', `${Math.round(weather.current.temperature)}<span class="temp-unit">°C</span>`);
        txt('weather-condition', weather.current.weatherCondition || '맑음');
        txt('feels-like', `${Math.round(weather.current.feelsLike)}°C`);
        txt('wind-speed', `${weather.current.windSpeed?.toFixed(1) || '0'} m/s`);
        txt('humidity', `${Math.round(weather.current.humidity) || '0'}%`);
        txt('precipitation', `${weather.current.precipitation || '0'} mm`);
    }

    let combinedHourly = [];
    if (weather.hourly && Array.isArray(weather.hourly)) {
        weather.hourly.forEach(item => {
            item.targetDate = '오늘';
            combinedHourly.push(item);
        });
    }
    if (weather.tomorrowHourly && Array.isArray(weather.tomorrowHourly)) {
        weather.tomorrowHourly.forEach(item => {
            item.targetDate = '내일';
            combinedHourly.push(item);
        });
    }
    renderHourlyForecast(combinedHourly.slice(0, 48));

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

    // [신규] 오늘의 생활 지수 업데이트 호출
    updateLifeIndexUI(weather);
    
    // [신규] 옷차림 퀵 가이드 업데이트
    updateOutfitQuickView(weather);
}

// [신규] 오늘의 추천 옷차림 요약 (1번 기능)
function updateOutfitQuickView(weather) {
    const el = document.getElementById('outfit-summary');
    if (!el || !weather.current) return;

    const temp = Math.round(weather.current.temperature);
    let outfit = "";

    if (temp >= 28) outfit = "민소매, 반바지, 원피스";
    else if (temp >= 23) outfit = "반팔, 얇은 셔츠, 반바지";
    else if (temp >= 20) outfit = "긴팔 티, 가디건, 면바지";
    else if (temp >= 17) outfit = "니트, 맨투맨, 청바지";
    else if (temp >= 12) outfit = "자켓, 가디건, 야상";
    else if (temp >= 9) outfit = "트렌치 코트, 니트, 야상";
    else if (temp >= 5) outfit = "코트, 가죽 자켓, 히트텍";
    else outfit = "패딩, 두꺼운 코트, 목도리";

    el.textContent = outfit;
}

// [신규] 최근 방문 지역 관리 (2번 기능)
function addToRecentRegions(name, lat, lng) {
    let recent = JSON.parse(localStorage.getItem('recentRegions') || '[]');
    const cleanName = (name.length > 5) ? name.substring(0, 5) : name;
    
    // 동일 지역 중복 제거
    recent = recent.filter(r => r.name !== cleanName);
    
    // 맨 앞에 추가
    recent.unshift({ name: cleanName, lat: parseFloat(lat), lng: parseFloat(lng) });
    
    // 3개까지만 유지
    recent = recent.slice(0, 3);
    localStorage.setItem('recentRegions', JSON.stringify(recent));
}

function updateRecentRegionsUI() {
    const container = document.getElementById('recent-regions');
    if (!container) return;
    
    const recent = JSON.parse(localStorage.getItem('recentRegions') || '[]');
    if (recent.length === 0) {
        container.innerHTML = '';
        return;
    }

    container.innerHTML = '<span style="font-size: 0.8em; color: #888; align-self: center; margin-right: 5px;"><i class="fas fa-history"></i> 최근:</span>';
    recent.forEach(region => {
        const chip = document.createElement('button');
        chip.className = 'recent-chip';
        chip.style.cssText = 'background: rgba(52, 152, 219, 0.1); border: 1px solid rgba(52, 152, 219, 0.2); border-radius: 15px; padding: 4px 12px; font-size: 0.8rem; cursor: pointer; transition: 0.3s; color: var(--primary-color); font-weight: 600;';
        chip.textContent = region.name;
        chip.onclick = () => changeDashboardLocation(region.lat, region.lng, region.name);
        
        // 호버 효과
        chip.onmouseover = () => { chip.style.background = 'var(--primary-color)'; chip.style.color = 'white'; };
        chip.onmouseout = () => { chip.style.background = 'rgba(52, 152, 219, 0.1)'; chip.style.color = 'var(--primary-color)'; };
        
        container.appendChild(chip);
    });
}

// [신규] 오늘의 생활 지수 계산 및 UI 업데이트
function updateLifeIndexUI(weather) {
    if (!weather || !weather.current) return;

    const temp = weather.current.temperature;
    const humidity = weather.current.humidity;
    const precipitation = weather.current.precipitation || 0;
    const wind = weather.current.windSpeed || 0;
    const condition = weather.current.weatherCondition || "";

    // 1. 세차 지수 계산 (비가 오면 0점, 습도 높으면 감점, 맑으면 가점)
    let washScore = 80;
    if (precipitation > 0 || condition.includes("비") || condition.includes("눈")) washScore = 10;
    else {
        if (humidity > 70) washScore -= 20;
        if (condition.includes("맑음")) washScore += 20;
        if (wind > 5) washScore -= 10;
    }
    washScore = Math.max(0, Math.min(100, washScore));
    
    // 2. 빨래 지수 계산 (습도 영향이 가장 큼, 비오면 0점)
    let laundryScore = 100 - humidity; // 습도 90%면 10점, 20%면 80점
    if (precipitation > 0 || condition.includes("비")) laundryScore = 5;
    else if (condition.includes("맑음")) laundryScore += 10;
    laundryScore = Math.max(0, Math.min(100, laundryScore));

    // 3. 야외활동 지수 (기온 18~25도 최적, 미세먼지 나쁘면 감점 - 추후 연동)
    let activityScore = 90;
    if (temp < 0 || temp > 33) activityScore -= 40;
    else if (temp < 10 || temp > 28) activityScore -= 20;
    if (precipitation > 0) activityScore -= 50;
    activityScore = Math.max(0, Math.min(100, activityScore));

    // UI 반영
    const update = (id, score, descId, descs) => {
        const el = document.getElementById(id);
        const descEl = document.getElementById(descId);
        if (el) el.textContent = `${score}점`;
        if (descEl) {
            let msg = descs[0];
            if (score >= 80) msg = descs[3];
            else if (score >= 60) msg = descs[2];
            else if (score >= 40) msg = descs[1];
            descEl.textContent = msg;
        }
    };

    update('car-wash-index', washScore, 'car-wash-desc', [
        "세차하면 후회해요!", "가급적 미루세요.", "세차하기 괜찮아요.", "세차 적극 추천!"
    ]);
    update('laundry-index', laundryScore, 'laundry-desc', [
        "절대 안 말라요!", "실내 건조 하세요.", "빨래하기 무난해요.", "뽀송하게 잘 말라요!"
    ]);
    update('activity-index', activityScore, 'activity-desc', [
        "위험! 실내에 계세요.", "실외 활동 자제.", "적당한 산책 가능.", "밖으로 나가세요!"
    ]);
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

function renderHourlyForecast(data) {
    const container = document.getElementById('hourly-forecast');
    if (!container) return;
    container.innerHTML = '';
    if (!data || data.length === 0) {
        container.innerHTML = '<div class="no-data">예보 정보가 없습니다.</div>';
        return;
    }
    let lastTargetDate = null;
    data.forEach((item, index) => {
        if (item.targetDate && item.targetDate !== lastTargetDate) {
            container.innerHTML += `<div class="hourly-date-marker"><div class="marker-day">${item.targetDate}</div></div>`;
            lastTargetDate = item.targetDate;
        }
        container.innerHTML += `<div class="hour-item"><div class="hour-time">${item.time}</div><div class="hour-icon"><i class="${item.weatherIcon || 'fas fa-sun'}"></i></div><div class="hour-temp">${Math.round(item.temperature)}°</div></div>`;
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
    console.log("Community data rendered by Server (Thymeleaf).");
}