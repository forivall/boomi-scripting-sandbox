#!/usr/bin/env zsh

S="${0:A}"
R="${S%/*/*}"
M="$HOME/Code/IdeaProjects/sandbox/lib"
java -cp "$M/groovy-all-1.5.8.jar:$M/jline-0.9.94.jar:$R/src:$R/lib/*" org.codehaus.groovy.tools.shell.Main