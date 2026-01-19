// 옷차림 상세 페이지 전용 스크립트

document.addEventListener('DOMContentLoaded', function () {
    setupTempCardHover();
    setupConditionCardClick();
});

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