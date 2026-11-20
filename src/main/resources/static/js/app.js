// 전역 변수
let allHosts = [];
let filteredHosts = [];
let currentPage = 1;
let rowsPerPage = 50;
let currentFilter = 'all';
let currentSort = { column: null, direction: 'asc' };

// DOM 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
    loadHosts();
});

/**
 * 이벤트 리스너 초기화
 */
function initializeEventListeners() {
    // Sync 버튼
    document.getElementById('syncBtn').addEventListener('click', syncHosts);

    // 필터 버튼들
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.dataset.filter;
            applyFilters();
        });
    });

    // 검색 입력
    document.getElementById('searchInput').addEventListener('input', function () {
        applyFilters();
    });

    // 페이지당 행 수 변경
    document.getElementById('rowsPerPage').addEventListener('change', function () {
        rowsPerPage = parseInt(this.value);
        currentPage = 1;
        renderTable();
    });

    // 테이블 헤더 정렬
    document.querySelectorAll('th[data-sort]').forEach(th => {
        th.addEventListener('click', function () {
            const column = this.dataset.sort;
            sortTable(column);
        });
    });

    // 모달 닫기 버튼
    document.querySelector('.close').addEventListener('click', closeModal);

    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function (event) {
        const modal = document.getElementById('hostDetailModal');
        if (event.target === modal) {
            closeModal();
        }
    });
}

/**
 * 호스트 목록 로드
 */
async function loadHosts() {
    try {
        const response = await fetch('/api/hosts');
        if (!response.ok) {
            throw new Error('데이터를 불러오는데 실패했습니다.');
        }
        allHosts = await response.json();
        filteredHosts = [...allHosts];
        renderTable();
        updateEntriesInfo();
    } catch (error) {
        console.error('Error loading hosts:', error);
        showError('데이터를 불러오는데 실패했습니다.');
    }
}

/**
 * 호스트 목록 동기화
 */
async function syncHosts() {
    const syncBtn = document.getElementById('syncBtn');
    syncBtn.disabled = true;
    syncBtn.textContent = 'Syncing...';

    try {
        const response = await fetch('/api/hosts/sync');
        if (!response.ok) {
            throw new Error('동기화에 실패했습니다.');
        }
        allHosts = await response.json();
        filteredHosts = [...allHosts];
        currentPage = 1;
        renderTable();
        updateEntriesInfo();
        alert('동기화가 완료되었습니다.');
    } catch (error) {
        console.error('Error syncing hosts:', error);
        alert('동기화에 실패했습니다.');
    } finally {
        syncBtn.disabled = false;
        syncBtn.textContent = 'Sync';
    }
}

/**
 * 필터 적용
 */
function applyFilters() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();

    filteredHosts = allHosts.filter(host => {
        // 필터 조건
        let matchesFilter = true;
        if (currentFilter !== 'all') {
            const filterLower = currentFilter.toLowerCase();
            matchesFilter =
                (host.hostname && host.hostname.toLowerCase().includes(filterLower)) ||
                (host.osVersion && host.osVersion.toLowerCase().includes(filterLower)) ||
                (host.osVersionDetail && host.osVersionDetail.toLowerCase().includes(filterLower)) ||
                (host.serviceCode && host.serviceCode.toLowerCase().includes(filterLower));
        }

        // 검색 조건
        let matchesSearch = true;
        if (searchTerm) {
            matchesSearch =
                (host.ip && host.ip.toLowerCase().includes(searchTerm)) ||
                (host.hostname && host.hostname.toLowerCase().includes(searchTerm)) ||
                (host.serviceCode && host.serviceCode.toLowerCase().includes(searchTerm)) ||
                (host.vendor && host.vendor.toLowerCase().includes(searchTerm)) ||
                (host.arch && host.arch.toLowerCase().includes(searchTerm)) ||
                (host.kernel && host.kernel.toLowerCase().includes(searchTerm)) ||
                (host.osVersion && host.osVersion.toLowerCase().includes(searchTerm)) ||
                (host.osVersionDetail && host.osVersionDetail.toLowerCase().includes(searchTerm));
        }

        return matchesFilter && matchesSearch;
    });

    currentPage = 1;
    renderTable();
    updateEntriesInfo();
}

/**
 * 테이블 정렬
 */
function sortTable(column) {
    if (currentSort.column === column) {
        currentSort.direction = currentSort.direction === 'asc' ? 'desc' : 'asc';
    } else {
        currentSort.column = column;
        currentSort.direction = 'asc';
    }

    filteredHosts.sort((a, b) => {
        let aVal = a[column] || '';
        let bVal = b[column] || '';

        if (typeof aVal === 'string') {
            aVal = aVal.toLowerCase();
            bVal = bVal.toLowerCase();
        }

        if (aVal < bVal) return currentSort.direction === 'asc' ? -1 : 1;
        if (aVal > bVal) return currentSort.direction === 'asc' ? 1 : -1;
        return 0;
    });

    // 정렬 아이콘 업데이트
    document.querySelectorAll('th[data-sort]').forEach(th => {
        th.classList.remove('sorted-asc', 'sorted-desc');
    });
    const sortedTh = document.querySelector(`th[data-sort="${column}"]`);
    sortedTh.classList.add(`sorted-${currentSort.direction}`);

    renderTable();
}

/**
 * 테이블 렌더링
 */
function renderTable() {
    const tbody = document.getElementById('hostsTableBody');
    const startIndex = (currentPage - 1) * rowsPerPage;
    const endIndex = startIndex + rowsPerPage;
    const pageHosts = filteredHosts.slice(startIndex, endIndex);

    if (pageHosts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="no-data">데이터가 없습니다.</td></tr>';
    } else {
        tbody.innerHTML = pageHosts.map(host => `
            <tr onclick="showHostDetail('${host.hostId}', '${escapeHtml(host.hostname)}', '${escapeHtml(host.ip)}', '${escapeHtml(host.serviceCode || '')}')">
                <td>${host.hostId || 'N/A'}</td>
                <td>${host.ip || '-'}</td>
                <td>${host.hostname || '-'}</td>
                <td>${host.serviceCode || '-'}</td>
                <td>${host.vendor || '-'}</td>
                <td>${host.arch || '-'}</td>
                <td>${host.kernel || '-'}</td>
                <td>${host.osVersion || '-'}</td>
                <td>${host.osVersionDetail || '-'}</td>
            </tr>
        `).join('');
    }

    renderPagination();
}

/**
 * 페이지네이션 렌더링
 */
function renderPagination() {
    const pagination = document.getElementById('pagination');
    const totalPages = Math.ceil(filteredHosts.length / rowsPerPage);

    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';

    // 이전 버튼
    html += `<button ${currentPage === 1 ? 'disabled' : ''} onclick="changePage(${currentPage - 1})">이전</button>`;

    // 페이지 번호
    const maxVisiblePages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    if (startPage > 1) {
        html += `<button onclick="changePage(1)">1</button>`;
        if (startPage > 2) html += `<button disabled>...</button>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="${i === currentPage ? 'active' : ''}" onclick="changePage(${i})">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<button disabled>...</button>`;
        html += `<button onclick="changePage(${totalPages})">${totalPages}</button>`;
    }

    // 다음 버튼
    html += `<button ${currentPage === totalPages ? 'disabled' : ''} onclick="changePage(${currentPage + 1})">다음</button>`;

    pagination.innerHTML = html;
}

/**
 * 페이지 변경
 */
function changePage(page) {
    currentPage = page;
    renderTable();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * 엔트리 정보 업데이트
 */
function updateEntriesInfo() {
    const info = document.querySelector('.entries-info');
    info.textContent = `전체 ${filteredHosts.length}개 (총 ${allHosts.length}개)`;
}

/**
 * 에러 표시
 */
function showError(message) {
    const tbody = document.getElementById('hostsTableBody');
    tbody.innerHTML = `<tr><td colspan="9" class="no-data">${message}</td></tr>`;
}

/**
 * HTML 이스케이프 (XSS 방지)
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 호스트 상세 정보 모달 표시
 */
async function showHostDetail(hostId, hostname, ip, serviceCode) {
    const modal = document.getElementById('hostDetailModal');

    // 호스트 기본 정보 표시
    document.getElementById('modalHostName').textContent = hostname || '호스트 상세 정보';
    document.getElementById('modalHostId').textContent = hostId;
    document.getElementById('modalIp').textContent = ip || '-';
    document.getElementById('modalServiceCode').textContent = serviceCode || '-';

    // 로딩 상태 표시
    document.getElementById('itemsLoading').style.display = 'block';
    document.getElementById('itemsWrapper').style.display = 'none';

    // 모달 표시
    modal.style.display = 'block';

    // 아이템 목록 로드
    await loadHostItems(hostId);
}

/**
 * 호스트 아이템 목록 로드
 */
async function loadHostItems(hostId) {
    try {
        const response = await fetch(`/api/hosts/${hostId}/items`);
        if (!response.ok) {
            throw new Error('아이템 정보를 불러오는데 실패했습니다.');
        }

        const items = await response.json();

        // 로딩 숨기기
        document.getElementById('itemsLoading').style.display = 'none';
        document.getElementById('itemsWrapper').style.display = 'block';

        // 아이템 테이블 렌더링
        const itemsTableBody = document.getElementById('itemsTableBody');

        if (items.length === 0) {
            itemsTableBody.innerHTML = '<tr><td colspan="2" style="text-align: center; padding: 20px; color: #999;">INFRA 아이템이 없습니다.</td></tr>';
        } else {
            itemsTableBody.innerHTML = items.map(item => `
                <tr>
                    <td>${item.name || '-'}</td>
                    <td>${item.lastValue || '-'}</td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading host items:', error);
        document.getElementById('itemsLoading').textContent = '아이템 정보를 불러오는데 실패했습니다.';
    }
}

/**
 * 모달 닫기
 */
function closeModal() {
    const modal = document.getElementById('hostDetailModal');
    modal.style.display = 'none';
}
