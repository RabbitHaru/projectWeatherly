/**
 * fine-dust.js - 미세먼지 페이지 (Full Version)
 */

document.addEventListener('DOMContentLoaded', function () {
    const locEl = document.getElementById('fine-dust-location');
    if (!locEl) return;

    updateCurrentTime();
    setInterval(updateCurrentTime, 60000);

    const urlParams = new URLSearchParams(window.location.search);
    const regionParam = urlParams.get('region');

    let saved = null;
    if (typeof RegionManager !== 'undefined') {
        saved = RegionManager.load();
    }

    let targetRegion = null;
    if (regionParam) targetRegion = regionParam;
    else if (saved && saved.name) targetRegion = saved.name;

    if (targetRegion) {
        locEl.textContent = targetRegion;
        const searchSido = extractSidoName(targetRegion);
        loadFineDustBySido(searchSido);
        loadRegionalComparisonWithCache();
        setInterval(() => {
            loadFineDustBySido(searchSido);
            loadRegionalComparisonWithCache(true);
        }, 300000);
    } else {
        if (locEl.textContent.trim() === '??' || locEl.textContent.trim() === '') locEl.textContent = '위치 확인 중..';
        loadCurrentAirQuality();
        loadRegionalComparisonWithCache();
        setInterval(() => {
            loadCurrentAirQuality();
            loadRegionalComparisonWithCache(true);
        }, 300000);
    }

    if (typeof bindGpsButton === 'function') {
        bindGpsButton('fine-dust-gps-sync-btn', async (lat, lng) => {
            await loadFineDustByGPS(lat, lng);
        });
    }
});

async function loadRegionalComparisonWithCache(forceRefresh = false) {
    const listContainer = document.getElementById('regional-aqi-list') || document.getElementById('nationwide-list');
    if (!listContainer) return;

    const CACHE_KEY = 'cachedNationwideData';
    const CACHE_TIME_KEY = 'cachedNationwideTime';
    const EXPIRE_TIME = 5 * 60 * 1000;

    if (!forceRefresh) {
        const cachedData = sessionStorage.getItem(CACHE_KEY);
        const cachedTime = sessionStorage.getItem(CACHE_TIME_KEY);
        const now = new Date().getTime();
        if (cachedData && cachedTime && (now - cachedTime < EXPIRE_TIME)) {
            console.log('🚀 전국 대기질: 캐시 데이터 사용 (속도 향상)');
            renderRegionalList(listContainer, JSON.parse(cachedData));
            return;
        }
    }

    if (listContainer.children.length === 0) {
        listContainer.innerHTML = '<div style="padding:20px; text-align:center; color:#999;"><i class="fas fa-spinner fa-spin"></i> 로딩 중...</div>';
    }

    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/sido/전국`);
        const result = await res.json();
        if (result.success && result.data) {
            sessionStorage.setItem(CACHE_KEY, JSON.stringify(result.data));
            sessionStorage.setItem(CACHE_TIME_KEY, new Date().getTime());
            renderRegionalList(listContainer, result.data);
        }
    } catch (e) {
        console.error("리스트 로드 실패:", e);
        listContainer.innerHTML = '<div style="padding:20px; text-align:center;">데이터 로드 실패</div>';
    }
}

function renderRegionalList(container, allData) {
    const targetRegions = [
        '서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종',
        '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주'
    ];
    container.innerHTML = '';
    targetRegions.forEach(r => {
        const data = allData.find(d => d.sidoName.includes(r));
        if (data) renderSimpleItem(container, data);
    });
}

function renderSimpleItem(container, r) {
    const div = document.createElement('div');
    div.className = 'region-weather';
    const statusText = r.overallStatus ? r.overallStatus : getAqiStatusText(r.overallGrade);
    const colorClass = getAqiClass(r.overallGrade);
    const testBadge = r.isMock ? '<span style="color:#e74c3c; font-size:0.7em; margin-left:5px;">[TEST]</span>' : '';

    div.innerHTML = `<div class="region-info"><span class="region-name">${r.sidoName}${testBadge}</span></div><span class="aqi-badge ${colorClass}">${statusText}</span>`;

    div.addEventListener('click', () => {
        document.querySelectorAll('.region-weather').forEach(el => el.classList.remove('selected'));
        div.classList.add('selected');
        if (typeof RegionManager !== 'undefined') RegionManager.save(r.sidoName);
        loadFineDustBySido(r.sidoName);
    });
    container.appendChild(div);
}

async function loadCurrentAirQuality() {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/current`);
        const data = await res.json();
        if (data.success && data.data) {
            updateFineDustUI(data.data);
            if (data.data.sidoName) loadForecast(data.data.sidoName);
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadFineDustBySido(sidoName) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/sido/${encodeURIComponent(sidoName)}`);
        const data = await res.json();
        if (data.success && data.data && data.data.length > 0) {
            updateFineDustUI(data.data[0]);
            loadForecast(sidoName);
        }
    } catch (e) {
        console.error("지역 로드 실패:", e);
    }
}

async function loadForecast(sido) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    if (!sido || sido.includes('?')) sido = '서울';
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/forecast/${encodeURIComponent(sido)}`);
        const data = await res.json();
        if (data.success) updateFineDustForecast(data.data);
    } catch (e) {
        console.error("예보 로드 실패:", e);
    }
}

function updateFineDustForecast(list) {
    const todayDiv = document.getElementById('today-aqi-forecast');
    const tomorrowDiv = document.getElementById('tomorrow-aqi-forecast');
    if (!list || !Array.isArray(list) || list.length === 0) {
        const noData = '<div class="no-data" style="padding:30px; text-align:center;">예보 정보가 없습니다.</div>';
        if (todayDiv) todayDiv.innerHTML = noData;
        if (tomorrowDiv) tomorrowDiv.innerHTML = noData;
        return;
    }
    if (todayDiv && list.length > 0) todayDiv.innerHTML = renderForecastCard(list[0], '오늘');
    if (tomorrowDiv) {
        if (list.length > 1) tomorrowDiv.innerHTML = renderForecastCard(list[1], '내일');
        else tomorrowDiv.innerHTML = '<div class="no-data" style="padding:30px; text-align:center;">내일 예보 준비 중</div>';
    }
}

function renderForecastCard(item, label) {
    const gradeClass = getAqiClass(item.overallGrade);
    const statusText = getAqiStatusText(item.overallGrade);
    const dateStr = item.date ? item.date : '';
    let adviceText = item.advice || '상세 예보 정보가 없습니다.';
    let causeText = item.cause ? `<br><br><strong><i class="fas fa-search-plus"></i> 원인:</strong><br>${item.cause}` : '';
    return `<div class="aqi-forecast-day"><div class="forecast-header"><span class="forecast-label" style="font-size:1.2rem; font-weight:bold;">${label}</span><span class="forecast-date" style="color:#666; font-size:0.9rem;">(${dateStr})</span></div><div style="font-size:3.5rem; margin:15px 0; color:var(--primary-color);">${getAqiIcon(item.overallGrade)}</div><div class="forecast-overall ${gradeClass}" style="margin-bottom:15px; font-weight:bold;">${statusText}</div><div class="forecast-advice" style="text-align: center; word-break: keep-all; line-height: 1.6;"><i class="fas fa-quote-left" style="color:#ddd; margin-right:5px;"></i>${adviceText}${causeText}<i class="fas fa-quote-right" style="color:#ddd; margin-left:5px;"></i></div></div>`;
}

function updateFineDustUI(data) {
    if (!data) return;
    const locNameEl = document.getElementById('fine-dust-location');
    if (locNameEl && data.sidoName) {
        let name = getFullSidoName(data.sidoName);
        document.title = `${name} 대기질 - Weatherly`;
        if (data.isMock) {
            name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px; vertical-align:middle; margin-left: 5px;">TEST MODE</span>';
            locNameEl.innerHTML = name;
        } else locNameEl.textContent = name;
    }
    if (data.stationName) {
        const st = document.getElementById('station-name');
        if (st) st.textContent = data.stationName;
    }
    if (data.dataTime) {
        let timeStr = Array.isArray(data.dataTime) ? `${data.dataTime[0]}-${String(data.dataTime[1]).padStart(2, '0')}-${String(data.dataTime[2]).padStart(2, '0')} ${String(data.dataTime[3]).padStart(2, '0')}:00` : data.dataTime;
        document.getElementById('fine-dust-update-time').textContent = `업데이트: ${timeStr}`;
        const mt = document.getElementById('measurement-time');
        if (mt) mt.textContent = timeStr;
    }
    const badge = document.getElementById('fine-dust-overall-badge');
    if (badge) {
        const statusText = data.overallStatus ? data.overallStatus : getAqiStatusText(data.overallGrade);
        badge.textContent = statusText;
        badge.className = 'aqi-badge large-badge ' + getAqiClass(data.overallGrade);
    }
    if (data.healthAdvice) {
        document.getElementById('fine-dust-health-advice').textContent = data.healthAdvice;
        document.getElementById('advice-description').textContent = data.healthAdvice;
    }
    if (data.overallStatus) document.getElementById('advice-title').textContent = `현재 상태: ${data.overallStatus}`;

    updateDetailCard('khai', data.khai, '');
    updateDetailCard('pm10-detail', data.pm10, 'µg/m³');
    updateDetailCard('pm25-detail', data.pm25, 'µg/m³');
    updateDetailCard('o3-detail', data.o3, 'ppm');
    updateDetailCard('no2', data.no2, 'ppm');
    updateDetailCard('co', data.co, 'ppm');
}

function updateDetailCard(prefix, item, unitStr) {
    const valEl = document.getElementById(`${prefix}-value`);
    if (!valEl) return;
    let statEl = document.getElementById(`${prefix}-status`);
    const val = (item && item.value !== null) ? item.value : '--';
    const grade = (item && item.grade) ? item.grade : '';
    valEl.textContent = `${val} ${item && item.unit ? item.unit : unitStr}`;
    if (statEl) {
        statEl.textContent = (item && item.status) ? item.status : '--';
        statEl.className = 'aqi-detail-status ' + getAqiClass(grade);
    }
}

async function loadFineDustByGPS(lat, lng) {
    const baseUrl = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : window.location.origin;
    try {
        const res = await fetch(`${baseUrl}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {method: 'POST'});
        const data = await res.json();

        if (data.success && data.data) {
            // ⭐ [핵심 수정] 서버가 알려준 시도명(예: 부산)으로 즉시 저장
            const realSidoName = data.data.sidoName || '내 위치';

            // 1. 세션 스토리지 업데이트
            RegionManager.save(realSidoName, lat, lng);

            console.log(`📍 미세먼지 위치 확정: ${realSidoName}`);

            // 2. UI 즉시 업데이트 (화면에 '부산' 표시)
            updateFineDustUI(data.data);

            // 3. 예보 로드
            const sido = extractSidoName(realSidoName);
            await loadForecast(sido);
        } else {
            alert('위치 정보를 찾을 수 없습니다.');
        }
    } catch (e) {
        console.error(e);
        alert("서버 연결 실패");
    }
}