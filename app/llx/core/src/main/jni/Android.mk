LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(call all-subdir-makefiles)

LOCAL_CFLAGS += -std=c99 -O2 -W -Wall -Wno-unused-parameter
LOCAL_MODULE    := ll
LOCAL_SRC_FILES := ll.c

LOCAL_LDLIBS    := -lm -llog -ljnigraphics -landroid
# Keep in sync with CMakeLists.txt: 16 KB page alignment for Android 15+ (this .mk is unused by the
# gradle build, which drives CMake, but stays here so both descriptions agree).
LOCAL_LDFLAGS   += -Wl,-z,max-page-size=16384

include $(BUILD_SHARED_LIBRARY)
