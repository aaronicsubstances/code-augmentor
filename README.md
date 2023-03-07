# code-augmentor-support

Code Augmentor is library that provides

1. *a markup language for source code files of programming languages.*
2. a way to insert generated code into source code files in a way that makes it easy to detect tampering with the generated code.
3. a way to detect and record changes between a list of files and their corresponding dynamically specified content.

## Markup language.

The [AstBuilder](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstBuilder.ts) and [AstFormatter](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstFormatter.ts) modules are responsible for providing parsing and formatting functionality of the markup language provided by this library.

Each line of a source code file is categorized initially into one or two structures using a list of special line prefixes known as "markers". E.g. given a list of decorated line markers containing only one member as "cd:", a file with Unix line endings containing two lines "ab" and " cd:e" will be parsed initially as follows:

1. { text: "ab", lineSep: "\n" } (known as undecorated line)
2. { indent: " ", marker: "cd:", markerAftermath: "e", lineSep: "\n" } (known as decorated line)

The second and final stage of the parsing involves identifying groups of lines as escaped blocks or nested blocks, through a list of escaped block start and end markers, and nested block start and end markers.

An escaped/nested block starts with a decorated line whose marker belongs to the list of escaped/nested block start markers, and ends with a decorated line whose marker belongs to the list of escaped/nested block end markers.

Any kind of line or block is permitted in between the starting and ending lines of a nested block. An escaped block however can only have undecorated lines in between its starting and ending lines, with the following restrictions:
1. The ending line's markerAftermath for an escaped block must equal that of the starting line's markerAftermath.
2. The concatenation of the ending line's marker and markerAftermath cannot be a substring of any of the lines of an escaped block above the ending line.

## Install

`npm install code-augmentor-support`

## Building and Testing Locally

   * Clone repository locally
   * Install project dependencies with `npm install`
   * With all dependencies present locally, test project with `npm test`
