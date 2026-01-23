/**
 * insights.js
 * 기상 인사이트 대시보드 로직
 * - common.js의 RegionManager와 bindGpsButton을 활용하여 위치 동기화
 * - 차트 시각화 및 다크모드 대응
 */

let tempChartInstance = null;
let aqiChartInstance = null;
let envChartInstance = null;

document.addEventListener('DOMContentLoaded', function () {
    // ⭐ 1. 위치 데이터 동기화
    syncLocationWithCommonJs();

    // ⭐ 2. 지역명 보정 (강원도 -> 강원특별자치도)
    fixLocationTextInInsights();

    // 3. 차트 초기화 및 기타 기능
    initTemperatureChart();
    initAirQualityChart();
    initEnvChart();
    setupDarkModeObserver();

    // 3. 다크모드 감지
    setupDarkModeObserver();

    // ⭐ 4. GPS 버튼 연결 (common.js의 bindGpsButton 활용)
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lon) {
            // GPS 위치를 찾으면 -> 저장된 고정 위치는 지우고(GPS 모드), 해당 좌표로 이동
            if (typeof RegionManager !== 'undefined') RegionManager.clear();

            // 페이지 새로고침 (좌표 파라미터 추가)
            window.location.href = `/insights?lat=${lat}&lon=${lon}`;
        });
    } else {
        console.warn("common.js가 로드되지 않았습니다.");
    }
});

// ⭐ 화면 텍스트 강제 보정 함수 (Insights 전용)
function fixLocationTextInInsights() {
    const locEl = document.getElementById('display-region-name'); // 통계 페이지의 지역명 ID

    if (locEl && typeof getFullSidoName === 'function' && typeof extractSidoName === 'function') {
        const originalText = locEl.textContent.trim();
        const shortName = extractSidoName(originalText);
        const fullName = getFullSidoName(shortName);

        if (originalText !== fullName) {
            console.log(`🔧 Insights 지역명 보정: ${originalText} -> ${fullName}`);
            locEl.textContent = fullName;
        }
    }
}

// ⭐ [핵심] common.js와 위치 데이터 연동 함수
function syncLocationWithCommonJs() {
    // 1. URL에 좌표가 있는지 확인
    const urlParams = new URLSearchParams(window.location.search);
    const lat = urlParams.get('lat');
    const lon = urlParams.get('lon');

    // RegionManager가 로드되었는지 확인
    if (typeof RegionManager === 'undefined') return;

    if (lat && lon) {
        // [케이스 A] URL에 좌표가 있음 (지도에서 왔거나 GPS로 옴)
        // -> 이 위치를 '현재 위치'로 저장해서 다른 페이지(메인 등)와 공유
        const regionNameEl = document.getElementById('display-region-name');
        const regionName = regionNameEl ? regionNameEl.innerText.trim() : '사용자 위치';

        // 현재 로드된 위치를 저장소에 업데이트
        RegionManager.save(regionName, lat, lon);

    } else {
        // [케이스 B] URL에 좌표가 없음 (메뉴 눌러서 그냥 들어옴)
        // -> 저장된 위치(메인에서 보던 곳)가 있다면 거기로 강제 이동
        const saved = RegionManager.load();
        if (saved) {
            console.log(`📍 저장된 위치(${saved.name})로 동기화`);
            window.location.replace(`/insights?lat=${saved.lat}&lon=${saved.lng}`);
        }
    }
}

// [공통] 현재 모드에 따른 차트 색상 반환
function getChartColors() {
    const isDarkMode = document.body.classList.contains('dark-mode');
    return {
        text: isDarkMode ? '#ecf0f1' : '#333333',
        subText: isDarkMode ? '#bdc3c7' : '#666666',
        grid: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)'
    };
}

// [공통] 요일 라벨 생성
function generateDayLabels() {
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    const labels = [];
    const today = new Date().getDay();
    for (let i = 0; i < 7; i++) {
        labels.push(days[(today + i) % 7]);
    }
    return labels;
}

// 1. 주간 기온 차트
function initTemperatureChart() {
    const ctx = document.getElementById('tempChart');
    if (!ctx) return;
    const maxData = typeof serverMaxTemps !== 'undefined' ? serverMaxTemps : [];
    const minData = typeof serverMinTemps !== 'undefined' ? serverMinTemps : [];
    const labels = generateDayLabels();

    tempChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: '최고기온',
                    data: maxData,
                    borderColor: '#ff7675',
                    backgroundColor: 'rgba(255, 118, 117, 0.2)',
                    tension: 0.3,
                    fill: true
                },
                {
                    label: '최저기온',
                    data: minData,
                    borderColor: '#74b9ff',
                    backgroundColor: 'rgba(116, 185, 255, 0.2)',
                    tension: 0.3,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {beginAtZero: false, grace: '10%', ticks: {color: 'white'}, grid: {color: 'rgba(255,255,255,0.1)'}},
                x: {ticks: {color: 'white'}, grid: {color: 'rgba(255,255,255,0.1)'}}
            },
            plugins: {
                legend: {labels: {color: 'white'}},
                tooltip: {mode: 'index', intersect: false}
            }
        }
    });
}

// 2. 미세먼지 차트
function initAirQualityChart() {
    const ctx = document.getElementById('airQualityChart');
    if (!ctx) return;
    const pm10Data = typeof serverPm10Data !== 'undefined' ? serverPm10Data : [];
    const pm25Data = typeof serverPm25Data !== 'undefined' ? serverPm25Data : [];

    const colors = getChartColors();
    const timeLabels = [];
    for (let i = 11; i >= 0; i--) {
        i === 0 ? timeLabels.push('현재') : timeLabels.push(i + 'H 전');
    }

    aqiChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: timeLabels,
            datasets: [
                {
                    label: '미세먼지',
                    data: pm10Data,
                    borderColor: '#f1c40f',
                    backgroundColor: 'rgba(241, 196, 15, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointRadius: 2
                },
                {
                    label: '초미세먼지',
                    data: pm25Data,
                    borderColor: '#e67e22',
                    backgroundColor: 'rgba(230, 126, 34, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointRadius: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {beginAtZero: true, ticks: {color: colors.subText}, grid: {color: colors.grid}},
                x: {ticks: {display: false}, grid: {display: false}}
            },
            plugins: {legend: {labels: {color: colors.text}}}
        }
    });

    if (pm10Data.length > 0) updateDustStatus(pm10Data[pm10Data.length - 1]);
}

// 3. 습도 및 바람 차트
function initEnvChart() {
    const ctx = document.getElementById('envChart');
    if (!ctx) return;

    const humidityData = typeof serverHumidityData !== 'undefined' ? serverHumidityData : [];
    const windData = typeof serverWindData !== 'undefined' ? serverWindData : [];
    const labels = typeof serverHourLabels !== 'undefined' ? serverHourLabels : [];

    const colors = getChartColors();

    envChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: '습도(%)',
                    data: humidityData,
                    backgroundColor: 'rgba(52, 152, 219, 0.5)',
                    borderColor: '#3498db',
                    borderWidth: 1,
                    order: 2,
                    yAxisID: 'y'
                },
                {
                    type: 'line',
                    label: '풍속(m/s)',
                    data: windData,
                    borderColor: '#2ecc71',
                    backgroundColor: 'rgba(46, 204, 113, 0.1)',
                    tension: 0.4,
                    pointRadius: 3,
                    order: 1,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true, position: 'top',
                    labels: {color: colors.text, font: {size: 11}, usePointStyle: true, boxWidth: 8}
                },
                tooltip: {mode: 'index', intersect: false}
            },
            scales: {
                x: {ticks: {color: colors.subText, maxTicksLimit: 6}, grid: {display: false}},
                y: {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    min: 0,
                    max: 100,
                    ticks: {color: '#3498db'},
                    grid: {color: colors.grid}
                },
                y1: {
                    type: 'linear',
                    display: true,
                    position: 'right',
                    min: 0,
                    grace: '20%',
                    grid: {drawOnChartArea: false},
                    ticks: {color: '#2ecc71'}
                }
            }
        }
    });
}

// 다크모드 감지 옵저버
function setupDarkModeObserver() {
    const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            if (mutation.attributeName === 'class') updateAllChartsColor();
        });
    });
    observer.observe(document.body, {attributes: true});
}

function updateAllChartsColor() {
    const colors = getChartColors();
    if (aqiChartInstance) {
        aqiChartInstance.options.scales.y.ticks.color = colors.subText;
        aqiChartInstance.options.scales.y.grid.color = colors.grid;
        aqiChartInstance.options.plugins.legend.labels.color = colors.text;
        aqiChartInstance.update();
    }
    if (envChartInstance) {
        envChartInstance.options.scales.x.ticks.color = colors.subText;
        envChartInstance.options.scales.y.grid.color = colors.grid;
        envChartInstance.options.plugins.legend.labels.color = colors.text;
        envChartInstance.update();
    }
}

function updateDustStatus(value) {
    const badge = document.getElementById('dust-badge');
    const msg = document.getElementById('dust-msg');
    if (!badge || !msg) return;

    badge.className = 'status-badge';
    if (value > 80) {
        badge.innerText = '나쁨';
        badge.classList.add('banned');
        msg.innerHTML = '공기가 탁합니다. <strong>마스크</strong> 필수!';
    } else if (value > 30) {
        badge.innerText = '보통';
        badge.classList.add('active');
        msg.innerHTML = '무난한 대기 상태입니다.';
    } else {
        badge.innerText = '좋음';
        badge.style.backgroundColor = '#3498db';
        badge.style.color = '#fff';
        msg.innerHTML = '공기가 상쾌합니다! <strong>환기</strong>하세요.';
    }
}