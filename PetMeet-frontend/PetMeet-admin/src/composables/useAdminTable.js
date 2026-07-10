import { ref } from 'vue'

export function useAdminTable(options = {}) {
    const loading = ref(false)
    const allData = ref([])
    const tableData = ref([])
    const currentPage = ref(options.pageNum || 1)
    const pageSize = ref(options.pageSize || 10)
    const total = ref(0)

    const runWithLoading = async (loader, afterLoad) => {
        loading.value = true
        try {
            return await loader()
        } finally {
            if (typeof afterLoad === 'function') {
                afterLoad()
            }
            loading.value = false
        }
    }

    const resetPage = () => {
        currentPage.value = 1
    }

    const setRows = (rows, totalCount) => {
        const nextRows = Array.isArray(rows) ? rows : []
        allData.value = nextRows
        tableData.value = nextRows
        total.value = Number(totalCount || 0)
    }

    return {
        loading,
        allData,
        tableData,
        currentPage,
        pageSize,
        total,
        runWithLoading,
        resetPage,
        setRows
    }
}
