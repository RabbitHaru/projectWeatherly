// [수정됨] IDE 오류 방지 주석 처리
// const postId = ... (HTML 내 inline script에서 처리됨)

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || document.querySelector('input[name="_csrf"]')?.value;
}

// 1. 게시글 좋아요
function toggleLike() {
    fetch('/community/boards/' + postId + '/like', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': getCsrfToken(),
            'Content-Type': 'application/json'
        }
    })
        .then(res => {
            if (res.status === 401) {
                if(confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) {
                    window.location.href='/login';
                }
                return null;
            }
            return res.json();
        })
        .then(data => {
            if(data && data.success) {
                document.getElementById('like-count').textContent = data.likeCount;
                // 좋아요 상태에 따라 클래스 토글 (CSS로 빨간색 처리)
                const btn = document.getElementById('like-btn');
                if (data.liked) {
                    btn.classList.add('liked');
                } else {
                    btn.classList.remove('liked');
                }
            }
        })
        .catch(err => console.error("Error:", err));
}

// 2. 댓글 좋아요
function toggleCommentLike(commentId) {
    if (!commentId) return;

    fetch('/community/boards/' + postId + '/comments/' + commentId + '/like', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': getCsrfToken(),
            'Content-Type': 'application/json'
        }
    })
        .then(res => {
            if (res.status === 401) {
                if(confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) {
                    window.location.href='/login';
                }
                return null;
            }
            return res.json();
        })
        .then(data => {
            if(data && data.success) {
                // 해당 댓글의 좋아요 수 업데이트
                const countSpan = document.getElementById('comment-like-count-' + commentId);
                if (countSpan) countSpan.textContent = data.likeCount;

                // 해당 댓글의 하트 버튼 스타일 업데이트
                const btn = document.getElementById('comment-like-btn-' + commentId);
                if (btn) {
                    if (data.liked) {
                        btn.classList.add('liked');
                    } else {
                        btn.classList.remove('liked');
                    }
                }
            } else {
                alert(data.message || '오류가 발생했습니다.');
            }
        })
        .catch(err => console.error("Error:", err));
}

// [중요] 조회수 증가 fetch 로직 제거됨 (Controller에서 처리)
document.addEventListener('DOMContentLoaded', function() {
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