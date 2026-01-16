// main.js - 날씨 및 대기질 통합 (요약 정보 자동 생성 기능 포함)

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

    // 메인 페이지 요소가 있는지 확인 후 데이터 로드
    if (document.getElementById('current-temp')) {
        loadInitialData();
        // 5분마다 날씨 데이터 새로고침
        setInterval(loadWeatherData, 1000*60*5);
    }
});

// ==========================================
// [초기 설정 및 공통 기능]
// ==========================================

function setupDarkMode() {
    const darkModeToggle = document.getElementById('darkmode-toggle');
    const body = document.body;
    const isDarkMode = localStorage.getItem('darkMode') === 'true';

    if (isDarkMode) {
        body.classList.add('dark-mode');
        updateDarkModeIcon(true);
    }

    if (darkModeToggle) {
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
}

function updateDarkModeIcon(isDarkMode) {
    const icon = document.querySelector('#darkmode-toggle i');
    const button = document.getElementById('darkmode-toggle');
    if (icon && button) {
        if (isDarkMode) {
            icon.className = 'fas fa-sun';
            button.title = '라이트모드로 전환';
        } else {
            icon.className = 'fas fa-moon';
            button.title = '다크모드로 전환';
        }
    }
}

function updateCurrentTime() {
    const now = new Date();
    const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long', hour: '2-digit', minute: '2-digit', hour12: false };

    try {
        const formattedTime = now.toLocaleDateString('ko-KR', options)
            .replace('년', '년 ').replace('월', '월 ').replace('일', '일 ');

        const currentTimeElement = document.getElementById('current-time');
        const fineDustTimeElement = document.getElementById('fine-dust-current-time');

        if (currentTimeElement) currentTimeElement.textContent = formattedTime;
        if (fineDustTimeElement) {
            // 미세먼지 페이지용 포맷
            const fdOptions = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long', hour: '2-digit', minute: '2-digit' };
            fineDustTimeElement.textContent = now.toLocaleDateString('ko-KR', fdOptions);
        }
    } catch (error) {
        console.error('시간 업데이트 오류:', error);
    }
}

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

                // 탭 전환 시 데이터 리프레시 (필요시)
                if (document.getElementById('hourly-forecast')) {
                    if (tabId === 'hourly' || tabId === 'tomorrow' || tabId === 'weekly') {
                        // 이미 데이터가 있으면 굳이 다시 로드하지 않음 (캐싱 효과)
                    } else if (tabId === 'air-quality') {
                        loadAirQualityData();
                    }
                }
            }
        });
    });
}

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
                            await syncLocationWithServer(lat, lng);
                            await loadWeatherDataByGPS(lat, lng);
                            gpsBtn.innerHTML = '<i class="fas fa-check-circle"></i> 완료';
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
                    (error) => {
                        alert('위치 정보를 가져올 수 없습니다.');
                        gpsBtn.innerHTML = originalHTML;
                        gpsBtn.disabled = false;
                    },
                    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
                );
            } else {
                alert('GPS 미지원 브라우저입니다.');
                gpsBtn.innerHTML = originalHTML;
                gpsBtn.disabled = false;
            }
        });
    }
}

async function syncLocationWithServer(latitude, longitude) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/sync-location?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }
        });
        const data = await response.json();
        if (data.success) return data.data;
        throw new Error(data.message);
    } catch (error) { throw error; }
}

// ==========================================
// [데이터 로드 로직]
// ==========================================

async function loadInitialData() {
    if (!document.getElementById('current-temp')) return;
    try {
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

async function loadWeatherData() {
    if (!document.getElementById('current-temp')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/current`);
        const data = await response.json();
        if (data.success) updateWeatherUI(data.data);
        else throw new Error(data.message);
    } catch (error) {
        console.error('날씨 로드 실패:', error);
        showFallbackWeatherData();
    }
}

async function loadWeatherDataByGPS(latitude, longitude) {
    try {
        showLoadingState('위치 기반 날씨 정보를 불러오는 중...');
        const response = await fetch(`${API_BASE_URL}/api/weather/gps?latitude=${latitude}&longitude=${longitude}`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }
        });
        const data = await response.json();
        if (data.success) {
            updateWeatherUI(data.data);
            await loadAirQualityDataByGPS(latitude, longitude);
        } else throw new Error(data.message);
    } catch (error) {
        console.error('GPS 날씨 로드 실패:', error);
        showErrorMessage('위치 기반 정보를 불러올 수 없습니다.');
        await loadWeatherData(); // 실패 시 기본 로드
    } finally {
        hideLoadingState();
    }
}

// ==========================================
// [UI 업데이트 로직 - 날씨 & 요약]
// ==========================================

function updateWeatherUI(weather) {
    if (!weather) return;

    // 안전하게 텍스트/HTML 넣기 헬퍼
    const setTxt = (id, txt) => { const el = document.getElementById(id); if(el) el.textContent = txt; };
    const setHTML = (id, html) => { const el = document.getElementById(id); if(el) el.innerHTML = html; };

    // 1. 현재 위치 및 날씨
    if (weather.regionName) setTxt('current-location', weather.regionName);

    if (weather.current) {
        const cur = weather.current;
        setHTML('current-temp', `${Math.round(cur.temperature)}<span class="temp-unit">°C</span>`);
        setTxt('weather-condition', cur.weatherCondition || '--');
        setTxt('feels-like', `${Math.round(cur.feelsLike)}°C`);
        setTxt('wind-speed', `${cur.windSpeed?.toFixed(1) || '--'} m/s`);
        setTxt('humidity', `${Math.round(cur.humidity) || '--'}%`);
        setTxt('precipitation', `${cur.precipitation || '0'} mm`);

        if (cur.updateTime) {
            const t = new Date(cur.updateTime);
            const timeStr = t.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
            const timeEl = document.getElementById('current-time');
            if (timeEl) {
                const baseText = timeEl.textContent.split('(')[0];
                timeEl.textContent = `${baseText} (${timeStr} 업데이트)`;
            }
        }
    }

    // 2. 예보 데이터 (가로 스크롤 카드)
    if (weather.hourly) updateTodayHourlyForecast(weather.hourly);
    if (weather.tomorrowHourly) updateTomorrowHourlyForecast(weather.tomorrowHourly);
    if (weather.daily) updateWeeklyForecast(weather.daily);

    // 3. [핵심] 초단기/단기/중기 요약 정보 생성 및 표시
    updateForecastSummaries(weather);
}

// ★★★ 요약 정보 자동 생성 및 연결 함수 ★★★
function updateForecastSummaries(weather) {
    const setTxt = (id, txt) => { const el = document.getElementById(id); if(el) el.textContent = txt; };

    // 1. 초단기 예보 (현재 ~ 6시간)
    if (weather.current && weather.hourly && weather.hourly.length > 0) {
        const next6Hours = weather.hourly.slice(0, 6);
        const nextCondition = next6Hours.length > 0 ? next6Hours[0].weatherCondition : weather.current.weatherCondition;
        const tempTrend = next6Hours[5].temperature > weather.current.temperature ? '오를' : '떨어질';

        // 요약 멘트 생성
        const ultraSummary = `앞으로 6시간 동안 ${nextCondition} 날씨가 이어지며, 기온은 다소 ${tempTrend} 전망입니다.`;

        setTxt('ultra-short-summary', ultraSummary);
        setTxt('ultra-short-temp', `${Math.round(weather.current.temperature)}°C`); // 현재 기온
        setTxt('ultra-short-humidity', `${Math.round(weather.current.humidity)}%`); // 현재 습도
    }

    // 2. 단기 예보 (내일 ~ 모레)
    if (weather.daily && weather.daily.length > 1) {
        const tomorrow = weather.daily[1]; // Index 1 = 내일
        const condition = tomorrow.dayWeather || tomorrow.nightWeather || '맑음';

        const shortSummary = `내일은 대체로 ${condition} 것으로 예상됩니다. 외출 시 날씨를 확인하세요.`;

        setTxt('short-term-summary', shortSummary);
        setTxt('short-term-max-temp', `${Math.round(tomorrow.maxTemp)}°C`);
        setTxt('short-term-min-temp', `${Math.round(tomorrow.minTemp)}°C`);
    }

    // 3. 중기 예보 (3일 후 ~ 10일 후)
    if (weather.daily && weather.daily.length > 3) {
        const midTermData = weather.daily.slice(3, 10); // 3일 후부터
        let rainyDay = null;

        // 비 오는 날 찾기
        for (let day of midTermData) {
            if (day.precipitationProbability >= 50) {
                rainyDay = day.dayOfWeek; // 예: "수"
                break;
            }
        }

        let midSummary = "당분간 맑거나 구름 많은 날씨가 이어지겠습니다.";
        let precipText = "0%";

        if (rainyDay) {
            midSummary = `다가오는 ${rainyDay}요일에 비 소식이 있습니다. 우산을 미리 준비하세요.`;
            precipText = "강수확률 높음";
        }

        // 기온 변화 추세 (마지막 날과 3일 후 비교)
        const startTemp = midTermData[0].maxTemp;
        const endTemp = midTermData[midTermData.length - 1].maxTemp;
        let trend = "비슷함";
        if (endTemp > startTemp + 3) trend = "상승세";
        else if (endTemp < startTemp - 3) trend = "하강세";

        setTxt('mid-term-summary', midSummary);
        setTxt('weekly-precipitation', precipText);
        setTxt('temp-trend', trend);
    }
}

// ==========================================
// [대기질 관련 로직]
// ==========================================

async function loadAirQualityData() {
    if (!document.getElementById('aqi-overall')) return;
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/current`);
        const data = await response.json();
        if (data.success && data.data) {
            updateAirQualityUI(data.data);
            const sido = data.data.sidoName || '서울';
            loadAirQualityForecast(sido);
        } else {
            showFallbackAirQualityData();
        }
    } catch (e) {
        console.error('대기질 로드 실패:', e);
        showFallbackAirQualityData();
    }
}

async function loadAirQualityDataByGPS(lat, lng) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/gps?latitude=${lat}&longitude=${lng}`, {
            method: 'POST', headers: {'Content-Type': 'application/json'}
        });
        const data = await response.json();
        if (data.success && data.data) {
            updateAirQualityUI(data.data);
            const sido = data.data.sidoName || '서울';
            loadAirQualityForecast(sido);
        }
    } catch (e) { console.error('GPS 대기질 로드 실패', e); }
}

async function loadAirQualityForecast(sidoName) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/air-quality/forecast/${encodeURIComponent(sidoName)}`);
        const data = await response.json();
        if (data.success) updateAirQualityForecast(data.data);
    } catch (e) { console.error('대기질 예보 실패', e); }
}

function updateAirQualityUI(aqi) {
    if (!aqi) return;
    const setTxt = (id, txt) => { const el = document.getElementById(id); if(el) el.textContent = txt; };

    if (aqi.overallStatus) {
        const badge = document.getElementById('aqi-overall');
        if (badge) {
            badge.textContent = aqi.overallStatus;
            badge.className = 'aqi-badge ' + getAqiClass(aqi.overallGrade || '2');
        }
    }

    // 상세 지수 업데이트
    const updateItem = (prefix, item) => {
        if (!item) return;
        setTxt(`${prefix}-value`, `${item.value || '--'} ${item.unit || ''}`);
        const statusEl = document.getElementById(`${prefix}-status`);
        if (statusEl) {
            statusEl.textContent = item.status || '--';
            statusEl.className = 'aqi-status ' + getAqiClass(item.grade || '2');
        }
    };

    updateItem('pm10', aqi.pm10);
    updateItem('pm25', aqi.pm25);
    updateItem('o3', aqi.o3);
}

// 미세먼지 예보 업데이트 (간소화 버전)
function updateAirQualityForecast(forecasts) {
    const container = document.getElementById('aqi-forecast-details');
    if (!container) return;
    container.innerHTML = '';

    if (!forecasts || !Array.isArray(forecasts) || forecasts.length === 0) {
        container.innerHTML = '<div class="no-data">미세먼지 예보 데이터가 없습니다.</div>';
        return;
    }

    forecasts.forEach(forecast => {
        const forecastElement = document.createElement('div');
        forecastElement.className = 'aqi-forecast-item';
        const gradeClass = getAqiClass(forecast.overallGrade || '2');
        const statusText = getAqiStatusText(forecast.overallGrade || '2');
        const gradeNum = forecast.overallGrade || '2';

        forecastElement.innerHTML = `
            <div class="aqi-forecast-header">
                <h5>${forecast.date || '--'}</h5>
                <div class="aqi-forecast-region">전국 대기질</div>
            </div>
            <div class="aqi-forecast-icon" style="font-size: 2.5rem; margin: 10px 0; color: var(--primary-color);">
                ${getAqiIcon(gradeNum)}
            </div>
            <div class="aqi-forecast-value" style="font-size: 1.5rem; font-weight: 800; margin-bottom: 10px;">
                ${statusText}
            </div>
            <div class="aqi-forecast-status ${gradeClass}" style="width: auto; display: inline-block; padding: 5px 15px;">
                통합지수 ${gradeNum}등급
            </div>
        `;
        container.appendChild(forecastElement);
    });
    setupScrollHints();
}

// ==========================================
// [예보 카드 UI 렌더링 함수들]
// ==========================================

function updateTodayHourlyForecast(hourlyData) {
    const container = document.getElementById('hourly-forecast');
    if (!container) return;
    container.innerHTML = '';

    const displayData = hourlyData.slice(0, 24);
    displayData.forEach(forecast => {
        const div = document.createElement('div');
        div.className = 'hour-item';
        let pop = '';
        if (forecast.precipitationProbability > 0) pop = `<div class="hour-pop">${Math.round(forecast.precipitationProbability)}%</div>`;

        div.innerHTML = `
            <div class="hour-time">${forecast.time}</div>
            <div class="hour-icon"><i class="${forecast.weatherIcon || 'fas fa-sun'}"></i></div>
            <div class="hour-temp">${Math.round(forecast.temperature)}°</div>
            ${pop}
            <div class="hour-humidity">${Math.round(forecast.humidity)}%</div>
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

function updateTomorrowHourlyForecast(hourlyData) {
    const container = document.getElementById('tomorrow-forecast');
    if (!container) return;
    container.innerHTML = '';

    // 2시간 간격, 12개 아이템
    const majorHours = [0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22];
    const filtered = hourlyData.filter(d => majorHours.includes(parseHourFromDisplay(d.time))).slice(0, 12);

    filtered.forEach(forecast => {
        const div = document.createElement('div');
        div.className = 'tomorrow-hour-item';
        let pop = '';
        if (forecast.precipitationProbability > 0) pop = `<div class="tomorrow-pop">${Math.round(forecast.precipitationProbability)}%</div>`;

        div.innerHTML = `
            <div class="tomorrow-time">${forecast.time}</div>
            <div class="tomorrow-icon"><i class="${forecast.weatherIcon || 'fas fa-sun'}"></i></div>
            <div class="tomorrow-temp">${Math.round(forecast.temperature)}°</div>
            ${pop}
            <div class="tomorrow-wind">${forecast.windSpeed?.toFixed(1) || '-'}m/s</div>
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

function updateWeeklyForecast(dailyData) {
    const container = document.getElementById('weekly-forecast');
    if (!container) return;
    container.innerHTML = '';

    dailyData.slice(0, 7).forEach(day => {
        const div = document.createElement('div');
        div.className = 'day-item';
        let pop = '';
        if (day.precipitationProbability > 0) pop = `<div class="day-pop"><i class="fas fa-umbrella"></i> ${Math.round(day.precipitationProbability)}%</div>`;

        div.innerHTML = `
            <div class="day-header">
                <div class="day-name">${day.dayOfWeek}</div>
                <div class="day-date">${day.date}</div>
            </div>
            <div class="day-temps">
                <div class="temp-am">
                    <div class="temp-label">오전</div>
                    <div class="temp-value">${Math.round(day.amTemp || day.minTemp)}°</div>
                    <div class="temp-icon"><i class="${day.nightIcon || 'fas fa-moon'}"></i></div>
                    <div class="temp-condition">${day.nightWeather || '-'}</div>
                </div>
                <div class="temp-pm">
                    <div class="temp-label">오후</div>
                    <div class="temp-value">${Math.round(day.pmTemp || day.maxTemp)}°</div>
                    <div class="temp-icon"><i class="${day.dayIcon || 'fas fa-sun'}"></i></div>
                    <div class="temp-condition">${day.dayWeather || '-'}</div>
                </div>
            </div>
            ${pop}
        `;
        container.appendChild(div);
    });
    setupScrollHints();
}

// ==========================================
// [기타 기능: 지역날씨, 커뮤니티, 유틸리티]
// ==========================================

// 지역별 날씨 데이터 로드 (최적화됨: 한 번의 요청으로 처리)
async function loadRegionalWeatherData() {
    const container = document.getElementById('regional-weather');
    if (!container) return;

    container.innerHTML = '<div style="text-align:center; padding:20px; color:#888;"><i class="fas fa-spinner fa-spin"></i> 로딩중...</div>';

    // [수정됨] 서울 추가 (맨 위)
    const regions = [
        {name: '서울', code: '1100000000'},
        {name: '부산', code: '2600000000'},
        {name: '대구', code: '2700000000'},
        {name: '인천', code: '2800000000'},
        {name: '광주', code: '2900000000'},
        {name: '대전', code: '3000000000'},
        {name: '울산', code: '3100000000'}
    ];

    const regionCodes = regions.map(r => r.code).join(',');

    try {
        const response = await fetch(`${API_BASE_URL}/api/weather/compare?regionCodes=${regionCodes}`);
        const result = await response.json();

        if (result.success && result.data) {
            container.innerHTML = '';

            regions.forEach(targetRegion => {
                const weatherData = result.data.find(d => d.regionCode === targetRegion.code);

                if (weatherData) {
                    const div = document.createElement('div');
                    div.className = 'region-weather';
                    div.style.cursor = 'pointer';
                    div.onclick = () => alert(targetRegion.name + ' 상세 날씨 페이지로 이동 (구현 예정)');

                    const cur = weatherData.current || {};
                    const temp = cur.temperature !== undefined ? Math.round(cur.temperature) : '--';
                    const condition = cur.weatherCondition || '--';

                    div.innerHTML = `
                        <div class="region-info">
                            <span class="region-name">${targetRegion.name}</span>
                            <span class="region-weather-desc">${condition}</span>
                        </div>
                        <div class="region-temp">${temp}°C</div>
                    `;
                    container.appendChild(div);
                }
            });
        } else {
            container.innerHTML = '<div class="no-data">지역 정보를 불러올 수 없습니다.</div>';
        }
    } catch (e) {
        console.error('지역별 날씨 로드 실패', e);
        container.innerHTML = '<div class="no-data">연결 오류</div>';
    }
}

async function loadCommunityData() {
    const container = document.getElementById('community-posts');
    if (!container) return;
    // 더미 데이터
    const posts = [
        { category: 'weather-talk', time: '10분 전', title: '오늘 날씨 정말 좋네요! 산책하기 딱 좋아요', author: '날씨매니아', likes: 24 },
        { category: 'outfit', time: '30분 전', title: '이번 주말에 캠핑 갈 건데 옷차림 조언 부탁드려요', author: '캠핑러버', likes: 18 },
        { category: 'dust', time: '1시간 전', title: '미세먼지 심한 날 실내 운동 추천합니다', author: '건강관리', likes: 32 },
        { category: 'weather-talk', time: '2시간 전', title: '내일 비온다는데 우산 꼭 챙기세요!', author: '우산챙기자', likes: 45 }
    ];
    container.innerHTML = '';
    posts.forEach(p => {
        const div = document.createElement('div');
        div.className = 'post-item';
        const catText = getCategoryText(p.category);
        div.innerHTML = `
            <div class="post-header">
                <span class="post-category ${p.category}">${catText}</span>
                <span class="post-time">${p.time}</span>
            </div>
            <h4 class="post-title">${p.title}</h4>
            <div class="post-meta">
                <span class="post-author"><i class="fas fa-user"></i> ${p.author}</span>
                <span class="post-likes"><i class="fas fa-heart"></i> ${p.likes}</span>
            </div>
        `;
        container.appendChild(div);
    });
}

// 유틸리티 함수들
function showLoadingState(msg='로딩중...') {
    let el = document.getElementById('loading-overlay');
    if (!el) {
        el = document.createElement('div');
        el.id = 'loading-overlay';
        el.innerHTML = `<div class="loading-spinner"><i class="fas fa-spinner fa-spin fa-3x"></i><p>${msg}</p></div>`;
        el.style.cssText = `position: fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:flex; justify-content:center; align-items:center; z-index:9999; color:white; text-align:center;`;
        document.body.appendChild(el);
    }
}
function hideLoadingState() { const el = document.getElementById('loading-overlay'); if(el) el.remove(); }
function showErrorMessage(msg) {
    const el = document.createElement('div');
    el.className = 'error-message';
    el.textContent = msg;
    el.style.cssText = `position:fixed; top:20px; right:20px; background:#e74c3c; color:white; padding:15px; border-radius:5px; z-index:10000; box-shadow:0 4px 12px rgba(0,0,0,0.2);`;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 5000);
}

function getAqiClass(g) {
    switch(String(g).trim()) {
        case '1': return 'aqi-good'; case '2': return 'aqi-moderate';
        case '3': return 'aqi-bad'; case '4': return 'aqi-very-bad';
        default: return 'aqi-moderate';
    }
}
function getAqiStatusText(g) {
    switch(String(g).trim()) {
        case '1': return '좋음'; case '2': return '보통';
        case '3': return '나쁨'; case '4': return '매우나쁨';
        default: return '보통';
    }
}
function getAqiIcon(g) {
    switch(String(g)) {
        case '1': return '<i class="fas fa-smile" style="color:#2ecc71"></i>';
        case '2': return '<i class="fas fa-meh" style="color:#f39c12"></i>';
        case '3': return '<i class="fas fa-frown" style="color:#e74c3c"></i>';
        case '4': return '<i class="fas fa-dizzy" style="color:#8e44ad"></i>';
        default: return '<i class="fas fa-meh"></i>';
    }
}
function getCategoryText(c) {
    if(c==='weather-talk') return '날씨톡';
    if(c==='outfit') return '옷차림';
    if(c==='dust') return '미세먼지';
    return '일반';
}
function parseHourFromDisplay(t) {
    if(!t) return 0;
    if(t.includes('오전')) { const m = t.match(/(\d+)/); return m ? parseInt(m[1]) : 0; }
    if(t.includes('오후')) { const m = t.match(/(\d+)/); const h = m ? parseInt(m[1]) : 0; return h===12 ? 12 : h+12; }
    if(t==='자정') return 0;
    if(t==='정오') return 12;
    return 0;
}

function setupScrollHints() {
    const containers = document.querySelectorAll('.horizontal-scroll-container');
    containers.forEach(c => {
        const check = () => {
            if(c.scrollWidth > c.clientWidth) {
                c.classList.add('has-scroll');
                if(c.scrollLeft > 10) c.classList.add('scrolling');
                else c.classList.remove('scrolling');
            } else c.classList.remove('has-scroll', 'scrolling');
        };
        check();
        c.addEventListener('scroll', check);
        window.addEventListener('resize', check);
    });
}

function showFallbackWeatherData() {
    if(document.getElementById('current-temp')) document.getElementById('current-temp').innerHTML = '--';
}
function showFallbackAirQualityData() {
    if(document.getElementById('aqi-overall')) document.getElementById('aqi-overall').textContent = '--';
}

// 지도 더미 함수
function zoomInMap() { alert('지도 확대'); }
function zoomOutMap() { alert('지도 축소'); }
function refreshMap() { alert('지도 새로고침'); }
window.addEventListener('load', function() {
    const el = document.getElementById('weather-map');
    if(el) el.innerHTML = `<div class="map-placeholder" style="text-align:center; padding:20px; color:white;"><i class="fas fa-map-marked-alt fa-3x"></i><p>지도 로딩중...</p></div>`;
});