#!/usr/bin/env zsh

S="${0:A}"
R="${S%/*/*}"
groovy -cp "$R/src:$R/lib/*" "$R/src/BoomiScriptRunner.groovy" "$@"


