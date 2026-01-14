// API 기본 URL
const API_BASE_URL = window.location.origin;

// DOMContentLoaded 이벤트
document.addEventListener('DOMContentLoaded', function() {
    // 다크모드 설정
    setupDarkMode();

    // 현재 시간 업데이트
    updateCurrentTime();
    setInterval(updateCurrentTime, 60000);

    // 탭 전환 기능
    setupTabSwitching();

    // GPS 동기화 기능
    setupGpsSync();

    // 데이터 로드
    loadInitialData();

    // 5분마다 날씨 데이터 새로고침
    setInterval(loadWeatherData, 300000);
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
        button.title = '라이트모드로 전환';
    } else {
        icon.className = 'fas fa-moon';
        button.title = '다크모드로 전환';
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
        minute: '2-digit',
        hour12: false
    };
    const formattedTime = now.toLocaleDateString('ko-KR', options)
        .replace('년', '년 ')
        .replace('월', '월 ')
        .replace('일', '일 ');
    document.getElementById('current-time').textContent = formattedTime;
}

// 탭 전환 설정
function setupTabSwitching() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');

            // 모든 탭 비활성화
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            // 현재 탭 활성화
            this.classList.add('active');
            const targetTab = document.getElementById(`tab-${tabId}`);
            if (targetTab) {
                targetTab.classList.add('active');

                // 탭 변경 시 데이터 새로고침
                if (tabId === 'hourly' || tabId === 'tomorrow' || tabId === 'weekly') {
                    loadWeatherData();
                } else if (tabId === 'air-quality') {
                    loadAirQualityData();
                }
            }
        });
    });
}

// GPS 동기화 설정
function setupGpsSync() {
    const gpsBtn = document.getElementById('gps-sync-btn');
    if (gpsBtn) {
        gpsBtn.addEventListener('click', function() {
            const originalHTML = this.innerHTML;
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 위치 확인 중...';
            this.disabled = true;

            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
                    async function(position) {
                        const lat = position.coords.latitude;
                        const lng = position.coords.longitude;

                        try {
                            // 위치 동기화 API 호출
                            await syncLocationWithServer(lat, lng);

                            // 새 위치로 날씨 데이터 로드
                            await loadWeatherDataByGPS(lat, lng);

                            // 성공 상태 표시
                            gpsBtn.innerHTML = '<i class="fas fa-check-circle"></i> 위치 업데이트 완료';
                            gpsBtn.classList.add('sync-success');

                            setTimeout(() => {
                                gpsBtn.innerHTML = originalHTML;
                                gpsBtn.disabled = false;
                                gpsBtn.classList.remove('sync-success');
                            }, 2000);

                        } catch (error) {
                            console.error('위치 동기화 실패:', error);
                            gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 실패';
                            gpsBtn.classList.add('sync-error');

                            setTimeout(() => {
                                gpsBtn.innerHTML = originalHTML;
                                gpsBtn.disabled = false;
                                gpsBtn.classList.remove('sync-error');
                            }, 2000);
                        }
                    },
                    function(error) {
                        console.error('GPS 오류:', error);
                        let errorMessage = '위치 정보를 가져올 수 없습니다.';

                        switch(error.code) {
                            case error.PERMISSION_DENIED:
                                errorMessage = '위치 접근 권한이 거부되었습니다.';
                                break;
                            case error.POSITION_UNAVAILABLE:
                                errorMessage = '위치 정보를 사용할 수 없습니다.';
                                break;
                            case error.TIMEOUT:
                                errorMessage = '위치 정보 요청 시간이 초과되었습니다.';
                                break;
                        }

                        alert(errorMessage);
                        gpsBtn.innerHTML = originalHTML;
                        gpsBtn.disabled = false;
                    },
                    {
                        enableHighAccuracy: true,
                        timeout: 10000,
                        maximumAge: 0
                    }
                );
            } else {
                alert('이 브라우저는 GPS를 지원하지 않습니다.');
                gpsBtn.innerHTML = originalHTML;
                gpsBtn.disabled = false;
            }
        });
    }
}

// 서버에 위치 동기화 요청
async function syncLocationWithServer(latitude, longitude) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/sync-location?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();
        if (data.success) {
            console.log('위치 동기화 성공:', data.data);
            return data.data;
        } else {
            throw new Error(data.message || '위치 동기화 실패');
        }
    } catch (error) {
        console.error('위치 동기화 실패:', error);
        throw error;
    }
}

// 초기 데이터 로드
async function loadInitialData() {
    try {
        // 로딩 상태 표시
        showLoadingState();

        await Promise.all([
            loadWeatherData(),
            loadAirQualityData(),
            loadRegionalWeatherData(),
            loadCommunityData()
        ]);

        hideLoadingState();

    } catch (error) {
        console.error('초기 데이터 로드 실패:', error);
        hideLoadingState();
        showErrorMessage('데이터를 불러오는데 실패했습니다.');
    }
}

// 로딩 상태 표시
function showLoadingState() {
    const loadingEl = document.createElement('div');
    loadingEl.id = 'loading-overlay';
    loadingEl.innerHTML = `
        <div class="loading-spinner">
            <i class="fas fa-spinner fa-spin fa-3x"></i>
            <p>날씨 정보를 불러오는 중...</p>
        </div>
    `;
    loadingEl.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 9999;
        color: white;
        text-align: center;
    `;
    document.body.appendChild(loadingEl);
}

// 로딩 상태 숨기기
function hideLoadingState() {
    const loadingEl = document.getElementById('loading-overlay');
    if (loadingEl) {
        loadingEl.remove();
    }
}

// 에러 메시지 표시
function showErrorMessage(message) {
    const errorEl = document.createElement('div');
    errorEl.className = 'error-message';
    errorEl.textContent = message;
    errorEl.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #e74c3c;
        color: white;
        padding: 15px 20px;
        border-radius: 5px;
        z-index: 10000;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    document.body.appendChild(errorEl);

    setTimeout(() => {
        errorEl.remove();
    }, 5000);
}

// 날씨 데이터 로드
async function loadWeatherData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await response.json();

        if (data.success) {
            updateWeatherUI(data.data);
        } else {
            throw new Error(data.message || '날씨 데이터를 불러올 수 없습니다.');
        }
    } catch (error) {
        console.error('날씨 데이터 로드 실패:', error);
        showFallbackWeatherData();
    }
}

// GPS로 날씨 데이터 로드
async function loadWeatherDataByGPS(latitude, longitude) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();

        if (data.success) {
            updateWeatherUI(data.data);
        } else {
            throw new Error(data.message || 'GPS 기반 날씨 데이터를 불러올 수 없습니다.');
        }
    } catch (error) {
        console.error('GPS 날씨 데이터 로드 실패:', error);
        showErrorMessage('위치 기반 날씨 정보를 불러올 수 없습니다.');
    }
}

// 대기질 데이터 로드
async function loadAirQualityData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await response.json();

        if (data.success) {
            updateAirQualityUI(data.data);
        } else {
            console.warn('대기질 API 실패:', data.message);
            showFallbackAirQualityData();
        }
    } catch (error) {
        console.error('대기질 데이터 로드 실패:', error);
        showFallbackAirQualityData();
    }
}

// 지역별 날씨 데이터 로드
async function loadRegionalWeatherData() {
    try {
        const majorRegions = [
            { name: '부산', code: '2600000000' },
            { name: '대구', code: '2700000000' },
            { name: '인천', code: '2800000000' },
            { name: '광주', code: '2900000000' },
            { name: '대전', code: '3000000000' },
            { name: '울산', code: '3100000000' }
        ];

        const regionalContainer = document.getElementById('regional-weather');
        if (!regionalContainer) return;

        regionalContainer.innerHTML = '';

        for (const region of majorRegions) {
            try {
                const response = await fetch(`${API_BASE_URL}/api/weather/region/${region.code}`);
                const data = await response.json();

                if (data.success) {
                    const element = createRegionalWeatherElement(region.name, data.data);
                    regionalContainer.appendChild(element);
                }
            } catch (error) {
                console.error(`지역 ${region.name} 데이터 로드 실패:`, error);
            }
        }
    } catch (error) {
        console.error('지역별 날씨 데이터 로드 실패:', error);
    }
}

// 커뮤니티 데이터 로드
async function loadCommunityData() {
    try {
        // 임시 더미 데이터
        const posts = [
            { category: 'weather-talk', time: '10분 전', title: '오늘 날씨 정말 좋네요! 산책하기 딱 좋아요', author: '날씨매니아', likes: 24 },
            { category: 'outfit', time: '30분 전', title: '이번 주말에 캠핑 갈 건데 옷차림 조언 부탁드려요', author: '캠핑러버', likes: 18 },
            { category: 'dust', time: '1시간 전', title: '미세먼지 심한 날 실내 운동 추천합니다', author: '건강관리', likes: 32 },
            { category: 'weather-talk', time: '2시간 전', title: '내일 비온다는데 우산 꼭 챙기세요!', author: '우산챙기자', likes: 45 },
            { category: 'outfit', time: '3시간 전', title: '가을철 아침저녁 쌀쌀할 때 패딩 입어야 할까요?', author: '패션왕', likes: 21 }
        ];

        const container = document.getElementById('community-posts');
        if (!container) return;

        container.innerHTML = '';

        posts.forEach(post => {
            const postElement = createCommunityPostElement(post);
            container.appendChild(postElement);
        });
    } catch (error) {
        console.error('커뮤니티 데이터 로드 실패:', error);
    }
}

// 날씨 UI 업데이트 (가로 스크롤 카드 형식으로 수정)
function updateWeatherUI(weather) {
    if (!weather) {
        console.error('날씨 데이터 없음');
        return;
    }

    console.log('날씨 데이터 업데이트:', weather);

    // 위치 정보 업데이트
    if (weather.regionName) {
        document.getElementById('current-location').textContent = weather.regionName;
    }

    // 현재 날씨 업데이트
    if (weather.current) {
        const current = weather.current;
        document.getElementById('current-temp').innerHTML = `${Math.round(current.temperature)}<span class="temp-unit">°C</span>`;
        document.getElementById('weather-condition').textContent = current.weatherCondition || '--';
        document.getElementById('feels-like').textContent = `${Math.round(current.feelsLike)}°C`;
        document.getElementById('wind-speed').textContent = `${current.windSpeed?.toFixed(1) || '--'} m/s`;
        document.getElementById('humidity').textContent = `${Math.round(current.humidity) || '--'}%`;
        document.getElementById('precipitation').textContent = `${current.precipitation || '0'} mm`;

        // 업데이트 시간 표시
        if (current.updateTime) {
            const updateTime = new Date(current.updateTime).toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit'
            });
            const timeElement = document.getElementById('current-time');
            if (timeElement) {
                const baseText = timeElement.textContent.split('(')[0];
                timeElement.textContent = `${baseText} (${updateTime} 업데이트)`;
            }
        }
    }

    // 오늘 시간별 예보 업데이트 (가로 스크롤)
    if (weather.hourly && Array.isArray(weather.hourly)) {
        updateTodayHourlyForecast(weather.hourly);
    }

    // 내일 시간별 예보 업데이트 (가로 스크롤)
    if (weather.tomorrowHourly && Array.isArray(weather.tomorrowHourly)) {
        updateTomorrowHourlyForecast(weather.tomorrowHourly);
    }

    // 주간 예보 업데이트 (가로 스크롤)
    if (weather.daily && Array.isArray(weather.daily)) {
        updateWeeklyForecast(weather.daily);
    }

    // 요약 정보 업데이트
    if (weather.summary) {
        const summary = weather.summary;
        document.getElementById('ultra-short-summary').textContent = summary.ultraShortSummary || '--';
        document.getElementById('short-term-summary').textContent = summary.shortSummary || '--';
        document.getElementById('mid-term-summary').textContent = summary.midSummary || '--';
    }
}

// 대기질 UI 업데이트
function updateAirQualityUI(airQuality) {
    if (!airQuality) {
        showFallbackAirQualityData();
        return;
    }

    // 전체 등급
    if (airQuality.overallStatus) {
        const badge = document.getElementById('aqi-overall');
        if (badge) {
            badge.textContent = airQuality.overallStatus;
            badge.className = 'aqi-badge ' + getAqiClass(airQuality.overallGrade || '2');
        }
    }

    // PM10 (미세먼지)
    if (airQuality.pm10) {
        const value = airQuality.pm10.value || '--';
        const status = airQuality.pm10.status || '--';
        const grade = airQuality.pm10.grade || '2';

        document.getElementById('pm10-value').textContent = `${value} ${airQuality.pm10.unit || '㎍/㎥'}`;
        document.getElementById('pm10-status').textContent = status;
        document.getElementById('pm10-status').className = 'aqi-status ' + getAqiClass(grade);
    }

    // PM2.5 (초미세먼지)
    if (airQuality.pm25) {
        const value = airQuality.pm25.value || '--';
        const status = airQuality.pm25.status || '--';
        const grade = airQuality.pm25.grade || '2';

        document.getElementById('pm25-value').textContent = `${value} ${airQuality.pm25.unit || '㎍/㎥'}`;
        document.getElementById('pm25-status').textContent = status;
        document.getElementById('pm25-status').className = 'aqi-status ' + getAqiClass(grade);
    }

    // 오존
    if (airQuality.o3) {
        const value = airQuality.o3.value || '--';
        const status = airQuality.o3.status || '--';
        const grade = airQuality.o3.grade || '2';

        document.getElementById('o3-value').textContent = `${value} ${airQuality.o3.unit || 'ppm'}`;
        document.getElementById('o3-status').textContent = status;
        document.getElementById('o3-status').className = 'aqi-status ' + getAqiClass(grade);
    }

    // 미세먼지 예보 데이터도 함께 업데이트
    updateAirQualityForecast(airQuality);
}

// 오늘 시간별 예보 업데이트 (가로 스크롤)
function updateTodayHourlyForecast(hourlyData) {
    const container = document.getElementById('hourly-forecast');
    if (!container) return;

    container.innerHTML = '';

    if (!hourlyData || hourlyData.length === 0) {
        container.innerHTML = '<div class="no-data">오늘 시간별 예보 데이터가 없습니다.</div>';
        return;
    }

    // 최대 24시간 표시
    const displayData = hourlyData.slice(0, Math.min(hourlyData.length, 24));

    displayData.forEach(forecast => {
        const hourElement = document.createElement('div');
        hourElement.className = 'hour-item';

        let popHtml = '';
        if (forecast.precipitationProbability > 0) {
            popHtml = `<div class="hour-pop">${Math.round(forecast.precipitationProbability)}%</div>`;
        }

        hourElement.innerHTML = `
            <div class="hour-time">${forecast.time || '--'}</div>
            <div class="hour-icon"><i class="${forecast.weatherIcon || 'fas fa-question'}"></i></div>
            <div class="hour-temp">${Math.round(forecast.temperature)}°</div>
            ${popHtml}
            <div class="hour-humidity">${Math.round(forecast.humidity)}%</div>
        `;
        container.appendChild(hourElement);
    });

    // 스크롤 가능 여부 표시
    showScrollHint(container.parentElement);
}

// 스크롤 힌트 표시 함수 추가
function showScrollHint(container) {
    if (container.scrollWidth > container.clientWidth) {
        container.classList.add('has-scroll');
    } else {
        container.classList.remove('has-scroll');
    }
}

// 내일 시간별 예보 업데이트 (가로 스크롤)
function updateTomorrowHourlyForecast(hourlyData) {
    const container = document.getElementById('tomorrow-forecast');
    if (!container) return;

    container.innerHTML = '';

    if (!hourlyData || hourlyData.length === 0) {
        container.innerHTML = '<div class="no-data">내일 예보 데이터가 없습니다.</div>';
        return;
    }

    // 2시간 간격으로 주요 시간대 표시
    const majorHours = [0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22];
    const filteredData = hourlyData.filter((forecast, index) => {
        const hour = parseHourFromDisplay(forecast.time);
        return majorHours.includes(hour % 24);
    }).slice(0, 12);

    filteredData.forEach(forecast => {
        const hourElement = document.createElement('div');
        hourElement.className = 'tomorrow-hour-item';

        let popHtml = '';
        if (forecast.precipitationProbability > 0) {
            popHtml = `<div class="tomorrow-pop">${Math.round(forecast.precipitationProbability)}%</div>`;
        }

        hourElement.innerHTML = `
            <div class="tomorrow-time">${forecast.time || '--'}</div>
            <div class="tomorrow-icon"><i class="${forecast.weatherIcon || 'fas fa-question'}"></i></div>
            <div class="tomorrow-temp">${Math.round(forecast.temperature)}°</div>
            ${popHtml}
            <div class="tomorrow-wind">${forecast.windSpeed?.toFixed(1) || '--'}m/s</div>
        `;
        container.appendChild(hourElement);
    });
}

// 주간 예보 업데이트 (가로 스크롤)
function updateWeeklyForecast(dailyData) {
    const container = document.getElementById('weekly-forecast');
    if (!container) return;

    container.innerHTML = '';

    if (!dailyData || dailyData.length === 0) {
        container.innerHTML = '<div class="no-data">주간 예보 데이터가 없습니다.</div>';
        return;
    }

    // 7일간 표시
    dailyData.slice(0, 7).forEach(day => {
        const dayElement = document.createElement('div');
        dayElement.className = 'day-item';

        let popHtml = '';
        if (day.precipitationProbability > 0) {
            popHtml = `<div class="day-pop"><i class="fas fa-umbrella"></i> ${Math.round(day.precipitationProbability)}%</div>`;
        }

        dayElement.innerHTML = `
            <div class="day-header">
                <div class="day-name">${day.dayOfWeek || '--'}</div>
                <div class="day-date">${day.date || '--'}</div>
            </div>
            <div class="day-temps">
                <div class="temp-am">
                    <div class="temp-label">오전</div>
                    <div class="temp-value">${Math.round(day.amTemp || day.minTemp)}°</div>
                    <div class="temp-icon"><i class="${day.nightIcon || 'fas fa-question'}"></i></div>
                    <div class="temp-condition">${day.nightWeather || '--'}</div>
                </div>
                <div class="temp-pm">
                    <div class="temp-label">오후</div>
                    <div class="temp-value">${Math.round(day.pmTemp || day.maxTemp)}°</div>
                    <div class="temp-icon"><i class="${day.dayIcon || 'fas fa-question'}"></i></div>
                    <div class="temp-condition">${day.dayWeather || '--'}</div>
                </div>
            </div>
            ${popHtml}
        `;
        container.appendChild(dayElement);
    });
}

// 미세먼지 예보 업데이트 (가로 스크롤)
function updateAirQualityForecast(airQualityData) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;

    container.innerHTML = '';

    // 더미 데이터 생성 (실제 API 연동 시 수정 필요)
    const regions = [
        { name: '서울', value: 35, status: '좋음', grade: '1' },
        { name: '인천', value: 45, status: '보통', grade: '2' },
        { name: '경기', value: 52, status: '나쁨', grade: '3' },
        { name: '강원', value: 28, status: '좋음', grade: '1' },
        { name: '충북', value: 40, status: '보통', grade: '2' },
        { name: '충남', value: 48, status: '보통', grade: '2' },
        { name: '전북', value: 38, status: '보통', grade: '2' },
        { name: '전남', value: 42, status: '보통', grade: '2' },
        { name: '경북', value: 55, status: '나쁨', grade: '3' },
        { name: '경남', value: 50, status: '보통', grade: '2' },
        { name: '제주', value: 25, status: '좋음', grade: '1' }
    ];

    regions.forEach(region => {
        const aqiElement = document.createElement('div');
        aqiElement.className = 'aqi-forecast-item';

        const gradeClass = getAqiClass(region.grade);

        aqiElement.innerHTML = `
            <div class="aqi-forecast-header">
                <h5>${region.name}</h5>
                <div class="aqi-forecast-region">PM10</div>
            </div>
            <div class="aqi-forecast-value">${region.value}</div>
            <div class="aqi-forecast-status ${gradeClass}">${region.status}</div>
        `;
        container.appendChild(aqiElement);
    });
}

// 지역별 날씨 요소 생성
function createRegionalWeatherElement(regionName, weather) {
    const element = document.createElement('div');
    element.className = 'region-weather';

    const currentTemp = weather.current?.temperature ? Math.round(weather.current.temperature) : '--';
    const condition = weather.current?.weatherCondition || '--';

    element.innerHTML = `
        <div class="region-info">
            <span class="region-name">${regionName}</span>
            <span class="region-weather-desc">${condition}</span>
        </div>
        <div class="region-temp">${currentTemp}°C</div>
    `;

    // 클릭 이벤트 추가
    element.style.cursor = 'pointer';
    element.addEventListener('click', () => {
        window.location.href = `/weather?region=${regionName}`;
    });

    return element;
}

// 커뮤니티 게시물 요소 생성
function createCommunityPostElement(post) {
    const element = document.createElement('div');
    element.className = 'post-item';

    const categoryClass = post.category || 'general';
    const categoryText = getCategoryText(post.category);

    element.innerHTML = `
        <div class="post-header">
            <span class="post-category ${categoryClass}">${categoryText}</span>
            <span class="post-time">${post.time}</span>
        </div>
        <h4 class="post-title">${post.title}</h4>
        <div class="post-meta">
            <span class="post-author"><i class="fas fa-user"></i> ${post.author}</span>
            <span class="post-likes"><i class="fas fa-heart"></i> ${post.likes}</span>
        </div>
    `;

    return element;
}

// 폴백 날씨 데이터 표시
function showFallbackWeatherData() {
    console.log('폴백 날씨 데이터 표시');

    document.getElementById('current-temp').innerHTML = '22<span class="temp-unit">°C</span>';
    document.getElementById('weather-condition').textContent = '맑음';
    document.getElementById('feels-like').textContent = '23°C';
    document.getElementById('wind-speed').textContent = '2.5 m/s';
    document.getElementById('humidity').textContent = '45%';
    document.getElementById('precipitation').textContent = '0 mm';
    document.getElementById('ultra-short-summary').textContent = '현재부터 6시간 후까지 맑은 날씨가 이어집니다.';
    document.getElementById('short-term-summary').textContent = '금요일 맑음 → 토요일 구름 조금 → 일요일 흐림';
    document.getElementById('mid-term-summary').textContent = '월요일 비 예상 후, 점차 개면서 기온 상승';

    // 기본 시간별 데이터 표시 (가로 스크롤)
    updateTodayHourlyForecast(createDefaultHourlyData(0));
    updateTomorrowHourlyForecast(createDefaultHourlyData(1));
    updateWeeklyForecast(createDefaultWeeklyData());
    updateAirQualityForecast();
}

// 폴백 대기질 데이터 표시
function showFallbackAirQualityData() {
    const badge = document.getElementById('aqi-overall');
    if (badge) {
        badge.textContent = '좋음';
        badge.className = 'aqi-badge aqi-good';
    }

    const pm10Value = document.getElementById('pm10-value');
    const pm10Status = document.getElementById('pm10-status');
    if (pm10Value) pm10Value.textContent = '35 ㎍/㎥';
    if (pm10Status) {
        pm10Status.textContent = '좋음';
        pm10Status.className = 'aqi-status aqi-good';
    }

    const pm25Value = document.getElementById('pm25-value');
    const pm25Status = document.getElementById('pm25-status');
    if (pm25Value) pm25Value.textContent = '15 ㎍/㎥';
    if (pm25Status) {
        pm25Status.textContent = '좋음';
        pm25Status.className = 'aqi-status aqi-good';
    }

    const o3Value = document.getElementById('o3-value');
    const o3Status = document.getElementById('o3-status');
    if (o3Value) o3Value.textContent = '0.025 ppm';
    if (o3Status) {
        o3Status.textContent = '좋음';
        o3Status.className = 'aqi-status aqi-good';
    }

    // 미세먼지 예보도 기본 데이터 표시
    updateAirQualityForecast();
}

// 기본 시간별 데이터 생성
function createDefaultHourlyData(daysFromNow) {
    const hourlyData = [];
    const startHour = new Date().getHours();

    for (let i = 0; i < 24; i++) {
        const hour = (startHour + i) % 24;
        const temp = 20 + Math.sin((hour - 6) * Math.PI / 12) * 5;

        hourlyData.push({
            time: formatHourToDisplay(hour),
            temperature: temp,
            weatherCondition: hour < 6 || hour > 20 ? '맑음' : hour < 12 ? '구름조금' : '구름많음',
            weatherIcon: hour >= 6 && hour <= 18 ? 'fas fa-sun' : 'fas fa-moon',
            precipitationProbability: 0,
            humidity: 45 + Math.sin(hour * Math.PI / 12) * 10,
            windSpeed: 2 + Math.random() * 2
        });
    }

    return hourlyData;
}

// 기본 주간 데이터 생성
function createDefaultWeeklyData() {
    const weeklyData = [];
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    const now = new Date();

    for (let i = 0; i < 7; i++) {
        const date = new Date(now);
        date.setDate(now.getDate() + i);

        const month = date.getMonth() + 1;
        const day = date.getDate();
        const maxTemp = 24 - i + Math.random() * 2;
        const minTemp = 16 - i - Math.random() * 2;

        weeklyData.push({
            date: `${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}`,
            dayOfWeek: days[date.getDay()],
            maxTemp: maxTemp,
            minTemp: minTemp,
            amTemp: minTemp + 3,
            pmTemp: maxTemp - 2,
            dayWeather: i % 3 === 0 ? '구름많음' : '맑음',
            nightWeather: '맑음',
            dayIcon: 'fas fa-cloud-sun',
            nightIcon: 'fas fa-moon',
            precipitationProbability: i === 3 ? 60 : i === 4 ? 30 : 0
        });
    }

    return weeklyData;
}

// 시간 포맷팅
function formatHourToDisplay(hour) {
    if (hour === 0) return '자정';
    if (hour === 12) return '정오';
    if (hour < 12) return `오전 ${hour}시`;
    if (hour === 24) return '자정';
    return `오후 ${hour - 12}시`;
}

// 시간 문자열 파싱
function parseHourFromDisplay(timeDisplay) {
    if (!timeDisplay) return 0;

    if (timeDisplay.includes('오전')) {
        const match = timeDisplay.match(/(\d+)/);
        return match ? parseInt(match[1]) : 0;
    } else if (timeDisplay.includes('오후')) {
        const match = timeDisplay.match(/(\d+)/);
        const hour = match ? parseInt(match[1]) : 0;
        return hour === 12 ? 12 : hour + 12;
    } else if (timeDisplay.includes('시')) {
        const match = timeDisplay.match(/(\d+)/);
        return match ? parseInt(match[1]) : 0;
    } else if (timeDisplay === '자정') {
        return 0;
    } else if (timeDisplay === '정오') {
        return 12;
    }
    return 0;
}

// 도움말 함수들
function getRegionCode(regionName) {
    const codes = {
        '서울': '1100000000',
        '부산': '2600000000',
        '대구': '2700000000',
        '인천': '2800000000',
        '광주': '2900000000',
        '대전': '3000000000',
        '울산': '3100000000',
        '경기': '4100000000',
        '강원': '4200000000',
        '충북': '4300000000',
        '충남': '4400000000',
        '전북': '4500000000',
        '전남': '4600000000',
        '경북': '4700000000',
        '경남': '4800000000',
        '제주': '5000000000'
    };
    return codes[regionName] || '1100000000';
}

function getAqiClass(grade) {
    switch(grade) {
        case '1': return 'aqi-good';
        case '2': return 'aqi-moderate';
        case '3': return 'aqi-bad';
        case '4': return 'aqi-very-bad';
        default: return 'aqi-moderate';
    }
}

function getCategoryText(category) {
    switch(category) {
        case 'weather-talk': return '날씨톡';
        case 'outfit': return '옷차림';
        case 'dust': return '미세먼지';
        default: return '일반';
    }
}

// 지도 관련 함수들
function zoomInMap() {
    alert('지도 확대 기능 (실제 구현 시 지도 라이브러리와 연동)');
}

function zoomOutMap() {
    alert('지도 축소 기능 (실제 구현 시 지도 라이브러리와 연동)');
}

function refreshMap() {
    alert('지도 새로고침 (실제 구현 시 지도 라이브러리와 연동)');
}

// 날씨 지도 로드
function loadWeatherMap() {
    const mapElement = document.getElementById('weather-map');
    if (mapElement) {
        mapElement.innerHTML = `
            <div class="map-placeholder" style="text-align: center; padding: 20px;">
                <i class="fas fa-map-marked-alt fa-3x" style="color: var(--primary-color); margin-bottom: 10px;"></i>
                <p>실시간 날씨 지도</p>
                <p style="font-size: 0.9rem; color: var(--secondary-color); margin-top: 10px;">
                    지도 기능은 준비 중입니다.
                </p>
            </div>
        `;
    }
}

// 페이지 로드 시 지도 로드
window.addEventListener('load', loadWeatherMap);

// 내부 탭 전환 기능 추가
function setupInnerTabs() {
    // 시간대별 탭
    const periodTabs = document.querySelectorAll('.period-tab');
    periodTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const period = this.getAttribute('data-period');
            const container = this.closest('.forecast-card-with-tabs');

            // 모든 탭 비활성화
            container.querySelectorAll('.period-tab').forEach(t => t.classList.remove('active'));
            container.querySelectorAll('.period-content').forEach(c => c.classList.remove('active'));

            // 현재 탭 활성화
            this.classList.add('active');
            const targetContent = container.querySelector(`#period-${period}`);
            if (targetContent) {
                targetContent.classList.add('active');
            }
        });
    });

    // 요일별 탭
    const dayTabs = document.querySelectorAll('.day-tab');
    dayTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const day = this.getAttribute('data-day');
            const container = this.closest('.forecast-card-with-tabs');

            // 모든 탭 비활성화
            container.querySelectorAll('.day-tab').forEach(t => t.classList.remove('active'));
            container.querySelectorAll('.day-content').forEach(c => c.classList.remove('active'));

            // 현재 탭 활성화
            this.classList.add('active');
            const targetContent = container.querySelector(`#day-${day}`);
            if (targetContent) {
                targetContent.classList.add('active');
            }
        });
    });

    // 미세먼지 시간대 탭
    const aqiTabs = document.querySelectorAll('.aqi-tab');
    aqiTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const period = this.getAttribute('data-period');
            const container = this.closest('.forecast-card-with-tabs');

            // 모든 탭 비활성화
            container.querySelectorAll('.aqi-tab').forEach(t => t.classList.remove('active'));
            container.querySelectorAll('.aqi-period-content').forEach(c => c.classList.remove('active'));

            // 현재 탭 활성화
            this.classList.add('active');
            const targetContent = container.querySelector(`#aqi-${period}`);
            if (targetContent) {
                targetContent.classList.add('active');
            }
        });
    });
}

// DOMContentLoaded에 추가
document.addEventListener('DOMContentLoaded', function() {
    // 기존 코드...
    setupInnerTabs();
    // 기존 코드...
});

// 시간별 예보 업데이트 함수 수정
function updateTodayHourlyForecast(hourlyData) {
    const containers = {
        morning: document.querySelector('#period-morning .compact-hourly-forecast'),
        afternoon: document.querySelector('#period-afternoon .compact-hourly-forecast'),
        evening: document.querySelector('#period-evening .compact-hourly-forecast'),
        dawn: document.querySelector('#period-dawn .compact-hourly-forecast')
    };

    // 각 컨테이너 초기화
    Object.values(containers).forEach(container => {
        if (container) container.innerHTML = '';
    });

    if (!hourlyData || hourlyData.length === 0) {
        Object.values(containers).forEach(container => {
            if (container) {
                container.innerHTML = '<div class="no-data">예보 데이터 없음</div>';
            }
        });
        return;
    }

    // 현재 시간 확인
    const now = new Date();
    const currentHour = now.getHours();

    // 시간대별로 데이터 분류
    hourlyData.forEach(forecast => {
        const hour = parseHourFromDisplay(forecast.time);
        let period = '';

        // 시간대 분류
        if (hour >= 6 && hour < 12) period = 'morning';
        else if (hour >= 12 && hour < 18) period = 'afternoon';
        else if (hour >= 18 && hour < 24) period = 'evening';
        else period = 'dawn';

        const container = containers[period];
        if (!container) return;

        const timeSlot = document.createElement('div');
        timeSlot.className = 'time-slot';

        // 현재 시간인 경우 강조 표시
        if (hour === currentHour) {
            timeSlot.classList.add('current');
        }

        // 아이콘 매핑 (간소화된 버전)
        const iconClass = getWeatherIconClass(forecast.weatherCondition, hour);

        let popHtml = '';
        if (forecast.precipitationProbability > 0) {
            popHtml = `<div class="precip"><i class="fas fa-tint"></i> ${Math.round(forecast.precipitationProbability)}%</div>`;
        }

        timeSlot.innerHTML = `
            <div class="time">${formatHourTo24(hour)}</div>
            <div class="weather-icon"><i class="${iconClass}"></i></div>
            <div class="temp">${Math.round(forecast.temperature)}°</div>
            ${popHtml}
        `;

        container.appendChild(timeSlot);
    });

    // 각 컨테이너에 데이터가 없는 경우 처리
    Object.entries(containers).forEach(([period, container]) => {
        if (container && container.children.length === 0) {
            container.innerHTML = '<div class="no-data">해당 시간대 데이터 없음</div>';
        }
    });
}

// 24시간제 포맷팅
function formatHourTo24(hour) {
    return `${hour.toString().padStart(2, '0')}:00`;
}

// 날씨 상태별 아이콘 클래스 반환 (간소화)
function getWeatherIconClass(condition, hour) {
    const isDaytime = hour >= 6 && hour <= 18;

    if (!condition) return isDaytime ? 'fas fa-sun' : 'fas fa-moon';

    const conditionLower = condition.toLowerCase();

    if (conditionLower.includes('맑음') || conditionLower.includes('clear')) {
        return isDaytime ? 'fas fa-sun' : 'fas fa-moon';
    } else if (conditionLower.includes('흐림') || conditionLower.includes('cloud')) {
        return 'fas fa-cloud';
    } else if (conditionLower.includes('비') || conditionLower.includes('rain')) {
        return 'fas fa-cloud-rain';
    } else if (conditionLower.includes('눈') || conditionLower.includes('snow')) {
        return 'fas fa-snowflake';
    } else if (conditionLower.includes('안개') || conditionLower.includes('fog')) {
        return 'fas fa-smog';
    } else if (conditionLower.includes('번개') || conditionLower.includes('thunder')) {
        return 'fas fa-bolt';
    }

    return isDaytime ? 'fas fa-sun' : 'fas fa-moon';
}

// 스크롤 힌트 표시 함수
function setupScrollHints() {
    const scrollContainers = document.querySelectorAll('.horizontal-scroll-container');

    scrollContainers.forEach(container => {
        const items = container.querySelector('.hourly-forecast-items, .tomorrow-forecast-items, .weekly-forecast-items, .aqi-forecast-items');

        // 스크롤 가능 여부 확인
        function checkScroll() {
            if (container.scrollWidth > container.clientWidth) {
                container.classList.add('has-scroll');

                // 스크롤 위치에 따라 힌트 표시
                if (container.scrollLeft > 10) {
                    container.classList.add('scrolling');
                } else {
                    container.classList.remove('scrolling');
                }

                // 오른쪽 끝에 도달했는지 확인
                const maxScroll = container.scrollWidth - container.clientWidth;
                if (container.scrollLeft < maxScroll - 10) {
                    container.classList.add('scrolling');
                }
            } else {
                container.classList.remove('has-scroll', 'scrolling');
            }
        }

        // 초기 확인
        checkScroll();

        // 스크롤 이벤트 리스너
        container.addEventListener('scroll', checkScroll);

        // 리사이즈 이벤트 리스너
        window.addEventListener('resize', checkScroll);

        // 데이터 로드 후 다시 확인
        setTimeout(checkScroll, 500);
    });
}

// DOMContentLoaded 이벤트에 추가
document.addEventListener('DOMContentLoaded', function() {
    // 기존 코드...

    // 스크롤 힌트 설정
    setupScrollHints();

    // 데이터 로드 후 스크롤 힌트 업데이트
    setTimeout(setupScrollHints, 1000);
});

// 날씨 데이터 로드 함수에서 스크롤 힌트 업데이트 호출
async function loadWeatherData() {
    try {
        // 기존 코드...

        // 데이터 업데이트 후 스크롤 힌트 재설정
        setTimeout(setupScrollHints, 100);
    } catch (error) {
        // 기존 코드...
    }
}