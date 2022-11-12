LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := lyra
LOCAL_SRC_FILES := liblyra.so
include $(PREBUILT_SHARED_LIBRARY)


