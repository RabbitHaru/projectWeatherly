// outfit-detail.js - 옷차림 상세 페이지 (지역 고정 기능 추가)

document.addEventListener('DOMContentLoaded', function () {
    // ⭐ 1. 저장된 위치 확인 (URL 파라미터 없을 시)
    checkSavedLocation();

    // 2. 다크모드 초기화 (HTML에서 이동됨)
    if (localStorage.getItem('darkMode') === 'true') {
        document.body.classList.add('dark-mode');
    }

    // 3. 기능 초기화
    setupTempCardHover();
    setupConditionCardClick();

    // 4. GPS 버튼 이벤트 바인딩
    if (typeof bindGpsButton === 'function') {
        bindGpsButton('gps-sync-btn', function(lat, lng) {
            // 위치 정보를 가지고 현재 페이지 리로드
            if (typeof RegionManager !== 'undefined') RegionManager.clear();
            window.location.href = `/outfit/detail?lat=${lat}&lon=${lng}`;
        });
    }
});

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