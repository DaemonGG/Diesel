#!/bin/bash

appium -U 1f4f52dbd53f18049a0c89f17a9170e9947ab961 &
ios_webkit_debug_proxy -d -c 1f4f52dbd53f18049a0c89f17a9170e9947ab961:27753 &
