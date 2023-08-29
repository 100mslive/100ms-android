package live.hms.roomkit.ui.meeting

import android.util.Log
import live.hms.video.signal.init.HMSRoomLayout
import live.hms.video.signal.init.OnStageExp

class ParticipantLabelInfo() {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()
    fun onStageExp(role : String) =
        roleMap[role]?.screens?.conferencing?.default?.elements?.onStageExp

    fun setParticipantLabelInfo(hmsRoomLayout: HMSRoomLayout?){
        this.hmsRoomLayout = hmsRoomLayout
        hmsRoomLayout?.data
            ?.forEach {data ->
                if(data?.role != null) {
                    roleMap[data.role] = data
                }
            }
    }
}