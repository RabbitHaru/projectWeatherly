/**
 * insights.js
 * 기상 인사이트 대시보드 전용 차트 로직
 * - 다크모드 실시간 차트 색상 변경
 * - GPS 위치 동기화 (common.js 연동)
 */

let tempChartInstance = null;
let aqiChartInstance = null;
let envChartInstance = null;

document.addEventListener('DOMContentLoaded', function () {

    // 1. 차트 초기화
    initTemperatureChart();
    initAirQualityChart();
    initEnvChart();

    // 2. 다크모드 감지
    setupDarkModeObserver();

    // 3. [핵심] GPS 버튼 연결 (ID: gps-sync-btn)
    // HTML에 id="gps-sync-btn"이 있어야 작동합니다!
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function(lat, lon) {
            // 위치 찾기 성공 시, 해당 좌표로 페이지 새로고침
            window.location.href = `/insights?lat=${lat}&lon=${lon}`;
        });
    } else {
        console.error("common.js가 로드되지 않아 GPS 기능을 사용할 수 없습니다.");
    }
});

// [공통] 현재 모드에 따른 차트 색상 반환
function getChartColors() {
    const isDarkMode = document.body.classList.contains('dark-mode');
    return {
        text: isDarkMode ? '#ecf0f1' : '#333333',       // 제목/범례
        subText: isDarkMode ? '#bdc3c7' : '#666666',    // 축 라벨
        grid: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)' // 그리드 선
    };
}

// [공통] 요일 라벨 생성
function generateDayLabels() {
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    const labels = [];
    const today = new Date().getDay();
    for (let i = 0; i < 7; i++) {
        const dayIndex = (today + i) % 7;
        labels.push(days[dayIndex]);
    }
    return labels;
}

// 1. 주간 기온 (배경이 항상 보라색 그라디언트 -> 글씨는 항상 흰색 고정)
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
                legend: { labels: { color: 'white' } },
                tooltip: {mode: 'index', intersect: false}
            }
        }
    });
}

// 2. 미세먼지 추이 (반응형 색상)
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
                y: {
                    beginAtZero: true,
                    ticks: {color: colors.subText},
                    grid: {color: colors.grid}
                },
                x: {
                    ticks: {display: false},
                    grid: {display: false}
                }
            },
            plugins: {
                legend: {labels: {color: colors.text}}
            }
        }
    });

    if (pm10Data.length > 0) updateDustStatus(pm10Data[pm10Data.length - 1]);
}

// 3. 습도 및 바람 분석 (반응형 색상)
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
                    display: true,
                    position: 'top',
                    labels: {
                        color: colors.text,
                        font: {size: 11},
                        usePointStyle: true,
                        boxWidth: 8
                    }
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            scales: {
                x: {
                    ticks: {color: colors.subText, maxTicksLimit: 6},
                    grid: {display: false}
                },
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

// [옵저버] 다크모드 변경 감지 및 차트 업데이트
function setupDarkModeObserver() {
    const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            if (mutation.attributeName === 'class') {
                updateAllChartsColor();
            }
        });
    });

    observer.observe(document.body, { attributes: true });
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