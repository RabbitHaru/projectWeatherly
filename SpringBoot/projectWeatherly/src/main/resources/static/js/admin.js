/**
 * admin.js
 * 관리자 페이지 전용 스크립트
 */

// ★ 1. [변경됨] 회원 정지 (기간 입력)
function suspendUser(memberId) {
    // 1. 관리자에게 기간 입력받기
    const input = prompt("정지할 기간(일수)을 입력하세요.\n(예: 3, 7, 30)\n※ '0' 입력 시 정지가 해제됩니다.");

    // 취소 버튼 누른 경우
    if (input === null) return;

    const days = parseInt(input);

    // 숫자가 아닌 경우 체크
    if (isNaN(days)) {
        alert("올바른 숫자를 입력해주세요.");
        return;
    }

    // 안내 메시지 설정
    let confirmMsg = "";
    if (days > 0) {
        confirmMsg = `해당 회원을 ${days}일간 정지하시겠습니까?`;
    } else {
        confirmMsg = "해당 회원의 정지를 해제하시겠습니까?";
    }

    if (!confirm(confirmMsg)) return;

    // 2. 서버로 전송 (/admin/... 경로 사용)
    fetch(`/admin/members/${memberId}/suspend?days=${days}`, {
        method: 'POST'
    })
        .then(response => {
            if (response.ok) {
                alert("처리가 완료되었습니다.");
                window.location.reload(); // 새로고침
            } else {
                alert("처리 중 오류가 발생했습니다.");
            }
        })
        .catch(error => console.error('Error:', error));
}


// 2. 신고 처리 (기존 유지)
function resolveReport(reportId) {
    if (!confirm('이 신고를 처리 완료(RESOLVED) 상태로 변경하시겠습니까?')) return;

    // [수정] 경로에서 /api 제거 (Controller 매핑에 맞춤)
    fetch(`/admin/reports/${reportId}/process?status=RESOLVED`, {
        method: 'POST'
    })
        .then(response => {
            if (response.ok) {
                alert('신고 처리가 완료되었습니다.');
                window.location.reload();
            } else {
                alert('처리 실패');
            }
        })
        .catch(error => console.error('Error:', error));
}