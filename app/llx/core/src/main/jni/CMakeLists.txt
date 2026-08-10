cmake_minimum_required(VERSION 3.13)

add_library( # Specifies the name of the library.
             ll

             # Sets the library as a static library.
             SHARED

             # Provides a relative path to your source file(s).
             ll.c )

#find_library(m -llog -ljnigraphics -landroid)
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(android-lib android)

target_link_libraries(ll ${log-lib} ${jnigraphics-lib} ${android-lib})

# 16 KB page size (Android 15+): the ELF LOAD segments must be aligned to 16384, otherwise the
# platform shows an "Android App Compatibility — ELF alignment check failed" dialog on first launch
# ("lib/arm64-v8a/libll.so : LOAD segment not aligned"). NDK r26+ links this way by default; ours is
# r25, so ask for it explicitly. Verify with `llvm-readelf -l libll.so` (LOAD Align must be 0x4000).
target_link_options(ll PRIVATE "-Wl,-z,max-page-size=16384")

set_target_properties(ll PROPERTIES LINKER_LANGUAGE CXX)