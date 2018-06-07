#!/usr/bin/env zsh

S="${0:A}"
R="${S%/*/*}"
groovy -cp "$R/vendor/boomi/*" "$R/src/split-uniques-cli.groovy" "$@"


