// outfit.js - 옷차림 추천(메인) 페이지

document.addEventListener('DOMContentLoaded', function () {
    // 1. 저장된 위치 확인
    checkSavedLocation();

    // 2. 강원도 등 지역명 보정
    fixLocationText();

    setupCardInteractions();
    setupGpsButton();
});

// ⭐ 화면 텍스트 강제 보정 함수
function fixLocationText() {
    const locEl = document.getElementById('current-location') || document.querySelector('.location-name');

    if (locEl && typeof getFullSidoName === 'function' && typeof extractSidoName === 'function') {
        const originalText = locEl.textContent;
        const shortName = extractSidoName(originalText);
        const fullName = getFullSidoName(shortName);

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
        card.addEventListener('mouseenter', () => {
            card.style.transform = 'translateY(-5px)';
        });
        card.addEventListener('mouseleave', () => {
            card.style.transform = 'translateY(0)';
        });
    });
}

function setupGpsButton() {
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lon) {
            // ⭐ URL만 변경하면 common.js가 자동으로 세션에 저장함
            window.location.href = `/outfit?lat=${lat}&lon=${lon}`;
        });
    }
}