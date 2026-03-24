/**
 * insights.js
 * 기상 인사이트 대시보드 로직 (Full Version)
 */

let tempChartInstance = null;
let aqiChartInstance = null;
let envChartInstance = null;

document.addEventListener('DOMContentLoaded', function () {
    // 1. 위치 데이터 동기화 (URL 파라미터 체크 및 리다이렉트)
    syncLocationWithCommonJs();

    // 2. 지역명 보정 (강원도 -> 강원특별자치도)
    fixLocationTextInInsights();

    // 3. 차트 초기화 및 기능 설정
    initTemperatureChart();
    initAirQualityChart();
    initEnvChart();
    setupDarkModeObserver();

    // 4. GPS 버튼 연결
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lon) {
            // ⭐ URL만 변경하면 common.js가 자동으로 세션에 저장함
            window.location.href = `/insights?lat=${lat}&lon=${lon}`;
        });
    }
});

// ⭐ 화면 텍스트 강제 보정 함수
function fixLocationTextInInsights() {
    const locEl = document.getElementById('display-region-name');
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

// ⭐ common.js와 위치 데이터 연동 함수
function syncLocationWithCommonJs() {
    const urlParams = new URLSearchParams(window.location.search);
    const lat = urlParams.get('lat');
    const lon = urlParams.get('lon');

    if (typeof RegionManager === 'undefined') return;

    if (lat && lon) {
        // URL에 좌표가 있으면 화면 갱신을 위해(이름 업데이트용) 저장
        const regionNameEl = document.getElementById('display-region-name');
        const regionName = regionNameEl ? regionNameEl.innerText.trim() : '사용자 위치';
        RegionManager.save(regionName, lat, lon);
    } else {
        // 좌표가 없으면 저장된 위치로 이동
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

    const colors = getChartColors();

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
                y: {beginAtZero: false, grace: '10%', ticks: {color: colors.subText}, grid: {color: colors.grid}},
                x: {ticks: {color: colors.subText}, grid: {color: colors.grid}}
            },
            plugins: {
                legend: {labels: {color: colors.text}},
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
    if (tempChartInstance) {
        tempChartInstance.options.scales.x.ticks.color = colors.subText;
        tempChartInstance.options.scales.x.grid.color = colors.grid;
        tempChartInstance.options.scales.y.ticks.color = colors.subText;
        tempChartInstance.options.scales.y.grid.color = colors.grid;
        tempChartInstance.options.plugins.legend.labels.color = colors.text;
        tempChartInstance.update();
    }
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