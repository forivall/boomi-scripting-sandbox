Boomi Groovy Custom Script Dev Environment
===

(only for mac osx or linux right now. feel free to submit a pull request to add a ".bat" version of the "run-boomi-script.sh" script.

## Setup

Install the Boomi Atom on your computer, deploy any integration to it, and then copy the contents of `<atom-install-dir>/lib` to the `lib` folder

## Basic Instructions

Use IntelliJ IDEA.

Load the project.

Write a custom processing step in groovy in the "scripts" directory

Open a terminal emulator

Run "./bin/run-boomi-script.sh <path-to-your-custom-scripting-code>"

You can also use the "-h" command for basic help.
