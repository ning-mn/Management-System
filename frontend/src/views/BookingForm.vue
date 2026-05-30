<template>
    <div style="max-width: 600px; margin: 0 auto;">
        <el-card>
            <h2 style="text-align: center;">预定会议室</h2>
            <p v-if="roomName" style="text-align: center; color: #409eff; font-size: 16px;">
                会议室：{{ roomName }}
            </p>

            <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="margin-top: 20px;">
                <el-form-item label="日期" prop="date">
                    <el-date-picker
                        v-model="form.date"
                        type="date"
                        placeholder="选择日期"
                        :disabled-date="disabledDate"
                        value-format="YYYY-MM-DD"
                        style="width: 100%;"
                    />
                </el-form-item>

                <el-form-item label="开始时间" prop="startTime">
                    <el-select v-model="form.startTime" placeholder="选择开始时间" style="width: 100%;">
                        <el-option
                            v-for="t in timeSlots"
                            :key="t"
                            :label="t"
                            :value="t"
                            :disabled="isTimeDisabled(t)"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="结束时间" prop="endTime">
                    <el-select v-model="form.endTime" placeholder="选择结束时间" style="width: 100%;">
                        <el-option
                            v-for="t in timeSlots"
                            :key="t"
                            :label="t"
                            :value="t"
                            :disabled="isTimeDisabled(t)"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="备注">
                    <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="选填" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="handleSubmit" :loading="loading" style="width: 100%;">
                        提交预定
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRoomById, getRoomBookings } from '../api/room'
import { createBooking } from '../api/booking'
import { ElMessage } from 'element-plus'

export default {
    name: 'BookingForm',
    setup() {
        const router = useRouter()
        const route = useRoute()
        const formRef = ref(null)
        const loading = ref(false)
        const roomName = ref('')
        const occupiedSlots = ref([])

        const form = reactive({
            roomId: parseInt(route.query.roomId) || null,
            date: route.query.date || new Date().toISOString().split('T')[0],
            startTime: '',
            endTime: '',
            remark: ''
        })

        // 生成08:00~20:00每30分钟的时间槽
        const timeSlots = computed(() => {
            const slots = []
            for (let h = 8; h < 20; h++) {
                slots.push(`${String(h).padStart(2, '0')}:00`)
                slots.push(`${String(h).padStart(2, '0')}:30`)
            }
            return slots
        })

        const rules = {
            date: [{ required: true, message: '请选择日期', trigger: 'change' }],
            startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
            endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
        }

        const disabledDate = (time) => {
            const today = new Date()
            today.setHours(0, 0, 0, 0)
            return time.getTime() < today.getTime()
        }

        const loadRoomInfo = async () => {
            if (!form.roomId) {
                ElMessage.error('请选择会议室')
                router.push('/')
                return
            }
            try {
                const res = await getRoomById(form.roomId)
                roomName.value = res.data.name
            } catch (e) {
                ElMessage.error('获取会议室信息失败')
                router.push('/')
            }
            await loadOccupiedSlots()
        }

        const loadOccupiedSlots = async () => {
            if (!form.date || !form.roomId) return
            try {
                const res = await getRoomBookings(form.roomId, { date: form.date })
                occupiedSlots.value = (res.data || []).filter(
                    b => b.status === 'pending' || b.status === 'confirmed'
                )
            } catch (e) {
                occupiedSlots.value = []
            }
        }

        const isSlotOccupied = (timeStr) => {
            const [h, m] = timeStr.split(':').map(Number)
            const checkTime = new Date(form.date + 'T' + timeStr + ':00')

            return occupiedSlots.value.some(b => {
                const start = new Date(b.startTime)
                const end = new Date(b.endTime)
                return checkTime >= start && checkTime < end
            })
        }

        const isTimeDisabled = (timeStr) => {
            const [h, m] = timeStr.split(':').map(Number)
            const now = new Date()
            const today = new Date().toISOString().split('T')[0]

            if (form.date === today) {
                const slotTime = new Date()
                slotTime.setHours(h, m, 0, 0)
                if (slotTime <= now) return true
            }

            if (isSlotOccupied(timeStr)) return true

            return false
        }

        const handleSubmit = async () => {
            const valid = await formRef.value.validate().catch(() => false)
            if (!valid) return

            if (form.startTime >= form.endTime) {
                ElMessage.error('结束时间必须晚于开始时间')
                return
            }

            loading.value = true
            try {
                const startDateTime = `${form.date}T${form.startTime}:00`
                const endDateTime = `${form.date}T${form.endTime}:00`

                await createBooking({
                    roomId: form.roomId,
                    startTime: startDateTime,
                    endTime: endDateTime,
                    remark: form.remark
                })
                ElMessage.success('预定成功！')
                router.push('/my-bookings')
            } catch (e) {
                // 错误已在拦截器中处理
            } finally {
                loading.value = false
            }
        }

        watch(() => form.date, () => {
            loadOccupiedSlots()
        })

        onMounted(() => {
            loadRoomInfo()
        })

        return {
            formRef, form, loading, roomName,
            timeSlots, rules, disabledDate,
            isTimeDisabled, handleSubmit
        }
    }
}
</script>
