// outfit-detail.js - 옷차림 상세 페이지 (지역 고정 기능 추가)

// outfit-detail.js - 옷차림 상세 페이지 (지역 보정 및 고정 기능)

document.addEventListener('DOMContentLoaded', function () {
    // 1. 저장된 위치 확인 및 이동 처리
    checkSavedLocation();

    // ⭐ 2. 지역명 보정 실행 (강원도 -> 강원특별자치도)
    // 여러 가능성 있는 요소를 모두 체크하도록 개선
    fixLocationTextInDetail();

    // 3. 다크모드 초기화
    if (localStorage.getItem('darkMode') === 'true') {
        document.body.classList.add('dark-mode');
    }

    // 4. 기능 초기화 (카드 효과 등)
    setupTempCardHover();
    setupConditionCardClick();

    // 5. GPS 버튼 이벤트 바인딩
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function(lat, lng) {
            if (typeof RegionManager !== 'undefined') RegionManager.clear();
            window.location.href = `/outfit/detail?lat=${lat}&lon=${lng}`;
        });
    }
});

// ⭐ 지역명 강제 보정 함수 (상세 페이지용)
function fixLocationTextInDetail() {
    // 1. 여러 검색 조건으로 지역명 요소 찾기
    const locEl = document.querySelector('.location-name') ||
        document.getElementById('current-location') ||
        document.querySelector('.location-title span'); // 구조에 따라 추가

    if (locEl && typeof getFullSidoName === 'function' && typeof extractSidoName === 'function') {
        const originalText = locEl.textContent.trim();

        // "강원" 또는 "강원도" 추출
        const shortName = extractSidoName(originalText);
        // 정식 명칭으로 변환 ("강원특별자치도")
        const fullName = getFullSidoName(shortName);

        if (originalText !== fullName) {
            console.log(`🔧 Detail 지역명 보정 완료: ${originalText} -> ${fullName}`);
            locEl.textContent = fullName;
        }
    }
}

function checkSavedLocation() {
    const urlParams = new URLSearchParams(window.location.search);
    // URL에 좌표가 없고, 저장된 위치가 있다면 이동
    if (!urlParams.has('lat') && typeof RegionManager !== 'undefined') {
        const saved = RegionManager.load();
        if (saved && saved.lat && saved.lng) {
            console.log(`📍 상세 페이지: 저장된 위치(${saved.name})로 이동`);
            window.location.replace(`/outfit/detail?lat=${saved.lat}&lon=${saved.lng}`);
        }
    }
}

// 1. 온도 카드 호버 효과 (현재 구간 제외)
function setupTempCardHover() {
    const tempCards = document.querySelectorAll('.temp-card');

    tempCards.forEach(card => {
        // 'current' 클래스가 없는 카드만 호버 효과 적용
        if (!card.classList.contains('current')) {
            card.addEventListener('mouseenter', function () {
                this.style.transform = 'translateY(-8px)';
                this.style.boxShadow = '0 12px 24px rgba(0, 0, 0, 0.2)';
            });

            card.addEventListener('mouseleave', function () {
                this.style.transform = 'translateY(0)';
                this.style.boxShadow = ''; // CSS 기본값으로 복귀
            });
        }
    });
}

// 2. 조건 카드 클릭 시 상세 정보 알림창 표시
function setupConditionCardClick() {
    const conditionCards = document.querySelectorAll('.condition-card');

    conditionCards.forEach(card => {
        card.addEventListener('click', function () {
            const conditionTitle = this.querySelector('.condition-title').textContent;
            const conditionItems = this.querySelectorAll('li');
            let itemsText = '';

            conditionItems.forEach(item => {
                itemsText += `• ${item.textContent}\n`;
            });

            alert(`[${conditionTitle}]\n\n${itemsText}`);
        });
    });
}