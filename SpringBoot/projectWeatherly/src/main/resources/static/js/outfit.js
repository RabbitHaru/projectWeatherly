// outfit.js - 옷차림 추천(메인) 페이지 (강원도 표기 오류 강제 수정 추가)

document.addEventListener('DOMContentLoaded', function () {
    // 1. 저장된 위치 확인
    checkSavedLocation();

    // ⭐ [추가] 화면에 '강원도'라고 떠있으면 '강원특별자치도'로 강제 변경
    fixLocationText();

    setupCardInteractions();
    setupGpsButton();
});

// ⭐ 화면 텍스트 강제 보정 함수
function fixLocationText() {
    // HTML에서 지역명이 들어가는 요소 ID를 확인해야 함 (보통 current-location)
    const locEl = document.getElementById('current-location') || document.querySelector('.location-name');

    if (locEl && typeof getFullSidoName === 'function' && typeof extractSidoName === 'function') {
        const originalText = locEl.textContent;
        // 1. 원래 텍스트에서 '강원', '강원도' 등을 추출해서 -> '강원'으로 만듦
        const shortName = extractSidoName(originalText);
        // 2. '강원' -> '강원특별자치도'로 변환
        const fullName = getFullSidoName(shortName);

        // 3. 텍스트가 다르면 업데이트
        if (originalText.trim() !== fullName) {
            console.log(`🔧 지역명 보정: ${originalText} -> ${fullName}`);
            locEl.textContent = fullName;
        }
    }
}

function checkSavedLocation() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('lat') && urlParams.has('lon')) return;

    if (typeof RegionManager !== 'undefined') {
        const saved = RegionManager.load();
        if (saved && saved.lat && saved.lng) {
            window.location.replace(`/outfit?lat=${saved.lat}&lon=${saved.lng}`);
        }
    }
}

function setupCardInteractions() {
    const dayCards = document.querySelectorAll('.day-card');
    dayCards.forEach(card => {
        card.addEventListener('mouseenter', () => { card.style.transform = 'translateY(-5px)'; });
        card.addEventListener('mouseleave', () => { card.style.transform = 'translateY(0)'; });
    });
}

function setupGpsButton() {
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lon) {
            if (typeof RegionManager !== 'undefined') RegionManager.clear();
            window.location.href = `/outfit?lat=${lat}&lon=${lon}`;
        });
    }
}