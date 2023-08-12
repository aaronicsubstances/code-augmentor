# code-augmentor-support

Code Augmentor is library that provides

1. a markup language for text files, especially programming language source files.
2. a way to insert generated code into source code files in a way that makes it easy to detect tampering with the generated code.
3. a way to detect and record changes between a list of files and their corresponding dynamically specified content.

## Markup Language

The [AstBuilder](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstBuilder.ts) and [AstFormatter](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstFormatter.ts) modules are responsible for providing parsing and formatting functionality of the markup language provided by this library.

## Code Transformation

The [DefaultAstTransformer](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/DefaultAstTransformer.ts) and [DefaultCodeGenerationStrategy](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/DefaultCodeGenerationStrategy.ts) modules provides a suggested way of transforming files written in the markup language provided by this library.

## Code Generation and Change Detection

The [CodeChangeDetective](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/CodeChangeDetective.ts) module provides the functionality of synchronizing the content of a pair of files (actually any pair of texts or binary blobs) to be equal.

## Install

`npm install code-augmentor-support`

## Building and Testing Locally

   * Clone repository locally
   * Install project dependencies with `npm install`
   * With all dependencies present locally, test project with `npm test`

## Usage

See [Examples](https://github.com/aaronicsubstances/code-augmentor-nodejs/tree/main/examples) folder for example projects demonstrating how to use the library. The pojo-class-generation-with-sync example folder requires some prior setup of copying the contents of the src folder into a child tempSrc folder (see the src/README.txt file for details). Each example is launched with `node main.js` from the child codeGenScripts folder (may have to run `npm init` first).

The library comes with an `effect-changes` script (available from node_modules/.bin folder for WIndows Powershell and Bash). The script is meant to accept through standard input a *changes-summary.txt* file, which is created in the codeGenScripts\generated folder when the example projects are run.

The main option is running with `effect-changes -f`, which will update the files in source folder (src or tempSrc folders of the examples) with the corresponding changed files listed in the changes-summary.txt file. Can run with `effect-changes -h` to see all the available options.
