<template>
    <div>
        <div style="margin-bottom: 20px; display: flex; align-items: center; gap: 10px;">
            <span>状态筛选：</span>
            <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 150px;" @change="loadMyBookings">
                <el-option label="待审批" value="pending" />
                <el-option label="已确认" value="confirmed" />
                <el-option label="已取消" value="cancelled" />
                <el-option label="已拒绝" value="rejected" />
            </el-select>
        </div>

        <el-table :data="bookings" stripe v-loading="loading" style="width: 100%;">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column label="会议室" width="120">
                <template #default="{ row }">
                    {{ getRoomName(row.roomId) }}
                </template>
            </el-table-column>
            <el-table-column label="开始时间" width="180">
                <template #default="{ row }">
                    {{ formatDateTime(row.startTime) }}
                </template>
            </el-table-column>
            <el-table-column label="结束时间" width="180">
                <template #default="{ row }">
                    {{ formatDateTime(row.endTime) }}
                </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="statusType(row.status)">
                        {{ statusText(row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" />
            <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                    <el-button
                        v-if="row.status === 'pending' || row.status === 'confirmed'"
                        type="danger"
                        size="small"
                        @click="handleCancel(row)"
                    >
                        取消预定
                    </el-button>
                    <span v-else>-</span>
                </template>
            </el-table-column>
        </el-table>

        <div style="text-align: center; margin-top: 20px;" v-if="total > 0">
            <el-pagination
                v-model:current-page="page"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadMyBookings"
            />
        </div>
    </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getMyBookings, cancelBooking } from '../api/booking'
import { getRoomById } from '../api/room'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
    name: 'MyBookings',
    setup() {
        const bookings = ref([])
        const loading = ref(false)
        const page = ref(1)
        const pageSize = ref(10)
        const total = ref(0)
        const statusFilter = ref('')
        const roomCache = ref({})

        const getRoomName = (roomId) => {
            if (roomCache.value[roomId]) {
                return roomCache.value[roomId]
            }
            // 异步获取并缓存
            getRoomById(roomId).then(res => {
                roomCache.value[roomId] = res.data.name
            }).catch(() => {
                roomCache.value[roomId] = '未知会议室'
            })
            return '加载中...'
        }

        const loadMyBookings = async () => {
            loading.value = true
            try {
                const params = {
                    page: page.value,
                    pageSize: pageSize.value,
                    status: statusFilter.value || undefined
                }
                Object.keys(params).forEach(k => {
                    if (params[k] === undefined) delete params[k]
                })
                const res = await getMyBookings(params)
                bookings.value = res.data.records
                total.value = res.data.total
                // 预加载会议室名称
                for (const b of bookings.value) {
                    if (!roomCache.value[b.roomId]) {
                        try {
                            const r = await getRoomById(b.roomId)
                            roomCache.value[b.roomId] = r.data.name
                        } catch (e) {
                            roomCache.value[b.roomId] = '未知会议室'
                        }
                    }
                }
            } catch (e) {
                console.error(e)
            } finally {
                loading.value = false
            }
        }

        const handleCancel = async (row) => {
            try {
                await ElMessageBox.confirm('确定要取消该预定吗？', '确认', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                })
                await cancelBooking(row.id)
                ElMessage.success('取消成功')
                await loadMyBookings()
            } catch (e) {
                if (e !== 'cancel') {
                    console.error(e)
                }
            }
        }

        const formatDateTime = (timeStr) => {
            if (!timeStr) return ''
            const d = new Date(timeStr)
            const pad = (n) => String(n).padStart(2, '0')
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
        }

        const statusType = (status) => {
            const map = { pending: 'warning', confirmed: 'success', cancelled: 'info', rejected: 'danger' }
            return map[status] || 'info'
        }

        const statusText = (status) => {
            const map = { pending: '待审批', confirmed: '已确认', cancelled: '已取消', rejected: '已拒绝' }
            return map[status] || status
        }

        onMounted(() => {
            loadMyBookings()
        })

        return {
            bookings, loading, page, pageSize, total, statusFilter,
            getRoomName, loadMyBookings, handleCancel,
            formatDateTime, statusType, statusText
        }
    }
}
</script>