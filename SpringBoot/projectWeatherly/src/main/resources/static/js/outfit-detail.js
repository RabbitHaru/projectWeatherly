// outfit-detail.js - 옷차림 상세 페이지

document.addEventListener('DOMContentLoaded', function () {
    // 1. 저장된 위치 확인 및 이동 처리
    checkSavedLocation();

    // 2. 지역명 보정 실행 (강원도 -> 강원특별자치도)
    fixLocationTextInDetail();

    // 3. 다크모드 초기화
    if (localStorage.getItem('darkMode') === 'true') {
        document.body.classList.add('dark-mode');
    }

    // 4. 기능 초기화
    setupTempCardHover();
    setupConditionCardClick();

    // 5. GPS 버튼 이벤트 바인딩
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function (lat, lng) {
            // ⭐ URL 변경 -> common.js가 자동 저장
            window.location.href = `/outfit/detail?lat=${lat}&lon=${lng}`;
        });
    }
});

// ⭐ 지역명 강제 보정 함수 (상세 페이지용)
function fixLocationTextInDetail() {
    const locEl = document.querySelector('.location-name') ||
        document.getElementById('current-location') ||
        document.querySelector('.location-title span');

    if (locEl && typeof getFullSidoName === 'function' && typeof extractSidoName === 'function') {
        const originalText = locEl.textContent.trim();
        const shortName = extractSidoName(originalText);
        const fullName = getFullSidoName(shortName);

        if (originalText !== fullName) {
            console.log(`🔧 Detail 지역명 보정 완료: ${originalText} -> ${fullName}`);
            locEl.textContent = fullName;
        }
    }
}

function checkSavedLocation() {
    const urlParams = new URLSearchParams(window.location.search);
    if (!urlParams.has('lat') && typeof RegionManager !== 'undefined') {
        const saved = RegionManager.load();
        if (saved && saved.lat && saved.lng) {
            console.log(`📍 상세 페이지: 저장된 위치(${saved.name})로 이동`);
            window.location.replace(`/outfit/detail?lat=${saved.lat}&lon=${saved.lng}`);
        }
    }
}

function setupTempCardHover() {
    const tempCards = document.querySelectorAll('.temp-card');
    tempCards.forEach(card => {
        if (!card.classList.contains('current')) {
            card.addEventListener('mouseenter', function () {
                this.style.transform = 'translateY(-8px)';
                this.style.boxShadow = '0 12px 24px rgba(0, 0, 0, 0.2)';
            });
            card.addEventListener('mouseleave', function () {
                this.style.transform = 'translateY(0)';
                this.style.boxShadow = '';
            });
        }
    });
}

function setupConditionCardClick() {
    const conditionCards = document.querySelectorAll('.condition-card');
    conditionCards.forEach(card => {
        card.addEventListener('click', function () {
            const conditionTitle = this.querySelector('.condition-title').textContent;
            const itemsText = Array.from(this.querySelectorAll('li')).map(item => `• ${item.textContent}`).join('\n');
            alert(`[${conditionTitle}]\n\n${itemsText}`);
        });
    });
}