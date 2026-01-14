// fine-dust.js - 미세먼지 페이지 전용 JavaScript

// API 기본 URL
const API_BASE_URL = window.location.origin;

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

    // 데이터 로드
    loadFineDustData();
    loadRegionalComparison();

    // 5분마다 데이터 새로고침
    setInterval(loadFineDustData, 300000);
    setInterval(loadRegionalComparison, 300000);
});

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
function syncLocationAndLoadData() {
    const gpsBtn = document.getElementById('fine-dust-gps-sync-btn');
    const originalHTML = gpsBtn.innerHTML;
    gpsBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 동기화 중...';
    gpsBtn.disabled = true;

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;

                // GPS 기반 대기질 데이터 로드
                loadAirQualityDataByGPS(lat, lng);

                // 버튼 상태 복원
                gpsBtn.innerHTML = '<i class="fas fa-check-circle"></i> 동기화 완료';
                gpsBtn.classList.add('sync-success');

                setTimeout(() => {
                    gpsBtn.innerHTML = originalHTML;
                    gpsBtn.disabled = false;
                    gpsBtn.classList.remove('sync-success');
                }, 2000);
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

                // GPS 실패 시 기본 데이터 로드
                loadFineDustData();
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0
            }
        );
    } else {
        gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 미지원';
        setTimeout(() => {
            gpsBtn.innerHTML = originalHTML;
            gpsBtn.disabled = false;
        }, 2000);
        loadFineDustData();
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
    // 간단한 토스트 메시지 구현
    const toast = document.createElement('div');
    toast.className = `notification-toast ${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        background: ${type === 'error' ? '#e74c3c' : '#2ecc71'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        animation: slideIn 0.3s ease-out;
    `;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            document.body.removeChild(toast);
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
        const data = await response.json();

        console.log('미세먼지 API 응답:', data);

        if (data.success) {
            updateFineDustUI(data.data);
        } else {
            console.error('미세먼지 API 실패:', data.message);
            showFallbackFineDustData();
            showNotification('대기질 정보를 불러오는데 실패했습니다. 기본 정보를 표시합니다.', 'error');
        }
    } catch (error) {
        console.error('미세먼지 데이터 로드 실패:', error);
        showFallbackFineDustData();
        showNotification('네트워크 오류로 대기질 정보를 불러올 수 없습니다.', 'error');
    }
}

// GPS 기반 대기질 데이터 로드
async function loadAirQualityDataByGPS(latitude, longitude) {
    try {
        console.log('GPS 기반 대기질 데이터 로드:', latitude, longitude);
        const response = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${latitude}&longitude=${longitude}`);
        const data = await response.json();

        if (data.success) {
            updateFineDustUI(data.data);
            showNotification('위치 정보가 업데이트되었습니다.', 'success');
        } else {
            throw new Error(data.message || 'GPS 데이터 로드 실패');
        }
    } catch (error) {
        console.error('GPS 대기질 데이터 로드 실패:', error);
        showNotification('위치 기반 대기질 정보를 불러오는데 실패했습니다.', 'error');
        loadFineDustData(); // 실패 시 기본 데이터 로드
    }
}

// 지역별 비교 데이터 로드
async function loadRegionalComparison() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/compare?sidoNames=서울,부산,대구,인천,광주,대전,울산`);
        const data = await response.json();

        if (data.success) {
            updateRegionalComparisonUI(data.data);
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
        document.getElementById('fine-dust-location').textContent = airQuality.sidoName;
    }
    if (airQuality.stationName) {
        document.getElementById('station-name').textContent = airQuality.stationName;
    }

    // 측정 시간
    if (airQuality.dataTime) {
        const date = new Date(airQuality.dataTime);
        const timeString = date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
        document.getElementById('measurement-time').textContent = timeString;
        document.getElementById('fine-dust-update-time').textContent = `업데이트: ${timeString}`;
    }

    // 전체 등급
    if (airQuality.overallStatus && airQuality.overallGrade) {
        const badge = document.getElementById('fine-dust-overall-badge');
        badge.textContent = airQuality.overallStatus;
        badge.className = 'aqi-badge large-badge ' + getAqiClass(airQuality.overallGrade);
    }

    // 건강 조언
    if (airQuality.healthAdvice) {
        document.getElementById('fine-dust-health-advice').textContent = airQuality.healthAdvice;
        document.getElementById('advice-description').textContent = airQuality.healthAdvice;
    }

    // 통합대기환경지수 (KHAI)
    if (airQuality.khai) {
        document.getElementById('khai-value').textContent = airQuality.khai.value || '--';
        document.getElementById('khai-status').textContent = airQuality.khai.status || '--';
        document.getElementById('khai-grade').textContent = `등급: ${airQuality.khai.grade || '--'}`;
        document.getElementById('khai-status').className = 'aqi-detail-status ' + getAqiClass(airQuality.khai.grade);
    }

    // PM10
    if (airQuality.pm10) {
        document.getElementById('pm10-detail-value').textContent = `${airQuality.pm10.value || '--'} ${airQuality.pm10.unit || '㎍/㎥'}`;
        document.getElementById('pm10-detail-status').textContent = airQuality.pm10.status || '--';
        document.getElementById('pm10-detail-grade').textContent = `등급: ${airQuality.pm10.grade || '--'}`;
        document.getElementById('pm10-detail-status').className = 'aqi-detail-status ' + getAqiClass(airQuality.pm10.grade);
    }

    // PM2.5
    if (airQuality.pm25) {
        document.getElementById('pm25-detail-value').textContent = `${airQuality.pm25.value || '--'} ${airQuality.pm25.unit || '㎍/㎥'}`;
        document.getElementById('pm25-detail-status').textContent = airQuality.pm25.status || '--';
        document.getElementById('pm25-detail-grade').textContent = `등급: ${airQuality.pm25.grade || '--'}`;
        document.getElementById('pm25-detail-status').className = 'aqi-detail-status ' + getAqiClass(airQuality.pm25.grade);
    }

    // 오존
    if (airQuality.o3) {
        document.getElementById('o3-detail-value').textContent = `${airQuality.o3.value || '--'} ${airQuality.o3.unit || 'ppm'}`;
        document.getElementById('o3-detail-status').textContent = airQuality.o3.status || '--';
        document.getElementById('o3-detail-grade').textContent = `등급: ${airQuality.o3.grade || '--'}`;
        document.getElementById('o3-detail-status').className = 'aqi-detail-status ' + getAqiClass(airQuality.o3.grade);
    }

    // 이산화질소
    if (airQuality.no2) {
        document.getElementById('no2-value').textContent = `${airQuality.no2.value || '--'} ${airQuality.no2.unit || 'ppm'}`;
        document.getElementById('no2-status').textContent = airQuality.no2.status || '--';
        document.getElementById('no2-grade').textContent = `등급: ${airQuality.no2.grade || '--'}`;
        document.getElementById('no2-status').className = 'aqi-detail-status ' + getAqiClass(airQuality.no2.grade);
    }

    // 일산화탄소
    if (airQuality.co) {
        document.getElementById('co-value').textContent = `${airQuality.co.value || '--'} ${airQuality.co.unit || 'ppm'}`;
        document.getElementById('co-status').textContent = airQuality.co.status || '--';
        document.getElementById('co-grade').textContent = `등급: ${airQuality.co.grade || '--'}`;
        document.getElementById('co-status').className = 'aqi-detail-status ' + getAqiClass(airQuality.co.grade);
    }

    // 건강 조언 제목
    if (airQuality.overallStatus) {
        document.getElementById('advice-title').textContent = `현재 대기질: ${airQuality.overallStatus}`;
    }

    // 예보 정보
    if (airQuality.forecasts && Array.isArray(airQuality.forecasts)) {
        updateFineDustForecast(airQuality.forecasts);
    } else {
        // 예보 데이터가 없으면 기본 예보 표시
        updateFineDustForecast([
            { date: '오늘', overallGrade: airQuality.overallGrade || '2', pm10Grade: airQuality.pm10?.grade || '2', pm25Grade: airQuality.pm25?.grade || '2', advice: airQuality.healthAdvice || '대기질 정보를 확인해주세요.' }
        ]);
    }
}

// 지역별 비교 UI 업데이트
function updateRegionalComparisonUI(regionalData) {
    const container = document.getElementById('regional-aqi-list');
    if (!container) return;

    container.innerHTML = '';

    regionalData.forEach(region => {
        if (region.sidoName && region.overallGrade) {
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

    // 오늘 예보
    if (todayContainer && forecasts.length > 0) {
        const today = forecasts[0];
        todayContainer.innerHTML = `
            <div class="aqi-forecast-day">
                <div class="forecast-date">${today.date || '오늘'}</div>
                <div class="forecast-overall ${getAqiClass(today.overallGrade)}">
                    ${getAqiStatusText(today.overallGrade)}
                </div>
                <div class="forecast-details">
                    <p><i class="fas fa-smog"></i> PM10: ${getAqiStatusText(today.pm10Grade)}</p>
                    <p><i class="fas fa-wind"></i> PM2.5: ${getAqiStatusText(today.pm25Grade)}</p>
                    <p class="forecast-advice"><i class="fas fa-lightbulb"></i> ${today.advice || '조언 정보 없음'}</p>
                </div>
            </div>
        `;
    }

    // 내일 예보
    if (tomorrowContainer && forecasts.length > 1) {
        const tomorrow = forecasts[1];
        tomorrowContainer.innerHTML = `
            <div class="aqi-forecast-day">
                <div class="forecast-date">${tomorrow.date || '내일'}</div>
                <div class="forecast-overall ${getAqiClass(tomorrow.overallGrade)}">
                    ${getAqiStatusText(tomorrow.overallGrade)}
                </div>
                <div class="forecast-details">
                    <p><i class="fas fa-smog"></i> PM10: ${getAqiStatusText(tomorrow.pm10Grade)}</p>
                    <p><i class="fas fa-wind"></i> PM2.5: ${getAqiStatusText(tomorrow.pm25Grade)}</p>
                    <p class="forecast-advice"><i class="fas fa-lightbulb"></i> ${tomorrow.advice || '조언 정보 없음'}</p>
                </div>
            </div>
        `;
    }

    // 주간 예보 (첫 3일만)
    if (weeklyContainer) {
        weeklyContainer.innerHTML = '';
        forecasts.slice(0, 3).forEach(forecast => {
            const element = document.createElement('div');
            element.className = 'aqi-forecast-day';
            element.innerHTML = `
                <div class="forecast-date">${forecast.date || '--'}</div>
                <div class="forecast-overall ${getAqiClass(forecast.overallGrade)}">
                    ${getAqiStatusText(forecast.overallGrade)}
                </div>
                <div class="forecast-details">
                    <p><i class="fas fa-smog"></i> PM10: ${getAqiStatusText(forecast.pm10Grade)}</p>
                    <p><i class="fas fa-wind"></i> PM2.5: ${getAqiStatusText(forecast.pm25Grade)}</p>
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
        co: { value: 0.5, status: '좋음', grade: '1', unit: 'ppm' },
        forecasts: [
            {
                date: '오늘',
                overallGrade: '2',
                pm10Grade: '1',
                pm25Grade: '1',
                advice: '오늘은 대기질이 양호합니다. 실외 활동에 문제 없습니다.'
            },
            {
                date: '내일',
                overallGrade: '2',
                pm10Grade: '2',
                pm25Grade: '2',
                advice: '내일도 대기질이 보통으로 예상됩니다. 민감한 분들은 주의하세요.'
            }
        ]
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
}

// 공통 유틸리티 함수
function getAqiClass(grade) {
    switch(grade) {
        case '1': return 'aqi-good';
        case '2': return 'aqi-moderate';
        case '3': return 'aqi-bad';
        case '4': return 'aqi-bad';
        default: return 'aqi-moderate';
    }
}

function getAqiStatusText(grade) {
    switch(grade) {
        case '1': return '좋음';
        case '2': return '보통';
        case '3': return '나쁨';
        case '4': return '매우나쁨';
        default: return '보통';
    }
}

// API 테스트 함수 (디버깅용)
async function testApiConnection() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await response.json();
        console.log('API 연결 테스트 결과:', {
            status: response.status,
            success: data.success,
            data: data.data ? '데이터 있음' : '데이터 없음'
        });
        return data;
    } catch (error) {
        console.error('API 연결 테스트 실패:', error);
        return null;
    }
}

// 데이터 새로고침 버튼 이벤트 추가 (옵션)
function addRefreshButton() {
    const refreshBtn = document.createElement('button');
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
        background: var(--primary-color);
        color: white;
        border: none;
        box-shadow: var(--shadow-heavy);
        cursor: pointer;
        font-size: 1.5rem;
        transition: all 0.3s ease;
    `;

    refreshBtn.addEventListener('click', function() {
        this.style.transform = 'rotate(360deg)';
        setTimeout(() => {
            this.style.transform = 'rotate(0deg)';
        }, 500);

        loadFineDustData();
        loadRegionalComparison();
        showNotification('데이터를 새로고침합니다...', 'info');
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