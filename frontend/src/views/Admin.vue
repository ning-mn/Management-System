<template>
    <div>
        <el-tabs v-model="activeTab">
            <el-tab-pane label="会议室管理" name="rooms">
                <div style="margin-bottom: 15px;">
                    <el-button type="primary" @click="showAddRoomDialog">新增会议室</el-button>
                </div>

                <el-table :data="rooms" stripe v-loading="roomsLoading" style="width: 100%;">
                    <el-table-column prop="id" label="ID" width="60" />
                    <el-table-column prop="name" label="名称" width="150" />
                    <el-table-column prop="capacity" label="容纳人数" width="100" />
                    <el-table-column prop="equipment" label="设备" min-width="200" />
                    <el-table-column prop="status" label="状态" width="100">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 'available' ? 'success' : 'danger'">
                                {{ row.status === 'available' ? '可用' : '维护中' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="200" fixed="right">
                        <template #default="{ row }">
                            <el-button type="primary" size="small" @click="showEditRoomDialog(row)">编辑</el-button>
                            <el-button type="danger" size="small" @click="handleDeleteRoom(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>

            <el-tab-pane label="预定管理" name="bookings">
                <div style="margin-bottom: 15px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                    <el-select v-model="bookingFilter.status" placeholder="状态筛选" clearable style="width: 130px;">
                        <el-option label="待审批" value="pending" />
                        <el-option label="已确认" value="confirmed" />
                        <el-option label="已取消" value="cancelled" />
                        <el-option label="已拒绝" value="rejected" />
                    </el-select>
                    <el-input v-model="bookingFilter.roomId" placeholder="会议室ID" type="number" style="width: 120px;" clearable />
                    <el-date-picker
                        v-model="bookingFilter.startTimeBegin"
                        type="date"
                        placeholder="开始日期起"
                        value-format="YYYY-MM-DD"
                        style="width: 140px;"
                    />
                    <el-date-picker
                        v-model="bookingFilter.startTimeEnd"
                        type="date"
                        placeholder="开始日期止"
                        value-format="YYYY-MM-DD"
                        style="width: 140px;"
                    />
                    <el-button type="primary" @click="loadAllBookings">搜索</el-button>
                </div>

                <el-table :data="allBookings" stripe v-loading="bookingsLoading" style="width: 100%;">
                    <el-table-column prop="id" label="ID" width="60" />
                    <el-table-column prop="userId" label="用户ID" width="80" />
                    <el-table-column prop="roomId" label="会议室ID" width="90" />
                    <el-table-column label="开始时间" width="160">
                        <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
                    </el-table-column>
                    <el-table-column label="结束时间" width="160">
                        <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="80">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="备注" min-width="120" />
                    <el-table-column label="操作" width="250" fixed="right">
                        <template #default="{ row }">
                            <el-button
                                v-if="row.status === 'pending'"
                                type="success"
                                size="small"
                                @click="handleApprove(row)"
                            >通过</el-button>
                            <el-button
                                v-if="row.status === 'pending'"
                                type="danger"
                                size="small"
                                @click="handleReject(row)"
                            >拒绝</el-button>
                            <el-button
                                v-if="row.status === 'pending' || row.status === 'confirmed'"
                                type="info"
                                size="small"
                                @click="handleAdminCancel(row)"
                            >取消</el-button>
                            <span v-else>-</span>
                        </template>
                    </el-table-column>
                </el-table>

                <div style="text-align: center; margin-top: 20px;" v-if="allTotal > 0">
                    <el-pagination
                        v-model:current-page="bookingPage"
                        :page-size="bookingPageSize"
                        :total="allTotal"
                        layout="prev, pager, next"
                        @current-change="loadAllBookings"
                    />
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- 新增/编辑会议室弹窗 -->
        <el-dialog v-model="roomDialogVisible" :title="isEdit ? '编辑会议室' : '新增会议室'" width="500px">
            <el-form :model="roomForm" :rules="roomRules" ref="roomFormRef" label-width="100px">
                <el-form-item label="名称" prop="name">
                    <el-input v-model="roomForm.name" />
                </el-form-item>
                <el-form-item label="容纳人数" prop="capacity">
                    <el-input-number v-model="roomForm.capacity" :min="1" :max="1000" />
                </el-form-item>
                <el-form-item label="设备" prop="equipment">
                    <el-input v-model="roomForm.equipment" placeholder="如：投影仪、白板" />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-select v-model="roomForm.status">
                        <el-option label="可用" value="available" />
                        <el-option label="维护中" value="maintenance" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="roomDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleRoomSubmit" :loading="roomSubmitting">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { getRoomList, addRoom, updateRoom, deleteRoom } from '../api/room'
import { getAllBookings, updateBookingStatus, cancelBooking } from '../api/booking'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
    name: 'Admin',
    setup() {
        const activeTab = ref('rooms')

        // ---- 会议室管理 ----
        const rooms = ref([])
        const roomsLoading = ref(false)
        const roomDialogVisible = ref(false)
        const isEdit = ref(false)
        const roomFormRef = ref(null)
        const roomSubmitting = ref(false)
        const editRoomId = ref(null)
        const roomForm = reactive({
            name: '',
            capacity: 1,
            equipment: '',
            status: 'available'
        })
        const roomRules = {
            name: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }],
            capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }]
        }

        const loadRooms = async () => {
            roomsLoading.value = true
            try {
                const res = await getRoomList({ page: 1, pageSize: 100 })
                rooms.value = res.data.records
            } catch (e) {
                console.error(e)
            } finally {
                roomsLoading.value = false
            }
        }

        const showAddRoomDialog = () => {
            isEdit.value = false
            editRoomId.value = null
            roomForm.name = ''
            roomForm.capacity = 1
            roomForm.equipment = ''
            roomForm.status = 'available'
            roomDialogVisible.value = true
        }

        const showEditRoomDialog = (row) => {
            isEdit.value = true
            editRoomId.value = row.id
            roomForm.name = row.name
            roomForm.capacity = row.capacity
            roomForm.equipment = row.equipment || ''
            roomForm.status = row.status
            roomDialogVisible.value = true
        }

        const handleRoomSubmit = async () => {
            const valid = await roomFormRef.value.validate().catch(() => false)
            if (!valid) return

            roomSubmitting.value = true
            try {
                if (isEdit.value) {
                    await updateRoom(editRoomId.value, roomForm)
                    ElMessage.success('修改成功')
                } else {
                    await addRoom(roomForm)
                    ElMessage.success('新增成功')
                }
                roomDialogVisible.value = false
                await loadRooms()
            } catch (e) {
                console.error(e)
            } finally {
                roomSubmitting.value = false
            }
        }

        const handleDeleteRoom = async (row) => {
            try {
                await ElMessageBox.confirm(`确定要删除「${row.name}」吗？`, '确认', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                })
                await deleteRoom(row.id)
                ElMessage.success('删除成功')
                await loadRooms()
            } catch (e) {
                if (e !== 'cancel') {
                    console.error(e)
                }
            }
        }

        // ---- 预定管理 ----
        const allBookings = ref([])
        const bookingsLoading = ref(false)
        const bookingPage = ref(1)
        const bookingPageSize = ref(10)
        const allTotal = ref(0)
        const bookingFilter = reactive({
            status: '',
            roomId: '',
            startTimeBegin: '',
            startTimeEnd: ''
        })

        const loadAllBookings = async () => {
            bookingsLoading.value = true
            try {
                const params = {
                    page: bookingPage.value,
                    pageSize: bookingPageSize.value,
                    status: bookingFilter.status || undefined,
                    roomId: bookingFilter.roomId ? parseInt(bookingFilter.roomId) : undefined,
                    startTimeBegin: bookingFilter.startTimeBegin || undefined,
                    startTimeEnd: bookingFilter.startTimeEnd || undefined
                }
                Object.keys(params).forEach(k => {
                    if (params[k] === undefined) delete params[k]
                })
                const res = await getAllBookings(params)
                allBookings.value = res.data.records
                allTotal.value = res.data.total
            } catch (e) {
                console.error(e)
            } finally {
                bookingsLoading.value = false
            }
        }

        const handleApprove = async (row) => {
            try {
                await updateBookingStatus(row.id, { status: 'confirmed' })
                ElMessage.success('已通过')
                await loadAllBookings()
            } catch (e) {
                console.error(e)
            }
        }

        const handleReject = async (row) => {
            try {
                await updateBookingStatus(row.id, { status: 'rejected' })
                ElMessage.success('已拒绝')
                await loadAllBookings()
            } catch (e) {
                console.error(e)
            }
        }

        const handleAdminCancel = async (row) => {
            try {
                await ElMessageBox.confirm('确定要取消该预定吗？', '确认', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                })
                await cancelBooking(row.id)
                ElMessage.success('已取消')
                await loadAllBookings()
            } catch (e) {
                if (e !== 'cancel') {
                    console.error(e)
                }
            }
        }

        // ---- 工具方法 ----
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
            loadRooms()
            loadAllBookings()
        })

        return {
            activeTab,
            rooms, roomsLoading, roomDialogVisible, isEdit, roomFormRef, roomSubmitting, editRoomId, roomForm, roomRules,
            showAddRoomDialog, showEditRoomDialog, handleRoomSubmit, handleDeleteRoom, loadRooms,
            allBookings, bookingsLoading, bookingPage, bookingPageSize, allTotal, bookingFilter,
            loadAllBookings, handleApprove, handleReject, handleAdminCancel,
            formatDateTime, statusType, statusText
        }
    }
}
</script>