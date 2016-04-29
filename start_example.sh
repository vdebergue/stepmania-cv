#!/bin/bash

# violent way to be sure we don't have -Djava.awt.headless=true
export JAVA_OPTS=

[[ "$BUILD" != "true" ]] || sbt stage || exit 3

x0=133
y0=125
w=54
h=70

x1=$((x0))
y1=$((y0))

x2=$((x1 + w))
y2=$((y0))

x3=$((x2 + w))
y3=$((y0))

x4=$((x3 + w))
y4=$((y0))

target/universal/stage/bin/stepmania-cv live -i 1 -a "$x1,$y1,$w,$h;$x2,$y2,$w,$h;$x3,$y3,$w,$h;$x4,$y4,$w,$h"
