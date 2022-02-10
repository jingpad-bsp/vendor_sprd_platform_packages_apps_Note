LOCAL_PATH:= $(call my-dir)

#SprdNote--new note

include $(CLEAR_VARS)
#ifeq ($(TARGET_BUILD_APPS),)
#support_library_root_dir := frameworks/support
#else
#support_library_root_dir := prebuilts/sdk/current/support
#endif

LOCAL_MODULE_TAGS := optional
#LOCAL_PACKAGE_NAME := NoteBook
#LOCAL_CERTIFICATE := PRESIGNED
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/src/main/res

src_dirs := src/main/java/
LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))

LOCAL_STATIC_ANDROID_LIBRARIES := \
        android-support-v4 \
        android-support-v7-appcompat \
        android-support-design \
#       notejar-release

LOCAL_USE_AAPT2 := true
#LOCAL_AAPT_FLAGS := \
#       --auto-add-overlay \
#       --extra-packages android.support.v7.appcompat \
#       --extra-packages android.support.design

LOCAL_PACKAGE_NAME := NoteBook
LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
