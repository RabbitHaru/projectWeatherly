// outfit.js - 옷차림 추천(메인) 페이지 (지역 고정 기능 추가)

document.addEventListener('DOMContentLoaded', function () {
    // ⭐ 1. 저장된 위치가 있는지 확인 (URL 파라미터가 없을 때만!)
    checkSavedLocation();

    setupCardInteractions();
    setupGpsButton();
});

function checkSavedLocation() {
    // URL에 이미 lat, lon이 있다면(즉, 특정 지역을 보러 온 거라면) 리다이렉트 안 함
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('lat') && urlParams.has('lon')) {
        return;
    }

    // URL 파라미터가 없을 때만 저장된 위치를 확인
    // (common.js가 로드되어 있어야 RegionManager 사용 가능)
    if (typeof RegionManager !== 'undefined') {
        const saved = RegionManager.load();
        if (saved && saved.lat && saved.lng) {
            console.log(`📍 옷차림 페이지: 저장된 위치(${saved.name})로 이동합니다.`);
            // 저장된 위치로 페이지 새로고침 (파라미터 추가)
            window.location.replace(`/outfit?lat=${saved.lat}&lon=${saved.lng}`);
        }
    }
}

// 2. 주간 예보 카드 호버 효과
function setupCardInteractions() {
    const dayCards = document.querySelectorAll('.day-card');

    dayCards.forEach(card => {
        card.addEventListener('mouseenter', () => {
            card.style.transform = 'translateY(-5px)';
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = 'translateY(0)';
        });
    });
}

// 3. GPS 버튼 연동 (common.js의 bindGpsButton 활용)
function setupGpsButton() {
    // bindGpsButton 함수가 common.js에 정의되어 있는지 확인
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lon) {
            // GPS 위치를 찾으면 저장된 위치(광주 등)를 지우고 현재 위치로 이동
            if (typeof RegionManager !== 'undefined') RegionManager.clear();
            window.location.href = `/outfit?lat=${lat}&lon=${lon}`;
        });
    } else {
        console.warn('bindGpsButton 함수를 찾을 수 없습니다. common.js가 로드되었는지 확인하세요.');
    }
}