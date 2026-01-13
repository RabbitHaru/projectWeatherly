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
    loadAllData();

    // 5분마다 데이터 새로고침
    setInterval(loadAllData, 300000);
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
    document.getElementById('current-time').textContent =
        now.toLocaleDateString('ko-KR', options);
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
    const gpsBtn = document.getElementById('gps-sync-btn');
    if (gpsBtn) {
        gpsBtn.addEventListener('click', function() {
            const originalHTML = this.innerHTML;
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 동기화 중...';
            this.disabled = true;

            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
                    function(position) {
                        const lat = position.coords.latitude;
                        const lng = position.coords.longitude;

                        // 위치 동기화 API 호출
                        syncLocationWithServer(lat, lng);

                        // 버튼 상태 복원
                        gpsBtn.innerHTML = '<i class="fas fa-check-circle"></i> 동기화 완료';
                        gpsBtn.classList.add('sync-success');

                        setTimeout(() => {
                            gpsBtn.innerHTML = originalHTML;
                            gpsBtn.disabled = false;
                            gpsBtn.classList.remove('sync-success');
                            // 새 위치로 데이터 다시 로드
                            loadWeatherDataByGPS(lat, lng);
                            loadAirQualityDataByGPS(lat, lng);
                        }, 2000);
                    },
                    function(error) {
                        console.error('GPS 오류:', error);
                        gpsBtn.innerHTML = '<i class="fas fa-exclamation-circle"></i> 실패';
                        gpsBtn.classList.add('sync-error');
                        setTimeout(() => {
                            gpsBtn.innerHTML = originalHTML;
                            gpsBtn.disabled = false;
                            gpsBtn.classList.remove('sync-error');
                        }, 2000);
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
            }
        });
    }
}

// 서버에 위치 동기화 요청
async function syncLocationWithServer(latitude, longitude) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/sync-location?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST'
        });
        const data = await response.json();
        if (data.success) {
            console.log('위치 동기화 성공:', data.data);
        }
    } catch (error) {
        console.error('위치 동기화 실패:', error);
    }
}

// 모든 데이터 로드
async function loadAllData() {
    try {
        await Promise.all([
            loadWeatherData(),
            loadAirQualityData(),
            loadRegionalWeatherData(),
            loadCommunityData()
        ]);
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    }
}

// 날씨 데이터 로드
async function loadWeatherData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await response.json();

        if (data.success) {
            updateWeatherUI(data.data);
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
            method: 'POST'
        });
        const data = await response.json();

        if (data.success) {
            updateWeatherUI(data.data);
        }
    } catch (error) {
        console.error('GPS 날씨 데이터 로드 실패:', error);
    }
}

// 대기질 데이터 로드
async function loadAirQualityData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await response.json();

        if (data.success) {
            updateAirQualityUI(data.data);
        }
    } catch (error) {
        console.error('대기질 데이터 로드 실패:', error);
        showFallbackAirQualityData();
    }
}

// GPS로 대기질 데이터 로드
async function loadAirQualityDataByGPS(latitude, longitude) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST'
        });
        const data = await response.json();

        if (data.success) {
            updateAirQualityUI(data.data);
        }
    } catch (error) {
        console.error('GPS 대기질 데이터 로드 실패:', error);
    }
}

// 지역별 날씨 데이터 로드
async function loadRegionalWeatherData() {
    try {
        const regions = ['부산', '대구', '인천', '광주'];
        const regionalContainer = document.getElementById('regional-weather');
        regionalContainer.innerHTML = '';

        for (const region of regions) {
            const response = await fetch(`${API_BASE_URL}/api/weather/region/${getRegionCode(region)}`);
            const data = await response.json();

            if (data.success) {
                const weather = data.data;
                const element = createRegionalWeatherElement(region, weather);
                regionalContainer.appendChild(element);
            }
        }
    } catch (error) {
        console.error('지역별 날씨 데이터 로드 실패:', error);
    }
}

// 커뮤니티 데이터 로드
async function loadCommunityData() {
    try {
        // 더미 커뮤니티 데이터
        const posts = [
            { category: 'weather-talk', time: '10분 전', title: '오늘 날씨 정말 좋네요! 산책하기 딱 좋아요', author: '날씨매니아', likes: 24 },
            { category: 'outfit', time: '30분 전', title: '이번 주말에 캠핑 갈 건데 옷차림 조언 부탁드려요', author: '캠핑러버', likes: 18 },
            { category: 'dust', time: '1시간 전', title: '미세먼지 심한 날 실내 운동 추천합니다', author: '건강관리', likes: 32 },
            { category: 'weather-talk', time: '2시간 전', title: '내일 비온다는데 우산 꼭 챙기세요!', author: '우산챙기자', likes: 45 },
            { category: 'outfit', time: '3시간 전', title: '가을철 아침저녁 쌀쌀할 때 패딩 입어야 할까요?', author: '패션왕', likes: 21 },
            { category: 'weather-talk', time: '5시간 전', title: '단풍 구경 가기 좋은 날씨 언제까지일까요?', author: '자연사랑', likes: 38 }
        ];

        const container = document.getElementById('community-posts');
        container.innerHTML = '';

        posts.forEach(post => {
            const postElement = createCommunityPostElement(post);
            container.appendChild(postElement);
        });
    } catch (error) {
        console.error('커뮤니티 데이터 로드 실패:', error);
    }
}

// 날씨 UI 업데이트
function updateWeatherUI(weather) {
    if (!weather) return;

    // 기본 정보
    if (weather.regionName) {
        document.getElementById('current-location').textContent = weather.regionName;
    }
    if (weather.currentTime) {
        document.getElementById('current-time').textContent = weather.currentTime;
    }

    // 현재 날씨
    if (weather.current) {
        const current = weather.current;
        document.getElementById('current-temp').innerHTML = `${current.temperature || '--'}<span class="temp-unit">°C</span>`;
        document.getElementById('weather-condition').textContent = current.weatherCondition || '--';
        document.getElementById('feels-like').textContent = `${current.feelsLike || '--'}°C`;
        document.getElementById('wind-speed').textContent = `${current.windSpeed || '--'} m/s`;
        document.getElementById('humidity').textContent = `${current.humidity || '--'}%`;
        document.getElementById('precipitation').textContent = `${current.precipitation || '--'} mm`;
    }

    // 시간별 예보
    if (weather.hourly && Array.isArray(weather.hourly)) {
        updateHourlyForecast(weather.hourly);
    }

    // 일별 예보
    if (weather.daily && Array.isArray(weather.daily)) {
        updateDailyForecast(weather.daily);
    }

    // 요약 정보
    if (weather.summary) {
        const summary = weather.summary;
        document.getElementById('ultra-short-summary').textContent = summary.ultraShortSummary || '--';
        document.getElementById('short-term-summary').textContent = summary.shortSummary || '--';
        document.getElementById('mid-term-summary').textContent = summary.midSummary || '--';
    }
}

// 대기질 UI 업데이트
function updateAirQualityUI(airQuality) {
    if (!airQuality) return;

    // 전체 등급
    if (airQuality.overallStatus) {
        const badge = document.getElementById('aqi-overall');
        badge.textContent = airQuality.overallStatus;
        badge.className = 'aqi-badge ' + getAqiClass(airQuality.overallGrade || '2');
    }

    // PM10
    if (airQuality.pm10) {
        document.getElementById('pm10-value').textContent = `${airQuality.pm10.value || '--'} ㎍/㎥`;
        document.getElementById('pm10-status').textContent = airQuality.pm10.status || '--';
        document.getElementById('pm10-status').className = 'aqi-status ' + getAqiClass(airQuality.pm10.grade || '2');
    }

    // PM2.5
    if (airQuality.pm25) {
        document.getElementById('pm25-value').textContent = `${airQuality.pm25.value || '--'} ㎍/㎥`;
        document.getElementById('pm25-status').textContent = airQuality.pm25.status || '--';
        document.getElementById('pm25-status').className = 'aqi-status ' + getAqiClass(airQuality.pm25.grade || '2');
    }

    // 오존
    if (airQuality.o3) {
        document.getElementById('o3-value').textContent = `${airQuality.o3.value || '--'} ppm`;
        document.getElementById('o3-status').textContent = airQuality.o3.status || '--';
        document.getElementById('o3-status').className = 'aqi-status ' + getAqiClass(airQuality.o3.grade || '2');
    }

    // 예보 정보
    if (airQuality.forecasts && Array.isArray(airQuality.forecasts)) {
        updateAirQualityForecast(airQuality.forecasts);
    }
}

// 시간별 예보 업데이트
function updateHourlyForecast(hourlyData) {
    const container = document.getElementById('hourly-forecast');
    container.innerHTML = '';

    hourlyData.slice(0, 8).forEach(hour => {
        const hourElement = document.createElement('div');
        hourElement.className = 'hour-item';
        hourElement.innerHTML = `
                <div class="hour-time">${hour.time || '--'}</div>
                <div class="hour-icon"><i class="${hour.weatherIcon || 'fas fa-question'}"></i></div>
                <div class="hour-temp">${hour.temperature || '--'}°</div>
            `;
        container.appendChild(hourElement);
    });
}

// 일별 예보 업데이트
function updateDailyForecast(dailyData) {
    // 주간 예보 업데이트
    const weeklyContainer = document.getElementById('weekly-forecast');
    weeklyContainer.innerHTML = '';

    dailyData.slice(0, 6).forEach(day => {
        const dayElement = document.createElement('div');
        dayElement.className = 'day-item';
        dayElement.innerHTML = `
                <div class="day-name">${day.dayOfWeek || '--'}</div>
                <div class="day-icon"><i class="${day.dayIcon || 'fas fa-question'}"></i></div>
                <div class="day-temp-range">
                    <span class="temp-max">${day.maxTemp || '--'}°</span>
                    <span class="temp-min">${day.minTemp || '--'}°</span>
                </div>
                <div class="day-aqi aqi-good">좋음</div>
            `;
        weeklyContainer.appendChild(dayElement);
    });

    // 내일 예보 업데이트
    if (dailyData.length > 1) {
        const tomorrow = dailyData[1];
        const tomorrowContainer = document.getElementById('tomorrow-forecast');
        tomorrowContainer.innerHTML = `
                <div class="day-item">
                    <div class="day-name">오전</div>
                    <div class="day-icon"><i class="${tomorrow.dayIcon || 'fas fa-sun'}"></i></div>
                    <div class="day-temp-range">
                        <span class="temp-max">${tomorrow.maxTemp || '--'}°</span>
                        <span class="temp-min">${tomorrow.minTemp || '--'}°</span>
                    </div>
                    <div class="day-aqi aqi-good">좋음</div>
                </div>
                <div class="day-item">
                    <div class="day-name">오후</div>
                    <div class="day-icon"><i class="${tomorrow.dayIcon || 'fas fa-cloud-sun'}"></i></div>
                    <div class="day-temp-range">
                        <span class="temp-max">${tomorrow.maxTemp || '--'}°</span>
                        <span class="temp-min">${tomorrow.minTemp || '--'}°</span>
                    </div>
                    <div class="day-aqi aqi-moderate">보통</div>
                </div>
            `;
    }
}

// 대기질 예보 업데이트
function updateAirQualityForecast(forecasts) {
    const container = document.getElementById('aqi-forecast-details');
    container.innerHTML = '';

    forecasts.slice(0, 3).forEach(forecast => {
        const element = document.createElement('div');
        element.className = 'aqi-item';
        element.innerHTML = `
                <span class="aqi-label">${forecast.date || '--'}</span>
                <span class="aqi-value">${getAqiStatusText(forecast.overallGrade)}</span>
                <span class="aqi-status ${getAqiClass(forecast.overallGrade)}">${getAqiStatusText(forecast.overallGrade)}</span>
            `;
        container.appendChild(element);
    });

    // 전체 등급 표시
    if (forecasts.length > 0) {
        const badge = document.getElementById('aqi-forecast-overall');
        badge.textContent = getAqiStatusText(forecasts[0].overallGrade);
        badge.className = 'aqi-badge ' + getAqiClass(forecasts[0].overallGrade);
    }
}

// 지역별 날씨 요소 생성
function createRegionalWeatherElement(regionName, weather) {
    const element = document.createElement('div');
    element.className = 'region-weather';
    element.innerHTML = `
            <div class="region-info">
                <span class="region-name">${regionName}</span>
                <span class="region-weather-desc">${weather.current?.weatherCondition || '--'}</span>
            </div>
            <div class="region-temp">${weather.current?.temperature || '--'}°C</div>
        `;
    return element;
}

// 커뮤니티 게시물 요소 생성
function createCommunityPostElement(post) {
    const element = document.createElement('div');
    element.className = 'post-item';
    element.innerHTML = `
            <div class="post-header">
                <span class="post-category ${post.category}">${getCategoryText(post.category)}</span>
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
    document.getElementById('current-temp').innerHTML = '22<span class="temp-unit">°C</span>';
    document.getElementById('weather-condition').textContent = '맑음';
    document.getElementById('feels-like').textContent = '23°C';
    document.getElementById('wind-speed').textContent = '2.5 m/s';
    document.getElementById('humidity').textContent = '45%';
    document.getElementById('precipitation').textContent = '0 mm';
    document.getElementById('ultra-short-summary').textContent = '현재부터 6시간 후까지 맑은 날씨가 이어집니다.';
    document.getElementById('short-term-summary').textContent = '금요일 맑음 → 토요일 구름 조금 → 일요일 흐림';
    document.getElementById('mid-term-summary').textContent = '월요일 비 예상 후, 점차 개면서 기온 상승';
}

// 폴백 대기질 데이터 표시
function showFallbackAirQualityData() {
    document.getElementById('aqi-overall').textContent = '좋음';
    document.getElementById('aqi-overall').className = 'aqi-badge aqi-good';
    document.getElementById('pm10-value').textContent = '35 ㎍/㎥';
    document.getElementById('pm10-status').textContent = '좋음';
    document.getElementById('pm10-status').className = 'aqi-status aqi-good';
    document.getElementById('pm25-value').textContent = '15 ㎍/㎥';
    document.getElementById('pm25-status').textContent = '좋음';
    document.getElementById('pm25-status').className = 'aqi-status aqi-good';
    document.getElementById('o3-value').textContent = '0.025 ppm';
    document.getElementById('o3-status').textContent = '좋음';
    document.getElementById('o3-status').className = 'aqi-status aqi-good';
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