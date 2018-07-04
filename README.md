# httpd module for Trigger.io

This repository holds everything required to build the `httpd` [Trigger.io](https://trigger.io/) module.

For more information about working on Trigger.io native modules, see [the documentation](https://trigger.io/docs/current/api/native_modules/index.html).


## Building Criollo

git clone https://github.com/thecatalinstan/Criollo.git
cd Criollo
git submodule update --init --recursive

### Edit project.pbxproj to convert to static library:

    productType = "com.apple.product-type.framework";

should become:

    productType = "com.apple.product-type.library.static”;


### build for hardware & simulator
    xcodebuild -arch arm64 -arch armv7 -arch armv7s -sdk iphoneos -configuration Release -target "Criollo iOS"
    xcodebuild -arch i386 -arch x86_64 -sdk iphonesimulator -configuration Release -target "Criollo iOS"

### Use lipo to combine the hardware & simulator binaries into a single fat binary
    # lipo -create -output Criollo.dylib ./build/Release-iphonesimulator/Criollo.framework/Criollo build/Release-iphoneos/Criollo.framework/Criollo
    lipo -create -output libCriollo.a ./build/Release-iphonesimulator/libCriollo.a build/Release-iphoneos/libCriollo.a
