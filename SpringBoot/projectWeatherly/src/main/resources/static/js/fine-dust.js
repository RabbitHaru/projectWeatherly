// fine-dust.js - 미세먼지 페이지 전용 JavaScript (API 연동 버전)

// if (typeof API_BASE_URL === 'undefined') {
//     var API_BASE_URL = window.location.origin;
// }

// DOMContentLoaded 이벤트
document.addEventListener('DOMContentLoaded', function() {
    console.log('미세먼지 페이지 로드 완료');

    // GPS 동기화 버튼 이벤트
    setupGpsSync();

    // 페이지 로드 시 IP 기반 데이터 로드
    loadInitialFineDustData();

    // 5분마다 데이터 새로고침
    setInterval(loadFineDustData, 300000);
    setInterval(loadRegionalComparison, 300000);
});

async function loadInitialFineDustData() {
    try {
        showLoading('대기질 정보를 불러오는 중...');

        // 1. 현재 대기질 데이터 로드
        await loadFineDustData();

        // 2. 지역별 비교 데이터 로드
        await loadRegionalComparison();

        // 3. 예보 데이터 로드 (현재 위치 기준)
        const locationElement = document.getElementById('fine-dust-location');
        const sidoName = locationElement ? locationElement.textContent : '서울';
        const extractedSido = extractSidoName(sidoName);
        await loadAirQualityForecastForTabs(extractedSido);

        hideLoading();
    } catch (error) {
        console.error('초기 데이터 로드 실패:', error);
        hideLoading();
        showNotification('데이터를 불러오는데 실패했습니다.', 'error');
    }
}

// 로딩 화면 표시
function showLoading(message) {
    let loadingEl = document.getElementById('fine-dust-loading');
    if (!loadingEl) {
        loadingEl = document.createElement('div');
        loadingEl.id = 'fine-dust-loading';
        document.body.appendChild(loadingEl);
    }

    loadingEl.innerHTML = `
        <div class="loading-content" style="background: rgba(0,0,0,0.8); padding: 30px; border-radius: 15px; color: white; text-align: center;">
            <i class="fas fa-spinner fa-spin fa-2x"></i>
            <p style="margin-top: 15px;">${message}</p>
        </div>
    `;
    loadingEl.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0, 0, 0, 0.5); display: flex; justify-content: center;
        align-items: center; z-index: 9999;
    `;
}

function hideLoading() {
    const loadingEl = document.getElementById('fine-dust-loading');
    if (loadingEl) loadingEl.remove();
}

// 시도명 추출 (API 요청용)
function extractSidoName(regionName) {
    if (!regionName) return '서울';
    if (regionName.includes('특별시') || regionName.includes('광역시')) {
        return regionName.substring(0, 2);
    }
    if (regionName.endsWith('도')) {
        return regionName.substring(0, regionName.length() - 1);
    }
    return regionName;
}

// GPS 동기화 설정
function setupGpsSync() {
    const gpsBtn = document.getElementById('fine-dust-gps-sync-btn');
    if (!gpsBtn) return;

    gpsBtn.addEventListener('click', function() {
        syncLocationAndLoadData();
    });
}

// 위치 동기화 및 데이터 로드
async function syncLocationAndLoadData() {
    const gpsBtn = document.getElementById('fine-dust-gps-sync-btn');
    const originalHTML = gpsBtn.innerHTML;
    gpsBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 동기화 중...';
    gpsBtn.disabled = true;

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            async function(position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;

                try {
                    showLoading('위치 기반 대기질 정보를 불러오는 중...');
                    const success = await loadAirQualityDataByGPS(lat, lng);

                    if (success) {
                        gpsBtn.innerHTML = '<i class="fas fa-check-circle"></i> 완료';
                        gpsBtn.classList.add('sync-success');
                        setTimeout(() => {
                            gpsBtn.innerHTML = originalHTML;
                            gpsBtn.disabled = false;
                            gpsBtn.classList.remove('sync-success');
                        }, 2000);
                        showNotification('위치 정보가 업데이트되었습니다.', 'success');
                    }
                } catch (error) {
                    console.error('GPS 데이터 로드 실패:', error);
                    gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 실패';
                    gpsBtn.classList.add('sync-error');
                    setTimeout(() => {
                        gpsBtn.innerHTML = originalHTML;
                        gpsBtn.disabled = false;
                        gpsBtn.classList.remove('sync-error');
                    }, 2000);
                } finally {
                    hideLoading();
                }
            },
            function(error) {
                console.error('GPS 오류:', error);
                alert("위치 정보를 가져올 수 없습니다.");
                gpsBtn.innerHTML = originalHTML;
                gpsBtn.disabled = false;
                hideLoading();
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    } else {
        alert("이 브라우저는 GPS를 지원하지 않습니다.");
        gpsBtn.innerHTML = originalHTML;
        gpsBtn.disabled = false;
    }
}

// 알림 표시 (토스트 메시지)
function showNotification(message, type = 'info') {
    const existingToast = document.querySelector('.notification-toast');
    if (existingToast) existingToast.remove();

    const toast = document.createElement('div');
    toast.className = `notification-toast ${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed; top: 20px; right: 20px; padding: 15px 20px;
        background: ${type === 'error' ? '#e74c3c' : '#2ecc71'};
        color: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000; animation: slideIn 0.3s ease-out;
    `;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ==========================================
// [API 호출] 현재 대기질 데이터 로드
// ==========================================
async function loadFineDustData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();

        if (data.success && data.data) {
            updateFineDustUI(data.data);
            return true;
        } else {
            console.warn('API 데이터 없음');
            return false;
        }
    } catch (error) {
        console.error('미세먼지 데이터 로드 실패:', error);
        return false;
    }
}

// ==========================================
// [API 호출] GPS 기반 대기질 데이터 로드
// ==========================================
async function loadAirQualityDataByGPS(latitude, longitude) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST'
        });
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();

        if (data.success && data.data) {
            updateFineDustUI(data.data);

            // GPS 위치 기반 예보도 로드
            if (data.data.sidoName) {
                const extractedSido = extractSidoName(data.data.sidoName);
                await loadAirQualityForecastForTabs(extractedSido);
            }
            return true;
        }
        return false;
    } catch (error) {
        console.error('GPS 데이터 로드 실패:', error);
        return false;
    }
}

// ==========================================
// [API 호출] 예보 데이터 로드
// ==========================================
async function loadAirQualityForecastForTabs(sidoName) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sidoName)}`);
        const data = await response.json();

        if (data.success && data.data) {
            updateFineDustForecast(data.data);
        } else {
            updateFineDustForecast([]); // 데이터 없음 처리
        }
    } catch (error) {
        console.error('예보 데이터 로드 실패:', error);
        updateFineDustForecast([]);
    }
}

// ==========================================
// [API 호출] 지역별 비교 데이터 로드
// ==========================================
async function loadRegionalComparison() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/compare?sidoNames=서울,부산,대구,인천,광주,대전,울산,경기,제주`);
        const data = await response.json();

        if (data.success && data.data) {
            updateRegionalComparisonUI(data.data);
        }
    } catch (error) {
        console.error('지역별 비교 데이터 로드 실패:', error);
    }
}

// ==========================================
// [UI 업데이트] 메인 UI 갱신
// ==========================================
function updateFineDustUI(airQuality) {
    if (!airQuality) return;

    // 1. 위치 정보 (지역명 '서울' -> '서울특별시'로 변환)
    if (airQuality.sidoName) {
        const locEl = document.getElementById('fine-dust-location');
        if (locEl) {
            // 짧은 지역명을 긴 행정구역명으로 매핑
            const fullRegionMap = {
                '서울': '서울특별시',
                '부산': '부산광역시',
                '대구': '대구광역시',
                '인천': '인천광역시',
                '광주': '광주광역시',
                '대전': '대전광역시',
                '울산': '울산광역시',
                '세종': '세종특별자치시',
                '경기': '경기도',
                '강원': '강원특별자치도',
                '충북': '충청북도',
                '충남': '충청남도',
                '전북': '전북특별자치도',
                '전남': '전라남도',
                '경북': '경상북도',
                '경남': '경상남도',
                '제주': '제주특별자치도'
            };

            // 매핑된 이름이 있으면 사용, 없으면 원래 이름 사용
            locEl.textContent = fullRegionMap[airQuality.sidoName] || airQuality.sidoName;
        }
    }

    // 측정소 및 시간
    if (airQuality.stationName) document.getElementById('station-name').textContent = airQuality.stationName;

    if (airQuality.dataTime) {
        const date = new Date(airQuality.dataTime);
        const timeStr = date.toLocaleString('ko-KR', {
            month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
        document.getElementById('fine-dust-current-time').textContent = timeStr;
        document.getElementById('measurement-time').textContent = timeStr;
        document.getElementById('fine-dust-update-time').textContent = `업데이트: ${timeStr}`;
    }

    // 2. 전체 등급 (배지)
    if (airQuality.overallStatus && airQuality.overallGrade) {
        const badge = document.getElementById('fine-dust-overall-badge');
        if (badge) {
            badge.textContent = airQuality.overallStatus;
            badge.className = 'aqi-badge large-badge ' + getAqiClass(airQuality.overallGrade);
        }
    }

    // 3. 건강 조언 (상단 & 하단 카드)
    if (airQuality.healthAdvice) {
        document.getElementById('fine-dust-health-advice').textContent = airQuality.healthAdvice;
        document.getElementById('advice-description').textContent = airQuality.healthAdvice;
    }
    if (airQuality.overallStatus) {
        document.getElementById('advice-title').textContent = `현재 대기질: ${airQuality.overallStatus}`;
    }

    // 4. 상세 지수 업데이트
    const updateElement = (prefix, data) => {
        if (!data) return;
        const valueEl = document.getElementById(`${prefix}-value`);
        const statusEl = document.getElementById(`${prefix}-status`);
        const gradeEl = document.getElementById(`${prefix}-grade`);

        if (valueEl) valueEl.textContent = `${data.value || '--'} ${data.unit || ''}`;
        if (statusEl) {
            statusEl.textContent = data.status || '--';
            statusEl.className = 'aqi-status ' + getAqiClass(data.grade || '2');
        }
        if (gradeEl) gradeEl.textContent = `등급: ${data.grade || '--'}`;
    };

    updateElement('khai', airQuality.khai); // KHAI 추가
    updateElement('pm10-detail', airQuality.pm10);
    updateElement('pm25-detail', airQuality.pm25);
    updateElement('o3-detail', airQuality.o3);
    updateElement('no2', airQuality.no2);
    updateElement('co', airQuality.co);

    updateAirQualityForecast(airQuality.forecasts || []);
}

// 상세 지수 업데이트 헬퍼
function updateAirQualityElement(prefix, dataObj) {
    if (!dataObj) return;

    // HTML ID 찾기
    const valueEl = document.getElementById(`${prefix}-value`);
    const statusEl = document.getElementById(`${prefix}-status`);
    const gradeEl = document.getElementById(`${prefix}-grade`);

    if (valueEl) valueEl.textContent = `${dataObj.value ?? '--'} ${dataObj.unit ?? ''}`;
    if (statusEl) {
        statusEl.textContent = dataObj.status ?? '--';
        statusEl.className = 'aqi-detail-status ' + getAqiClass(dataObj.grade);
    }
    if (gradeEl) gradeEl.textContent = `등급: ${dataObj.grade ?? '--'}`;
}

// ==========================================
// [UI 업데이트] 예보 탭 갱신
// ==========================================
function updateFineDustForecast(forecasts) {
    const todayContainer = document.getElementById('today-aqi-forecast');
    const tomorrowContainer = document.getElementById('tomorrow-aqi-forecast');
    const weeklyContainer = document.getElementById('weekly-aqi-forecast');

    if (!forecasts || forecasts.length === 0) {
        const noDataHtml = '<div class="no-data">예보 정보가 없습니다.</div>';
        if (todayContainer) todayContainer.innerHTML = noDataHtml;
        if (tomorrowContainer) tomorrowContainer.innerHTML = noDataHtml;
        if (weeklyContainer) weeklyContainer.innerHTML = noDataHtml;
        return;
    }

    // 1. 오늘 예보
    if (todayContainer && forecasts[0]) {
        renderForecastCard(todayContainer, forecasts[0], '오늘');
    }

    // 2. 내일 예보
    if (tomorrowContainer && forecasts[1]) {
        renderForecastCard(tomorrowContainer, forecasts[1], '내일');
    }

    // 3. 주간 예보 (전체 리스트)
    if (weeklyContainer) {
        weeklyContainer.innerHTML = '';
        forecasts.slice(0, 5).forEach((fc, idx) => { // 최대 5일치
            renderForecastCard(weeklyContainer, fc, fc.date, true); // true = append 모드
        });
    }
}

// 예보 카드 렌더링 헬퍼
function renderForecastCard(container, forecast, dateLabel, append = false) {
    const html = `
        <div class="aqi-forecast-day">
            <div class="forecast-date">${dateLabel} (${forecast.date})</div>
            <div class="forecast-overall ${getAqiClass(forecast.overallGrade)}">
                ${getAqiStatusText(forecast.overallGrade)}
            </div>
            <div class="forecast-details">
                <p><i class="fas fa-smog"></i> 미세먼지: ${getAqiStatusText(forecast.pm10Grade)}</p>
                <p><i class="fas fa-wind"></i> 초미세먼지: ${getAqiStatusText(forecast.pm25Grade)}</p>
                <p class="forecast-advice"><i class="fas fa-comment-alt"></i> ${forecast.advice}</p>
            </div>
        </div>
    `;

    if (append) {
        container.innerHTML += html;
    } else {
        container.innerHTML = html;
    }
}

// ==========================================
// [UI 업데이트] 지역별 비교 리스트 갱신
// ==========================================
function updateRegionalComparisonUI(regionalData) {
    const container = document.getElementById('regional-aqi-list');
    if (!container) return;

    container.innerHTML = '';

    regionalData.forEach(region => {
        const div = document.createElement('div');
        div.className = 'regional-aqi-item';
        div.innerHTML = `
            <span class="regional-aqi-name">${region.sidoName}</span>
            <span class="regional-aqi-badge ${getAqiClass(region.overallGrade)}">
                ${region.overallStatus}
            </span>
        `;
        container.appendChild(div);
    });
}

// ==========================================
// [유틸리티] 클래스 및 텍스트 변환
// ==========================================
function getAqiClass(grade) {
    grade = String(grade).trim();
    switch(grade) {
        case '1': return 'aqi-good';
        case '2': return 'aqi-moderate';
        case '3': return 'aqi-bad';
        case '4': return 'aqi-very-bad';
        default: return 'aqi-moderate';
    }
}

function getAqiStatusText(grade) {
    grade = String(grade).trim();
    switch(grade) {
        case '1': return '좋음';
        case '2': return '보통';
        case '3': return '나쁨';
        case '4': return '매우나쁨';
        default: return '보통';
    }
}