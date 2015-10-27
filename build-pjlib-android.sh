#!/bin/bash

# based on http://trac.pjsip.org/repos/wiki/Getting-Started/Android
SOURCE_URL="http://www.pjsip.org/release/2.4/pjproject-2.4.tar.bz2"
ARCHIVE_FILE="./pjproject-2.4.tar.bz2"
SOURCE_DIR="./pjproject-2.4"
TARGETS="armeabi armeabi-v7a x86 mips"
OUTPUT_ARCHIVE="pjsip.tar"

# Android-21 platform libc is incompatible with older versions of
# earlier versions of the libc (android). Fallback to earlier version.
export APP_PLATFORM=android-20

function die {
    echo $1
    exit 1
}
# Require a NDK installation
test -n "$ANDROID_NDK_ROOT" && test -d "$ANDROID_NDK_ROOT" || \ 
        die "Need a valid ANDROID_NDK_ROOT environment variable"
if [ ! -f $ARCHIVE_FILE ]
then
    echo "Acquiring source from $SOURCE_URL"
    curl $SOURCE_URL -o $ARCHIVE_FILE || die "Cannot download archive file"
fi



rm -f $OUTPUT_ARCHIVE

# Build all targets
for t in $TARGETS
do
    export TARGET_ABI="$t"
    # Remove source directory to get a pristine build
    rm -rf $SOURCE_DIR
    tar xjf $ARCHIVE_FILE || die "Cannot unpack archive"

    pushd $SOURCE_DIR

    # Configure pjlib to use default android configuration
    cat <<EOF > pjlib/include/pj/config_site.h
/* auto-generated from build script */
#define PJ_CONFIG_ANDROID 1
#include <pj/config_site_sample.h>
EOF

    # Build the library
    echo "Building for $TARGET_ABI"
    # Configure this targetversion of android
    ./configure-android --use-ndk-cflags > /dev/null \
        || die "Cannot configure $TARGET_ABI"

    # Make, but be silent about it
   (make dep && make clean && make) &> /dev/null || \
            die "Cannot build library for $TARGET_ABI"

    # Build swig wrapper and .so file
    pushd ./pjsip-apps/src/swig
    make > /dev/null || die "Cannot build Java Wrapper for $TARGET_ABI"
    # Move .so file to correct directory, build system always creates
    # the archive in the armeabi directory
    if [ "$TARGET_ABI" != "armeabi" ]
    then
        mv java/android/libs/armeabi java/android/libs/$TARGET_ABI
    fi
    popd # leave swig
    popd # leave source directory
    if [ ! -f $OUTPUT_ARCHIVE ]
    then
        # create new archive
        tar cvf $OUTPUT_ARCHIVE --exclude '*/pjsua2/app/' \
            -C $SOURCE_DIR/pjsip-apps/src/swig/java/android \
            src libs || die "Cannot create archive"
    else
        # add compiled libs
        tar rvf $OUTPUT_ARCHIVE \
            -C $SOURCE_DIR/pjsip-apps/src/swig/java/android \
            libs || die "Cannot add to archive"
    fi
done

rm -rf $SOURCE_DIR
echo "Done: output in $OUTPUT_ARCHIVE"



