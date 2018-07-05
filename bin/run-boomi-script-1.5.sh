#!/usr/bin/env zsh

S="${0:A}"
R="${S%/*/*}"
M="$HOME/Code/IdeaProjects/sandbox/lib"
java -cp "$M/groovy-all-1.5.8.jar:$M/groovy-all-1.5.8.jar:$R/src:$R/lib/*" groovy.lang.GroovyShell "$R/src/BoomiScriptRunner.groovy" "$@"


