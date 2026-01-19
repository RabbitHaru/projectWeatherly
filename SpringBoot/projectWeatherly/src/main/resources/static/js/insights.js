document.addEventListener('DOMContentLoaded', function () {
    initTemperatureChart();
    initAirQualityChart();
    initEnvChart();
});

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

// 1. 주간 기온
function initTemperatureChart() {
    const ctx = document.getElementById('tempChart');
    if (!ctx) return;
    const maxData = typeof serverMaxTemps !== 'undefined' ? serverMaxTemps : [];
    const minData = typeof serverMinTemps !== 'undefined' ? serverMinTemps : [];
    const labels = generateDayLabels();

    Chart.defaults.color = '#fff';
    Chart.defaults.borderColor = 'rgba(255,255,255,0.2)';

    new Chart(ctx, {
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
                y: { beginAtZero: false, grace: '10%', ticks: { color: 'white' } },
                x: { ticks: { color: 'white' } }
            },
            plugins: { tooltip: { mode: 'index', intersect: false } }
        }
    });
}

// 2. 미세먼지 추이
function initAirQualityChart() {
    const ctx = document.getElementById('airQualityChart');
    if (!ctx) return;
    const pm10Data = typeof serverPm10Data !== 'undefined' ? serverPm10Data : [];
    const pm25Data = typeof serverPm25Data !== 'undefined' ? serverPm25Data : [];
    const timeLabels = [];
    for(let i=11; i>=0; i--) {
        i===0 ? timeLabels.push('현재') : timeLabels.push(i + 'H 전');
    }

    new Chart(ctx, {
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
                y: { beginAtZero: true, ticks: { color: '#666' }, grid: { color: '#eee' } },
                x: { ticks: { display: false }, grid: { display: false } }
            },
            plugins: { legend: { labels: { color: '#333' } } }
        }
    });

    if (pm10Data.length > 0) updateDustStatus(pm10Data[pm10Data.length - 1]);
}

// 3. 습도 및 바람 분석
function initEnvChart() {
    const ctx = document.getElementById('envChart');
    if (!ctx) return;
    const humidityData = typeof serverHumidityData !== 'undefined' ? serverHumidityData : [];
    const windData = typeof serverWindData !== 'undefined' ? serverWindData : [];
    const labels = typeof serverHourLabels !== 'undefined' ? serverHourLabels : [];
    const darkTextColor = '#666';

    new Chart(ctx, {
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
            plugins: { tooltip: { mode: 'index', intersect: false } },
            scales: {
                x: { ticks: { color: darkTextColor, maxTicksLimit: 6 }, grid: { display: false } },
                y: {
                    type: 'linear', display: true, position: 'left', min: 0, max: 100,
                    title: { display: true, text: '습도(%)' },
                    ticks: { color: '#3498db' }, grid: { color: '#eee' }
                },
                y1: {
                    type: 'linear', display: true, position: 'right', min: 0, grace: '20%',
                    title: { display: true, text: '풍속(m/s)' },
                    grid: { drawOnChartArea: false }, ticks: { color: '#2ecc71' }
                }
            }
        }
    });
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

// [NEW] 위치 동기화 함수
function syncLocation() {
    if (!navigator.geolocation) {
        alert('이 브라우저는 위치 정보를 지원하지 않습니다.');
        return;
    }

    // 버튼 회전 애니메이션 효과
    const btnIcon = document.querySelector('.btn-sync-location i');
    if(btnIcon) btnIcon.classList.add('fa-spin');

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const lat = position.coords.latitude;
            const lon = position.coords.longitude;
            // 좌표를 포함하여 페이지 새로고침 (Controller가 받아서 처리함)
            window.location.href = `/insights?lat=${lat}&lon=${lon}`;
        },
        (error) => {
            if(btnIcon) btnIcon.classList.remove('fa-spin');
            console.error('위치 확인 실패:', error);
            alert('위치 정보를 가져올 수 없습니다. 권한을 확인해주세요.');
        }
    );
}