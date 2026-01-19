// 옷차림 추천(메인) 페이지 전용 스크립트

document.addEventListener('DOMContentLoaded', function () {
    setupCardInteractions();
    setupGpsButton();
});

// 1. 주간 예보 카드 호버 효과
function setupCardInteractions() {
    const dayCards = document.querySelectorAll('.day-card');

    dayCards.forEach(card => {
        card.addEventListener('mouseenter', () => {
            card.style.transform = 'translateY(-5px)';
            // 그림자 효과를 CSS에서 처리하지 않았다면 여기서 추가 가능
            // card.style.boxShadow = '0 10px 20px rgba(0,0,0,0.1)';
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = 'translateY(0)';
            // card.style.boxShadow = '';
        });
    });
}

// 2. GPS 버튼 연동 (common.js의 bindGpsButton 활용)
function setupGpsButton() {
    // bindGpsButton 함수가 common.js에 정의되어 있는지 확인
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lon) {
            // 위치 조회 성공 시 실행될 콜백: 해당 좌표로 페이지 새로고침
            window.location.href = `/outfit?lat=${lat}&lon=${lon}`;
        });
    } else {
        console.warn('bindGpsButton 함수를 찾을 수 없습니다. common.js가 로드되었는지 확인하세요.');
    }
}