#!/bin/sh

cmake -S . -B build -GNinja

ninja -C ./build
