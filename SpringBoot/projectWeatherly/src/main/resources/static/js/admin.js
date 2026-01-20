/**
 * admin.js
 * 관리자 페이지 전용 스크립트
 * - 회원 정지/해제 API 호출
 * - 신고 처리 API 호출
 */

// 1. 회원 상태 변경 (정지 <-> 해제)
function toggleMemberStatus(memberId, currentStatus) {
    // currentStatus: true(활동중)이면 -> 정지(false) 시킴
    // currentStatus: false(정지됨)이면 -> 해제(true) 시킴
    const nextStatus = !currentStatus;
    const actionName = nextStatus ? '해제' : '정지';

    if (!confirm(`해당 회원을 정말로 '${actionName}' 처리하시겠습니까?`)) return;

    fetch(`/api/admin/members/${memberId}/status?isActive=${nextStatus}`, {
        method: 'POST'
    })
        .then(response => {
            if (response.ok) {
                alert(`회원 상태가 ${actionName}되었습니다.`);
                window.location.reload(); // 새로고침해서 변경사항 반영
            } else {
                alert('처리 중 오류가 발생했습니다.');
            }
        })
        .catch(error => console.error('Error:', error));
}

// 2. 신고 처리 (대기중 -> 처리완료)
function resolveReport(reportId) {
    if (!confirm('이 신고를 처리 완료(RESOLVED) 상태로 변경하시겠습니까?')) return;

    fetch(`/api/admin/reports/${reportId}/process`, {
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