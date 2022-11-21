LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := lyra
LOCAL_SRC_FILES := liblyra.so
include $(PREBUILT_SHARED_LIBRARY)


#include $(CLEAR_VARS)
#LOCAL_MODULE    := notlyra2
#LOCAL_SHARED_LIBRARIES := lyra
#include $(BUILD_SHARED_LIBRARY)