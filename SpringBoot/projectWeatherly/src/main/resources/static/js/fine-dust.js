// fine-dust.js - 미세먼지 페이지 전용 JavaScript (개선 버전)

if (typeof API_BASE_URL === 'undefined') {
    var API_BASE_URL = window.location.origin;
}

// DOMContentLoaded 이벤트
document.addEventListener('DOMContentLoaded', function() {
    console.log('미세먼지 페이지 로드 완료');

    // 다크모드 설정
    setupDarkMode();

    // 현재 시간 업데이트
    updateCurrentTime();
    setInterval(updateCurrentTime, 60000);

    // 탭 전환 기능
    setupTabSwitching();

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

        // 순차적으로 로드 (예보는 기본 데이터 로드 후)
        await loadFineDustData();
        await loadRegionalComparison();

        // 현재 위치 기반 예보 로드
        const locationElement = document.getElementById('fine-dust-location');
        const sidoName = locationElement ? locationElement.textContent : '서울';
        const extractedSido = extractSidoName(sidoName);
        await loadAirQualityForecastForTabs(extractedSido);

        hideLoading();
    } catch (error) {
        console.error('초기 데이터 로드 실패:', error);
        hideLoading();
        showNotification('데이터를 불러오는데 실패했습니다.', 'error');
        // 폴백 데이터 표시
        showFallbackFineDustData();
    }
}

function showLoading(message) {
    // 이미 로딩 중이면 업데이트만
    let loadingEl = document.getElementById('fine-dust-loading');
    if (!loadingEl) {
        loadingEl = document.createElement('div');
        loadingEl.id = 'fine-dust-loading';
        document.body.appendChild(loadingEl);
    }

    loadingEl.innerHTML = `
        <div class="loading-content">
            <i class="fas fa-spinner fa-spin fa-2x"></i>
            <p>${message}</p>
        </div>
    `;
    loadingEl.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.8);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 9999;
        color: white;
        text-align: center;
    `;
}

function hideLoading() {
    const loadingEl = document.getElementById('fine-dust-loading');
    if (loadingEl) {
        loadingEl.remove();
    }
}

async function loadAirQualityForecastForTabs(sidoName) {
    try {
        if (!sidoName) {
            sidoName = '서울'; // 기본값
        }

        const response = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sidoName)}`);
        const data = await response.json();

        console.log('대기질 예보 데이터:', data);

        if (data.success && data.data && Array.isArray(data.data)) {
            updateFineDustForecast(data.data);
        } else {
            // 예보 데이터가 없으면 기본 예보 표시
            updateFineDustForecast([]);
        }
    } catch (error) {
        console.error('대기질 예보 데이터 로드 실패:', error);
        // 예보 데이터가 없어도 계속 진행
        updateFineDustForecast([]);
    }
}

function extractSidoName(regionName) {
    if (!regionName) return '서울';

    // "서울특별시" → "서울", "부산광역시" → "부산"
    if (regionName.includes('특별시') || regionName.includes('광역시')) {
        return regionName.substring(0, 2);
    }

    // "경기도" → "경기"
    if (regionName.endsWith('도')) {
        return regionName.substring(0, regionName.length() - 1);
    }

    return regionName;
}

// 다크모드 설정 함수
function setupDarkMode() {
    const darkModeToggle = document.getElementById('darkmode-toggle');
    const body = document.body;
    const isDarkMode = localStorage.getItem('darkMode') === 'true';

    if (isDarkMode) {
        body.classList.add('dark-mode');
        updateDarkModeIcon(true);
    }

    darkModeToggle.addEventListener('click', function() {
        const isCurrentlyDark = body.classList.contains('dark-mode');
        if (isCurrentlyDark) {
            body.classList.remove('dark-mode');
            localStorage.setItem('darkMode', 'false');
            updateDarkModeIcon(false);
        } else {
            body.classList.add('dark-mode');
            localStorage.setItem('darkMode', 'true');
            updateDarkModeIcon(true);
        }
    });
}

// 다크모드 아이콘 업데이트
function updateDarkModeIcon(isDarkMode) {
    const icon = document.querySelector('#darkmode-toggle i');
    const button = document.getElementById('darkmode-toggle');
    if (isDarkMode) {
        icon.className = 'fas fa-sun';
        button.title = '라이트모드';
    } else {
        icon.className = 'fas fa-moon';
        button.title = '다크모드';
    }
}

// 현재 시간 업데이트
function updateCurrentTime() {
    const now = new Date();
    const options = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long',
        hour: '2-digit',
        minute: '2-digit'
    };
    const timeElement = document.getElementById('fine-dust-current-time');
    if (timeElement) {
        timeElement.textContent = now.toLocaleDateString('ko-KR', options);
    }
}

// 탭 전환 설정
function setupTabSwitching() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            this.classList.add('active');
            const targetTab = document.getElementById(`tab-${tabId}`);
            if (targetTab) {
                targetTab.classList.add('active');

                // 탭 변경 시 해당 탭 데이터 새로고침
                if (tabId === 'today-aqi' || tabId === 'tomorrow-aqi' || tabId === 'weekly-aqi') {
                    const locationElement = document.getElementById('fine-dust-location');
                    const sidoName = locationElement ? locationElement.textContent : '서울';
                    const extractedSido = extractSidoName(sidoName);
                    loadAirQualityForecastForTabs(extractedSido);
                }
            }
        });
    });
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

                    // GPS 기반 대기질 데이터 로드
                    const success = await loadAirQualityDataByGPS(lat, lng);

                    if (success) {
                        // 버튼 상태 복원
                        gpsBtn.innerHTML = '<i class="fas fa-check-circle"></i> 동기화 완료';
                        gpsBtn.classList.add('sync-success');

                        setTimeout(() => {
                            gpsBtn.innerHTML = originalHTML;
                            gpsBtn.disabled = false;
                            gpsBtn.classList.remove('sync-success');
                        }, 2000);

                        showNotification('위치 정보가 업데이트되었습니다.', 'success');
                    } else {
                        throw new Error('GPS 데이터 로드 실패');
                    }

                } catch (error) {
                    console.error('GPS 데이터 로드 실패:', error);
                    gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 실패';
                    gpsBtn.classList.add('sync-error');

                    showNotification('위치 기반 대기질 정보를 불러오는데 실패했습니다.', 'error');

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
                gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 실패';
                gpsBtn.classList.add('sync-error');

                // 오류 메시지 표시
                const errorMessage = getGeolocationErrorMessage(error);
                showNotification(errorMessage, 'error');

                setTimeout(() => {
                    gpsBtn.innerHTML = originalHTML;
                    gpsBtn.disabled = false;
                    gpsBtn.classList.remove('sync-error');
                }, 2000);
                hideLoading();
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0
            }
        );
    } else {
        gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 미지원';
        showNotification('이 브라우저는 GPS를 지원하지 않습니다.', 'error');

        setTimeout(() => {
            gpsBtn.innerHTML = originalHTML;
            gpsBtn.disabled = false;
        }, 2000);
    }
}

// GPS 오류 메시지 가져오기
function getGeolocationErrorMessage(error) {
    switch(error.code) {
        case error.PERMISSION_DENIED:
            return "위치 정보 사용이 거부되었습니다. 브라우저 설정에서 위치 권한을 허용해주세요.";
        case error.POSITION_UNAVAILABLE:
            return "위치 정보를 가져올 수 없습니다.";
        case error.TIMEOUT:
            return "위치 정보 요청이 시간 초과되었습니다.";
        default:
            return "위치 정보를 가져오는 중 오류가 발생했습니다.";
    }
}

// 알림 표시
function showNotification(message, type = 'info') {
    // 이미 알림이 표시 중이면 제거
    const existingToast = document.querySelector('.notification-toast');
    if (existingToast) {
        existingToast.remove();
    }

    // 간단한 토스트 메시지 구현
    const toast = document.createElement('div');
    toast.className = `notification-toast ${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        background: ${type === 'error' ? '#e74c3c' : (type === 'success' ? '#2ecc71' : '#3498db')};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        animation: slideIn 0.3s ease-out;
        max-width: 300px;
        word-wrap: break-word;
    `;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            if (toast.parentNode) {
                document.body.removeChild(toast);
            }
        }, 300);
    }, 3000);

    // 애니메이션 스타일 추가
    if (!document.getElementById('toast-animations')) {
        const style = document.createElement('style');
        style.id = 'toast-animations';
        style.textContent = `
            @keyframes slideIn {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
            @keyframes slideOut {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
        `;
        document.head.appendChild(style);
    }
}

// 미세먼지 데이터 로드
async function loadFineDustData() {
    try {
        console.log('미세먼지 데이터 로드 시작...');
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('미세먼지 API 응답:', data);

        if (data.success && data.data) {
            updateFineDustUI(data.data);
            return true;
        } else {
            console.error('미세먼지 API 실패:', data.message);
            showFallbackFineDustData();
            showNotification('대기질 정보를 불러오는데 실패했습니다.', 'error');
            return false;
        }
    } catch (error) {
        console.error('미세먼지 데이터 로드 실패:', error);
        showFallbackFineDustData();
        showNotification('네트워크 오류로 대기질 정보를 불러올 수 없습니다.', 'error');
        return false;
    }
}

// GPS 기반 대기질 데이터 로드
async function loadAirQualityDataByGPS(latitude, longitude) {
    try {
        console.log('GPS 기반 대기질 데이터 로드:', latitude, longitude);
        const response = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${latitude}&longitude=${longitude}`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('GPS 대기질 응답:', data);

        if (data.success && data.data) {
            updateFineDustUI(data.data);

            // GPS 위치 기반 예보도 로드
            if (data.data.sidoName) {
                const extractedSido = extractSidoName(data.data.sidoName);
                await loadAirQualityForecastForTabs(extractedSido);
            }

            return true;
        } else {
            throw new Error(data.message || 'GPS 데이터 로드 실패');
        }
    } catch (error) {
        console.error('GPS 대기질 데이터 로드 실패:', error);
        showNotification('위치 기반 대기질 정보를 불러오는데 실패했습니다.', 'error');

        // 실패 시 기본 데이터 로드
        await loadFineDustData();
        return false;
    }
}

// 지역별 비교 데이터 로드
async function loadRegionalComparison() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/compare?sidoNames=서울,부산,대구,인천,광주,대전,울산`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.success && data.data && Array.isArray(data.data)) {
            updateRegionalComparisonUI(data.data);
            return true;
        } else {
            throw new Error('지역별 비교 데이터 없음');
        }
    } catch (error) {
        console.error('지역별 비교 데이터 로드 실패:', error);
        // 실패 시 기본 데이터 표시
        updateRegionalComparisonUI([
            { sidoName: '서울', overallGrade: '2' },
            { sidoName: '부산', overallGrade: '1' },
            { sidoName: '대구', overallGrade: '2' },
            { sidoName: '인천', overallGrade: '2' },
            { sidoName: '광주', overallGrade: '1' },
            { sidoName: '대전', overallGrade: '2' },
            { sidoName: '울산', overallGrade: '1' }
        ]);
        return false;
    }
}

// 미세먼지 UI 업데이트
function updateFineDustUI(airQuality) {
    console.log('대기질 데이터 업데이트:', airQuality);

    if (!airQuality) {
        console.error('대기질 데이터 없음');
        showFallbackFineDustData();
        return;
    }

    // 위치 정보
    if (airQuality.sidoName) {
        const locationElement = document.getElementById('fine-dust-location');
        if (locationElement) {
            locationElement.textContent = airQuality.sidoName;
        }
    }

    if (airQuality.stationName) {
        const stationElement = document.getElementById('station-name');
        if (stationElement) {
            stationElement.textContent = airQuality.stationName;
        }
    }

    // 측정 시간
    if (airQuality.dataTime) {
        try {
            const date = new Date(airQuality.dataTime);
            if (!isNaN(date.getTime())) {
                const timeString = date.toLocaleString('ko-KR', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });

                const measurementTimeElement = document.getElementById('measurement-time');
                const updateTimeElement = document.getElementById('fine-dust-update-time');

                if (measurementTimeElement) {
                    measurementTimeElement.textContent = timeString;
                }
                if (updateTimeElement) {
                    updateTimeElement.textContent = `업데이트: ${timeString}`;
                }
            }
        } catch (e) {
            console.error('시간 파싱 오류:', e);
        }
    }

    // 전체 등급
    if (airQuality.overallStatus && airQuality.overallGrade) {
        const badge = document.getElementById('fine-dust-overall-badge');
        if (badge) {
            badge.textContent = airQuality.overallStatus;
            badge.className = 'aqi-badge large-badge ' + getAqiClass(airQuality.overallGrade);
        }
    }

    // 건강 조언
    if (airQuality.healthAdvice) {
        const healthAdviceElement = document.getElementById('fine-dust-health-advice');
        const adviceDescriptionElement = document.getElementById('advice-description');

        if (healthAdviceElement) {
            healthAdviceElement.textContent = airQuality.healthAdvice;
        }
        if (adviceDescriptionElement) {
            adviceDescriptionElement.textContent = airQuality.healthAdvice;
        }
    }

    // 통합대기환경지수 (KHAI)
    updateAirQualityElement('khai', airQuality.khai);

    // PM10
    updateAirQualityElement('pm10-detail', airQuality.pm10);

    // PM2.5
    updateAirQualityElement('pm25-detail', airQuality.pm25);

    // 오존
    updateAirQualityElement('o3-detail', airQuality.o3);

    // 이산화질소
    updateAirQualityElement('no2', airQuality.no2);

    // 일산화탄소
    updateAirQualityElement('co', airQuality.co);

    // 건강 조언 제목
    if (airQuality.overallStatus) {
        const adviceTitleElement = document.getElementById('advice-title');
        if (adviceTitleElement) {
            adviceTitleElement.textContent = `현재 대기질: ${airQuality.overallStatus}`;
        }
    }
}

// 대기질 요소 업데이트 헬퍼 함수
function updateAirQualityElement(prefix, data) {
    if (!data) return;

    const valueElement = document.getElementById(`${prefix}-value`);
    const statusElement = document.getElementById(`${prefix}-status`);
    const gradeElement = document.getElementById(`${prefix}-grade`);

    if (valueElement) {
        valueElement.textContent = `${data.value || '--'} ${data.unit || ''}`;
    }

    if (statusElement) {
        statusElement.textContent = data.status || '--';
        statusElement.className = 'aqi-detail-status ' + getAqiClass(data.grade);
    }

    if (gradeElement) {
        gradeElement.textContent = `등급: ${data.grade || '--'}`;
    }
}

// 지역별 비교 UI 업데이트
function updateRegionalComparisonUI(regionalData) {
    const container = document.getElementById('regional-aqi-list');
    if (!container) return;

    container.innerHTML = '';

    if (!regionalData || !Array.isArray(regionalData) || regionalData.length === 0) {
        container.innerHTML = '<div class="no-data">지역별 비교 데이터가 없습니다.</div>';
        return;
    }

    regionalData.forEach(region => {
        if (region && region.sidoName && region.overallGrade) {
            const element = document.createElement('div');
            element.className = 'regional-aqi-item';
            element.innerHTML = `
                <span class="regional-aqi-name">${region.sidoName}</span>
                <span class="regional-aqi-badge ${getAqiClass(region.overallGrade)}">
                    ${getAqiStatusText(region.overallGrade)}
                </span>
            `;
            container.appendChild(element);
        }
    });
}

// 미세먼지 예보 업데이트
function updateFineDustForecast(forecasts) {
    const todayContainer = document.getElementById('today-aqi-forecast');
    const tomorrowContainer = document.getElementById('tomorrow-aqi-forecast');
    const weeklyContainer = document.getElementById('weekly-aqi-forecast');

    // 데이터가 없을 경우 처리
    if (!forecasts || !Array.isArray(forecasts) || forecasts.length === 0) {
        const noDataHTML = '<div class="no-data">예보 데이터를 불러올 수 없습니다.</div>';

        if (todayContainer) todayContainer.innerHTML = noDataHTML;
        if (tomorrowContainer) tomorrowContainer.innerHTML = noDataHTML;
        if (weeklyContainer) weeklyContainer.innerHTML = noDataHTML;
        return;
    }

    // 오늘 예보
    if (todayContainer) {
        const today = forecasts[0] || {};
        todayContainer.innerHTML = `
            <div class="aqi-forecast-day">
                <div class="forecast-date">${today.date || '오늘'}</div>
                <div class="forecast-overall ${getAqiClass(today.overallGrade || '2')}">
                    ${getAqiStatusText(today.overallGrade || '2')}
                </div>
                <div class="forecast-details">
                    <p><i class="fas fa-smog"></i> PM10: ${getAqiStatusText(today.pm10Grade || '2')}</p>
                    <p><i class="fas fa-wind"></i> PM2.5: ${getAqiStatusText(today.pm25Grade || '2')}</p>
                    <p class="forecast-advice"><i class="fas fa-lightbulb"></i> ${today.advice || '대기질 정보를 확인해주세요.'}</p>
                </div>
            </div>
        `;
    }

    // 내일 예보
    if (tomorrowContainer) {
        const tomorrow = forecasts[1] || {};
        tomorrowContainer.innerHTML = `
            <div class="aqi-forecast-day">
                <div class="forecast-date">${tomorrow.date || '내일'}</div>
                <div class="forecast-overall ${getAqiClass(tomorrow.overallGrade || '2')}">
                    ${getAqiStatusText(tomorrow.overallGrade || '2')}
                </div>
                <div class="forecast-details">
                    <p><i class="fas fa-smog"></i> PM10: ${getAqiStatusText(tomorrow.pm10Grade || '2')}</p>
                    <p><i class="fas fa-wind"></i> PM2.5: ${getAqiStatusText(tomorrow.pm25Grade || '2')}</p>
                    <p class="forecast-advice"><i class="fas fa-lightbulb"></i> ${tomorrow.advice || '대기질 정보를 확인해주세요.'}</p>
                </div>
            </div>
        `;
    }

    // 주간 예보
    if (weeklyContainer) {
        weeklyContainer.innerHTML = '';

        // 최대 3개의 예보만 표시
        const displayForecasts = forecasts.slice(0, 3);

        if (displayForecasts.length === 0) {
            weeklyContainer.innerHTML = '<div class="no-data">주간 예보 데이터가 없습니다.</div>';
            return;
        }

        displayForecasts.forEach((forecast, index) => {
            const element = document.createElement('div');
            element.className = 'aqi-forecast-day';
            element.innerHTML = `
                <div class="forecast-date">${forecast.date || `예보 ${index + 1}`}</div>
                <div class="forecast-overall ${getAqiClass(forecast.overallGrade || '2')}">
                    ${getAqiStatusText(forecast.overallGrade || '2')}
                </div>
                <div class="forecast-details">
                    <p><i class="fas fa-smog"></i> PM10: ${getAqiStatusText(forecast.pm10Grade || '2')}</p>
                    <p><i class="fas fa-wind"></i> PM2.5: ${getAqiStatusText(forecast.pm25Grade || '2')}</p>
                </div>
            `;
            weeklyContainer.appendChild(element);
        });
    }
}

// 폴백 데이터 표시
function showFallbackFineDustData() {
    console.log('폴백 대기질 데이터 표시');

    // 기본 데이터
    const fallbackData = {
        sidoName: '서울특별시',
        stationName: '중구',
        dataTime: new Date().toISOString(),
        overallStatus: '보통',
        overallGrade: '2',
        healthAdvice: '대기질이 보통입니다. 민감한 분들은 주의가 필요합니다.',
        khai: { value: 75, status: '보통', grade: '2', unit: '' },
        pm10: { value: 35, status: '좋음', grade: '1', unit: '㎍/㎥' },
        pm25: { value: 15, status: '좋음', grade: '1', unit: '㎍/㎥' },
        o3: { value: 0.025, status: '좋음', grade: '1', unit: 'ppm' },
        no2: { value: 0.02, status: '좋음', grade: '1', unit: 'ppm' },
        co: { value: 0.5, status: '좋음', grade: '1', unit: 'ppm' }
    };

    // UI 업데이트
    updateFineDustUI(fallbackData);

    // 기본 지역별 비교 데이터
    updateRegionalComparisonUI([
        { sidoName: '서울', overallGrade: '2' },
        { sidoName: '부산', overallGrade: '1' },
        { sidoName: '대구', overallGrade: '2' },
        { sidoName: '인천', overallGrade: '2' },
        { sidoName: '광주', overallGrade: '1' },
        { sidoName: '대전', overallGrade: '2' },
        { sidoName: '울산', overallGrade: '1' }
    ]);

    // 예보 데이터 없음 표시
    updateFineDustForecast([]);
}

// 공통 유틸리티 함수
function getAqiClass(grade) {
    if (!grade) return 'aqi-moderate';

    switch(String(grade).trim()) {
        case '1': return 'aqi-good';
        case '2': return 'aqi-moderate';
        case '3': return 'aqi-bad';
        case '4': return 'aqi-very-bad';
        default: return 'aqi-moderate';
    }
}

function getAqiStatusText(grade) {
    if (!grade) return '보통';

    switch(String(grade).trim()) {
        case '1': return '좋음';
        case '2': return '보통';
        case '3': return '나쁨';
        case '4': return '매우나쁨';
        default: return '보통';
    }
}

// API 테스트 함수
async function testApiConnection() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/health-check`);
        const data = await response.json();
        console.log('API 연결 테스트 결과:', {
            status: response.status,
            success: data.success,
            message: data.message
        });
        return data;
    } catch (error) {
        console.error('API 연결 테스트 실패:', error);
        return null;
    }
}

// 데이터 새로고침 버튼 이벤트 추가
function addRefreshButton() {
    // 이미 버튼이 있으면 추가하지 않음
    if (document.getElementById('refresh-data-btn')) return;

    const refreshBtn = document.createElement('button');
    refreshBtn.id = 'refresh-data-btn';
    refreshBtn.className = 'control-btn refresh-btn';
    refreshBtn.innerHTML = '<i class="fas fa-sync-alt"></i>';
    refreshBtn.title = '데이터 새로고침';
    refreshBtn.style.cssText = `
        position: fixed;
        bottom: 30px;
        right: 30px;
        z-index: 1000;
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: var(--primary-color, #3498db);
        color: white;
        border: none;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        cursor: pointer;
        font-size: 1.5rem;
        transition: all 0.3s ease;
    `;

    refreshBtn.addEventListener('click', async function() {
        this.style.transform = 'rotate(360deg)';

        showLoading('데이터를 새로고침하는 중...');

        try {
            await Promise.all([
                loadFineDustData(),
                loadRegionalComparison()
            ]);

            showNotification('데이터가 새로고침되었습니다.', 'success');
        } catch (error) {
            console.error('데이터 새로고침 실패:', error);
            showNotification('데이터 새로고침에 실패했습니다.', 'error');
        } finally {
            setTimeout(() => {
                this.style.transform = 'rotate(0deg)';
            }, 500);
            hideLoading();
        }
    });

    refreshBtn.addEventListener('mouseenter', function() {
        this.style.transform = 'scale(1.1)';
    });

    refreshBtn.addEventListener('mouseleave', function() {
        this.style.transform = 'scale(1)';
    });

    document.body.appendChild(refreshBtn);
}

// 페이지 로드 시 새로고침 버튼 추가
window.addEventListener('load', function() {
    setTimeout(() => {
        addRefreshButton();
        // API 연결 테스트
        testApiConnection();
    }, 1000);
});