

# OpenCV & Tesseract Gradle Build

This repo includes a gradle build that provides the opencv and tesseract dependencies based on your operating system. 

## Requirements

### Mac

1. [Opencv](https://opencv.org/): Simply follow the Instructions in the [docs](http://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html#install-opencv-3-x-under-macos).
2. [Tesseract](https://github.com/tesseract-ocr): Install using `brew`:

```brew install tesseract --with-all-languages```

### Linux (currently not supported & tested but should work the same as macos, but needs some minor adjustments regarding installation paths)

1. [Opencv](https://opencv.org/): Simply follow the Instructions in the [docs](http://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html#install-opencv-3-x-under-linux).
2. [Tesseract](https://github.com/tesseract-ocr): Simply follow the Instructions in the [wiki](https://github.com/tesseract-ocr/tesseract/wiki#linux) for your distribution

### Windows

No additional installations required, the opecnv files are downloaded during the first build and stored at [./opencv](./opencv)


## Installation

Simply run `gradlew build`, on windows this will take some time the first time, since we're pulling the opencv files directly from sourceforge and the install it. Subsequent builds will be faster once downloaded.


## Usage

```java
import de.mattsse.opencv.LoadLibs;

// load the native opencv library
LoadLibs.loadOpencvLib();

```

## Troubleshooting

Currently the version for opencv are hardcoded in the build.gradle, if this differs from your homebrew installation this will result in an error. To resolve that issue you can either change your homebrew installation, adjust the version in build.gradle or overwrite the path to the opencv installation by setting the `OPENCV_PATH` environment variable pointing to your custom installation path.