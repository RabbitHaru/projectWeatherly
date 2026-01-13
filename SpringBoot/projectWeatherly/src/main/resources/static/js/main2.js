// API 기본 URL
const API_BASE_URL = 'http://localhost:8080';

// DOMContentLoaded 이벤트
document.addEventListener('DOMContentLoaded', function() {
    console.log('Weatherly 애플리케이션 로딩 완료');

    // 데이터 로드
    loadWeatherData();
    loadAirQualityData();

    // 이벤트 리스너 설정
    setupEventListeners();
});

// 이벤트 리스너 설정
function setupEventListeners() {
    // GPS 동기화 버튼
    const gpsBtn = document.getElementById('gps-sync-btn');
    if (gpsBtn) {
        gpsBtn.addEventListener('click', function() {
            getCurrentLocation();
        });
    }

    // 다크모드 토글
    const darkModeToggle = document.getElementById('darkmode-toggle');
    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function() {
            toggleDarkMode();
        });
    }

    // 탭 전환
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            switchTab(this.dataset.tab);
        });
    });
}

// 날씨 데이터 로드
async function loadWeatherData() {
    try {
        console.log('날씨 데이터 로드 시작...');
        const response = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await response.json();

        if (data.success) {
            console.log('날씨 데이터 로드 성공:', data.data);
            updateWeatherUI(data.data);
        } else {
            console.error('날씨 데이터 로드 실패:', data.message);
            showFallbackWeatherData();
        }
    } catch (error) {
        console.error('날씨 데이터 로드 중 오류:', error);
        showFallbackWeatherData();
    }
}

// 대기질 데이터 로드
async function loadAirQualityData() {
    try {
        console.log('대기질 데이터 로드 시작...');
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await response.json();

        if (data.success) {
            console.log('대기질 데이터 로드 성공:', data.data);
            updateAirQualityUI(data.data);
        } else {
            console.error('대기질 데이터 로드 실패:', data.message);
            showFallbackAirQualityData();
        }
    } catch (error) {
        console.error('대기질 데이터 로드 중 오류:', error);
        showFallbackAirQualityData();
    }
}

// 현재 위치 가져오기
function getCurrentLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;
                console.log('현재 위치:', lat, lng);

                // GPS 데이터로 날씨/대기질 정보 업데이트
                updateWeatherByGPS(lat, lng);
                updateAirQualityByGPS(lat, lng);
            },
            function(error) {
                console.error('위치 정보 가져오기 실패:', error);
                alert('위치 정보를 가져올 수 없습니다. 브라우저 설정을 확인해주세요.');
            }
        );
    } else {
        alert('이 브라우저는 위치 서비스를 지원하지 않습니다.');
    }
}

// 날씨 UI 업데이트
function updateWeatherUI(weatherData) {
    if (!weatherData) return;

    // 위치 정보
    if (weatherData.regionName) {
        document.getElementById('current-location').textContent = weatherData.regionName;
    }

    // 현재 날씨
    if (weatherData.current) {
        const current = weatherData.current;
        document.getElementById('current-temp').textContent = `${Math.round(current.temperature)}°C`;
        document.getElementById('weather-condition').textContent = current.weatherCondition || '정보 없음';
        document.getElementById('feels-like').textContent = `${Math.round(current.feelsLike)}°C`;
        document.getElementById('wind-speed').textContent = `${current.windSpeed} m/s`;
        document.getElementById('humidity').textContent = `${current.humidity}%`;
        document.getElementById('precipitation').textContent = `${current.precipitation || 0} mm`;
    }
}

// 대기질 UI 업데이트
function updateAirQualityUI(airQualityData) {
    if (!airQualityData) return;

    // 전체 등급
    const overallStatus = airQualityData.overallStatus || '보통';
    const overallGrade = airQualityData.overallGrade || '2';

    document.getElementById('aqi-overall').textContent = overallStatus;
    document.getElementById('aqi-overall').className = `aqi-badge ${getAQIClass(overallGrade)}`;

    // 세부 항목
    updateAQIItem('pm10', airQualityData.pm10);
    updateAQIItem('pm25', airQualityData.pm25);
    updateAQIItem('o3', airQualityData.o3);
}

// AQI 항목 업데이트
function updateAQIItem(type, data) {
    if (!data) return;

    const valueElement = document.getElementById(`${type}-value`);
    const statusElement = document.getElementById(`${type}-status`);

    if (valueElement) {
        valueElement.textContent = `${data.value || '--'} ${data.unit || ''}`;
    }

    if (statusElement) {
        statusElement.textContent = data.status || '--';
        statusElement.className = `aqi-status ${getAQIClass(data.grade)}`;
    }
}

// AQI 등급에 따른 클래스 반환
function getAQIClass(grade) {
    switch(grade) {
        case '1': return 'aqi-good';
        case '2': return 'aqi-moderate';
        case '3': return 'aqi-bad';
        case '4': return 'aqi-bad';
        default: return 'aqi-moderate';
    }
}

// 다크모드 토글
function toggleDarkMode() {
    const body = document.body;
    const isDarkMode = body.classList.contains('dark-mode');

    if (isDarkMode) {
        body.classList.remove('dark-mode');
        localStorage.setItem('darkMode', 'false');
    } else {
        body.classList.add('dark-mode');
        localStorage.setItem('darkMode', 'true');
    }

    updateDarkModeIcon(!isDarkMode);
}

// 다크모드 아이콘 업데이트
function updateDarkModeIcon(isDarkMode) {
    const icon = document.querySelector('#darkmode-toggle i');
    if (isDarkMode) {
        icon.className = 'fas fa-sun';
    } else {
        icon.className = 'fas fa-moon';
    }
}

// 탭 전환
function switchTab(tabName) {
    // 탭 버튼 활성화
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // 탭 콘텐츠 표시
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    // 선택된 탭 활성화
    const selectedBtn = document.querySelector(`[data-tab="${tabName}"]`);
    const selectedContent = document.getElementById(`tab-${tabName}`);

    if (selectedBtn) selectedBtn.classList.add('active');
    if (selectedContent) selectedContent.classList.add('active');

    // 탭별 데이터 로드
    switch(tabName) {
        case 'hourly':
            loadHourlyForecast();
            break;
        case 'weekly':
            loadWeeklyForecast();
            break;
        case 'air-quality':
            loadAirQualityForecast();
            break;
    }
}

// 폴백 날씨 데이터
function showFallbackWeatherData() {
    document.getElementById('current-temp').textContent = '22°C';
    document.getElementById('weather-condition').textContent = '맑음';
    document.getElementById('feels-like').textContent = '23°C';
    document.getElementById('wind-speed').textContent = '2.5 m/s';
    document.getElementById('humidity').textContent = '45%';
    document.getElementById('precipitation').textContent = '0 mm';
}

// 폴백 대기질 데이터
function showFallbackAirQualityData() {
    document.getElementById('aqi-overall').textContent = '좋음';
    document.getElementById('aqi-overall').className = 'aqi-badge aqi-good';
    document.getElementById('pm10-value').textContent = '25 ㎍/㎥';
    document.getElementById('pm10-status').textContent = '좋음';
    document.getElementById('pm25-value').textContent = '12 ㎍/㎥';
    document.getElementById('pm25-status').textContent = '좋음';
    document.getElementById('o3-value').textContent = '0.02 ppm';
    document.getElementById('o3-status').textContent = '좋음';
}