<template>
    <div>
        <div style="margin-bottom: 20px; display: flex; align-items: center; gap: 20px; flex-wrap: wrap;">
            <el-input v-model="equipment" placeholder="搜索设备" style="width: 200px;" clearable @change="searchRooms" />
            <el-input v-model="capacityMin" placeholder="最少容纳人数" type="number" style="width: 150px;" clearable @change="searchRooms" />
            <el-button type="primary" @click="searchRooms">搜索</el-button>
            <el-date-picker
                v-model="selectedDate"
                type="date"
                placeholder="选择日期查看占用情况"
                value-format="YYYY-MM-DD"
                :default-value="new Date()"
                style="margin-left: auto;"
            />
        </div>

        <div v-if="loading" style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>加载中...</p>
        </div>

        <div v-else-if="rooms.length === 0" style="text-align: center; padding: 40px; color: #999;">
            <el-empty description="暂无可用会议室" />
        </div>

        <el-row v-else :gutter="20">
            <el-col :span="24" v-for="room in rooms" :key="room.id" style="margin-bottom: 20px;">
                <el-card shadow="hover">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                        <div>
                            <h3 style="margin: 0 0 10px 0;">{{ room.name }}</h3>
                            <p style="margin: 5px 0; color: #666;">
                                <el-icon><User /></el-icon> 容纳人数: {{ room.capacity }}
                            </p>
                            <p style="margin: 5px 0; color: #666;">
                                <el-icon><Setting /></el-icon> 设备: {{ room.equipment || '无' }}
                            </p>
                        </div>
                        <el-button type="primary" @click="goBooking(room.id)">
                            预定
                        </el-button>
                    </div>

                    <!-- 时间轴显示 -->
                    <div v-if="selectedDate" style="margin-top: 15px; border-top: 1px solid #eee; padding-top: 15px;">
                        <h4 style="margin: 0 0 10px 0; font-size: 14px; color: #666;">{{ selectedDate }} 占用情况</h4>
                        <div v-if="getRoomBookings(room.id).length === 0" style="color: #999; font-size: 13px;">
                            该日暂无预定
                        </div>
                        <div v-else>
                            <el-tag
                                v-for="booking in getRoomBookings(room.id)"
                                :key="booking.id"
                                :type="booking.status === 'confirmed' ? 'danger' : 'warning'"
                                style="margin-right: 8px; margin-bottom: 5px;"
                            >
                                {{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}
                                ({{ booking.status === 'confirmed' ? '已确认' : '待审批' }})
                            </el-tag>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 分页 -->
        <div style="text-align: center; margin-top: 20px;" v-if="total > 0">
            <el-pagination
                v-model:current-page="page"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadRooms"
            />
        </div>
    </div>
</template>

<script>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRoomList, getRoomBookings } from '../api/room'

export default {
    name: 'RoomList',
    setup() {
        const router = useRouter()
        const rooms = ref([])
        const loading = ref(false)
        const page = ref(1)
        const pageSize = ref(10)
        const total = ref(0)
        const equipment = ref('')
        const capacityMin = ref('')
        const selectedDate = ref(new Date().toISOString().split('T')[0])
        const bookingsMap = reactive({})

        const loadRooms = async () => {
            loading.value = true
            try {
                const params = {
                    page: page.value,
                    pageSize: pageSize.value,
                    equipment: equipment.value || undefined,
                    capacityMin: capacityMin.value ? parseInt(capacityMin.value) : undefined
                }
                Object.keys(params).forEach(k => {
                    if (params[k] === undefined) delete params[k]
                })
                const res = await getRoomList(params)
                rooms.value = res.data.records
                total.value = res.data.total
                // 加载每个会议室的占用情况
                if (selectedDate.value) {
                    await loadAllBookings()
                }
            } catch (e) {
                console.error(e)
            } finally {
                loading.value = false
            }
        }

        const loadAllBookings = async () => {
            for (const room of rooms.value) {
                try {
                    const res = await getRoomBookings(room.id, { date: selectedDate.value })
                    bookingsMap[room.id] = res.data || []
                } catch (e) {
                    bookingsMap[room.id] = []
                }
            }
        }

        const getRoomBookingsList = (roomId) => {
            return bookingsMap[roomId] || []
        }

        const searchRooms = () => {
            page.value = 1
            loadRooms()
        }

        const goBooking = (roomId) => {
            router.push({
                path: '/booking',
                query: { roomId, date: selectedDate.value }
            })
        }

        const formatTime = (timeStr) => {
            if (!timeStr) return ''
            const d = new Date(timeStr)
            return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
        }

        watch(selectedDate, () => {
            if (rooms.value.length > 0) {
                loadAllBookings()
            }
        })

        onMounted(() => {
            loadRooms()
        })

        return {
            rooms, loading, page, pageSize, total,
            equipment, capacityMin, selectedDate,
            searchRooms, loadRooms, goBooking, formatTime,
            getRoomBookings: getRoomBookingsList
        }
    }
}
</script>