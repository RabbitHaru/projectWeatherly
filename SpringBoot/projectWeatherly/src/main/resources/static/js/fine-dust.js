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

    // [신규] 지도 모달 닫기 바인딩
    const closeBtn = document.getElementById('close-map-modal');
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('station-map-modal').style.display = 'none';
        };
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
    const testBadge = r.isMock ? '<span style="color:#e74c3c; font-size:0.7em; margin-left:5px;">[더미 데이터]</span>' : '';

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
    
    // 원인 텍스트가 있을 경우 디자인 강화
    let causeHtml = '';
    if (item.cause && item.cause.trim() !== "" && !item.cause.includes("준비 중")) {
        causeHtml = `<div class="forecast-cause" style="margin-top: 15px; padding-top: 15px; border-top: 1px dashed #ddd; font-size: 0.9rem; color: #555; text-align: left;">
            <strong style="color: var(--primary-color);"><i class="fas fa-search-plus"></i> 발생 원인:</strong><br>
            ${item.cause}
        </div>`;
    }

    return `
        <div class="aqi-forecast-day" style="background: white; border-radius: 20px; padding: 25px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);">
            <div class="forecast-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                <span class="forecast-label" style="font-size: 1.2rem; font-weight: 800; color: var(--secondary-color);">${label}</span>
                <span class="forecast-date" style="color: #888; font-size: 0.9rem;">${dateStr}</span>
            </div>
            <div style="font-size: 3.5rem; margin: 10px 0;">${getAqiIcon(item.overallGrade)}</div>
            <div class="forecast-overall ${gradeClass}" style="margin-bottom: 20px; font-weight: 800; padding: 8px 25px; border-radius: 30px; display: inline-block;">${statusText}</div>
            <div class="forecast-advice" style="background: #f8f9fa; padding: 15px; border-radius: 15px; line-height: 1.6; word-break: keep-all;">
                <i class="fas fa-quote-left" style="color: #ddd; margin-right: 5px;"></i>
                ${adviceText}
                <i class="fas fa-quote-right" style="color: #ddd; margin-left: 5px;"></i>
                ${causeHtml}
            </div>
        </div>
    `;
}

function updateFineDustUI(data) {
    if (!data) return;
    const locNameEl = document.getElementById('fine-dust-location');
    if (locNameEl && data.sidoName) {
        let name = getFullSidoName(data.sidoName);
        document.title = `${name} 대기질 - Weatherly`;
        if (data.isMock) {
            name += ' <span style="background:#e74c3c; color:white; font-size:0.6em; padding:2px 6px; border-radius:4px; vertical-align:middle; margin-left: 5px;">더미 데이터</span>';
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
    
    // [추가] 측정소 지도 보기 이벤트 바인딩
    const stationHeader = document.getElementById('station-info-header');
    if (stationHeader && data.stationName) {
        stationHeader.onclick = () => {
            showStationMap(data.stationName, data.sidoName);
        };
    }

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
        const res = await fetch(`${baseUrl}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, { method: 'POST' });
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

// [신규] 측정소 지도를 보여주는 함수 (카카오맵 연동 - 검색 정확도 보강)
function showStationMap(stationName, sidoName) {
    const modal = document.getElementById('station-map-modal');
    const displayEl = document.getElementById('target-station-display');
    const mapContainer = document.getElementById('station-map');
    
    if (displayEl) displayEl.textContent = stationName;
    modal.style.display = 'flex';

    if (typeof kakao === 'undefined' || !kakao.maps) {
        mapContainer.innerHTML = '<div style="padding:100px 20px; text-align:center; color:#666;"><i class="fas fa-exclamation-triangle"></i> 지도를 불러오려면 메인 페이지를 먼저 방문하거나 API키가 필요합니다.</div>';
        return;
    }

    const geocoder = new kakao.maps.services.Geocoder();
    const fullSido = getFullSidoName(sidoName);
    const searchQuery = `${fullSido} ${stationName}`;

    // 1차 시도: "시도명 + 측정소명"으로 검색
    geocoder.addressSearch(searchQuery, function(result, status) {
        if (status === kakao.maps.services.Status.OK) {
            renderMap(result[0].x, result[0].y, stationName);
        } else {
            // 2차 시도: "시도명 + 측정소명 + 측정소" 단어 추가
            geocoder.addressSearch(searchQuery + " 측정소", function(res2, stat2) {
                if (stat2 === kakao.maps.services.Status.OK) {
                    renderMap(res2[0].x, res2[0].y, stationName);
                } else {
                    // 최종 폴백: 주소가 아닌 키워드 장소 검색(Keyword Search) 시도 또는 시도 지역명으로라도 이동
                    console.warn(`'${searchQuery}' 주소 검색 실패. 시도명(${fullSido})으로 대체 시도합니다.`);
                    geocoder.addressSearch(fullSido, function(res3, stat3) {
                        if (stat3 === kakao.maps.services.Status.OK) {
                            renderMap(res3[0].x, res3[0].y, `${fullSido} (상세좌표 없음)`);
                        } else {
                            mapContainer.innerHTML = '<div style="padding:100px 20px; text-align:center; color:#666;"><i class="fas fa-search"></i> 해당 지역의 좌표를 찾을 수 없습니다.</div>';
                        }
                    });
                }
            });
        }
    });

    // 지도 렌더링 헬퍼 함수
    function renderMap(x, y, label) {
        const coords = new kakao.maps.LatLng(y, x);
        const mapOptions = { center: coords, level: 4 };
        const map = new kakao.maps.Map(mapContainer, mapOptions);
        const marker = new kakao.maps.Marker({ map: map, position: coords });
        const infowindow = new kakao.maps.InfoWindow({
            content: `<div style="width:150px;text-align:center;padding:6px 0;font-size:12px;font-weight:bold;">${label}</div>`
        });
        infowindow.open(map, marker);
        setTimeout(() => map.relayout(), 150);
    }
}