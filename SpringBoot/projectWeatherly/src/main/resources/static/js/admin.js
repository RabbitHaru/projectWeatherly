/**
 * admin.js (v5 - Dark Mode Fix)
 * 관리자 페이지 전용 스크립트
 */

const swalCustom = Swal.mixin({
    customClass: {
        popup: 'swal-custom-popup',
        confirmButton: 'btn-xs danger',
        cancelButton: 'action-btn-outline'
    },
    buttonsStyling: true
});

function getCsrfHeader() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');

    if (!tokenMeta || !headerMeta) return null;
    return {
        token: tokenMeta.getAttribute('content'),
        header: headerMeta.getAttribute('content')
    };
}

// ★ [수정] 돋보기 팝업 (다크모드 대응 추가)
function viewDetails(btn) {
    const reason = btn.getAttribute('data-reason');
    const details = btn.getAttribute('data-details');
    const formattedDetails = details ? details.replace(/\n/g, '<br>') : '상세 내용이 없습니다.';

    // 1. 현재 다크모드인지 확인 (body 태그에 클래스가 있는지)
    const isDark = document.body.classList.contains('dark-mode');

    // 2. 모드에 따라 글자색/배경색 결정
    const textColor = isDark ? '#dfe6e9' : '#444444'; // 다크모드면 밝은 회색, 아니면 진한 회색
    const bgColor = isDark ? '#2d3436' : '#ffffff';   // 팝업 배경색도 같이 맞춰줌

    Swal.fire({
        title: `신고 사유: ${reason}`,
        background: bgColor, // 배경색 적용
        color: textColor,    // 제목 글자색 적용
        html: `<div style="text-align: left; font-size: 0.9rem; line-height: 1.6; color: ${textColor}; max-height: 300px; overflow-y: auto;">
                ${formattedDetails}
               </div>`,
        showConfirmButton: true,
        confirmButtonText: '확인',
        confirmButtonColor: '#6c5ce7',
        width: '500px'
    });
}

// 1. 회원 정지
function suspendUser(memberId) {
    // 다크모드 확인
    const isDark = document.body.classList.contains('dark-mode');
    const bgColor = isDark ? '#2d3436' : '#ffffff';
    const textColor = isDark ? '#dfe6e9' : '#444444';

    Swal.fire({
        title: '회원 제재 관리',
        text: "정지할 기간(일수)을 입력하세요.",
        background: bgColor,
        color: textColor,
        input: 'number',
        inputValue: 0,
        inputPlaceholder: "0 입력 시 해제",
        showCancelButton: true,
        confirmButtonText: '확인',
        cancelButtonText: '취소',
        confirmButtonColor: '#ef4444'
    }).then((result) => {
        if (result.isConfirmed) {
            const days = parseInt(result.value);
            const csrf = getCsrfHeader();
            const headers = {};
            if (csrf) headers[csrf.header] = csrf.token;

            fetch(`/admin/members/${memberId}/suspend?days=${days}`, {
                method: 'POST',
                headers: headers,
                credentials: 'include'
            }).then(res => {
                if(res.ok) Swal.fire('완료', '처리되었습니다.', 'success').then(() => location.reload());
                else Swal.fire('실패', '오류 발생', 'error');
            });
        }
    });
}

// 2. 신고 처리
function resolveReport(reportId) {
    // 다크모드 확인
    const isDark = document.body.classList.contains('dark-mode');
    const bgColor = isDark ? '#2d3436' : '#ffffff';
    const textColor = isDark ? '#dfe6e9' : '#444444';

    Swal.fire({
        title: '신고 처리 및 제재',
        background: bgColor,
        color: textColor,
        input: 'select',
        inputOptions: {
            '0': '단순 삭제',
            '1': '1일 정지 + 삭제',
            '3': '3일 정지 + 삭제',
            '7': '7일 정지 + 삭제',
            '30': '30일 정지 + 삭제',
            '36500': '영구 정지'
        },
        inputValue: '0',
        showCancelButton: true,
        confirmButtonText: '처리하기',
        confirmButtonColor: '#ef4444'
    }).then((result) => {
        if (result.isConfirmed) {
            const banDays = result.value;
            const csrf = getCsrfHeader();
            const headers = {};
            if (csrf) headers[csrf.header] = csrf.token;

            fetch(`/api/admin/reports/${reportId}/process?banDays=${banDays}`, {
                method: 'POST',
                headers: headers,
                credentials: 'include'
            }).then(async response => {
                if (response.ok) {
                    Swal.fire('성공', '처리되었습니다.', 'success').then(() => window.location.reload());
                } else {
                    Swal.fire('실패', `서버 오류 (코드: ${response.status})`, 'error');
                }
            });
        }
    });
}