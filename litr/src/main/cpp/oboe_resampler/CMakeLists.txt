cmake_minimum_required(VERSION 3.10.2)

project("OboeResampler")

add_library(oboe-resampler STATIC
        IntegerRatio.cpp
        LinearResampler.cpp
        MultiChannelResampler.cpp
        PolyphaseResampler.cpp
        PolyphaseResamplerMono.cpp
        PolyphaseResamplerStereo.cpp
        SincResampler.cpp
        SincResamplerStereo.cpp
)