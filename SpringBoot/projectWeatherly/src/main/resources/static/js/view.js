/* [수정됨] IDE 오류 방지 주석 처리 */
const postId = /*[[${board.id}]]*/ 0;

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || document.querySelector('input[name="_csrf"]')?.value;
}

// 게시글 좋아요 (AJAX 유지)
function toggleLike() {
    fetch('/community/boards/' + postId + '/like', { method: 'POST', headers: { 'X-CSRF-TOKEN': getCsrfToken() }})
        .then(res => res.status === 401 ? (window.location.href='/login') : res.json())
        .then(data => {
            if(data.success) {
                document.getElementById('like-count').textContent = data.likeCount;
                document.getElementById('like-btn').classList.toggle('liked', data.liked);
            }
        });
}

// 조회수 증가
document.addEventListener('DOMContentLoaded', function() {
    if (postId > 0) {
        fetch('/community/boards/' + postId + '/view', { method: 'POST', headers: { 'X-CSRF-TOKEN': getCsrfToken() }});
    }

    // 이미지 모달 처리
    document.querySelectorAll('.gallery-image').forEach(img => {
        img.addEventListener('click', function() {
            document.getElementById('modal-image').src = this.getAttribute('data-image');
            document.getElementById('image-modal').style.display = 'flex';
        });
    });
});

function closeImageModal() { document.getElementById('image-modal').style.display = 'none'; }
function closeReportModal() { document.getElementById('report-modal').style.display = 'none'; }

// 신고 모달 열기
function openReportModal(type, id) {
    document.getElementById('report-type').value = type;
    document.getElementById('report-target-id').value = id;
    document.getElementById('report-modal').style.display = 'block';
}