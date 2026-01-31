**NB: This project has been archived but is kept here as a reminder of inevitability of code duplication in the programming occupation. The use of ever-improving continuous integration checks is a better way to benefit from code duplication and also address its pitfalls.**

# code-augmentor-support

Code Augmentor is not just a code generator, but also your comforter in times of waiting for a feature in your favourite framework. It achieves this by providing

1. a markup language for text files which already have their own syntax, especially programming language source files.
2. a way to insert generated code into source code files in a way that makes it easy to detect tampering with the generated code.
3. a way to detect and record changes between a list of files and their corresponding dynamically specified content.

## Markup Language

The [AstParser](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstParser.ts) and [AstFormatter](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstFormatter.ts) modules are responsible for providing parsing and formatting functionality of the markup language provided by this library.

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

See [Examples](https://github.com/aaronicsubstances/code-augmentor/tree/master/examples) folder for example projects demonstrating how to use the library.

Each example is launched with `node main.js` from the child codeGenScripts folder (may have to run `npm install` first).

The [pojo-class-generation-with-sync](https://github.com/aaronicsubstances/code-augmentor/tree/master/examples/pojo-class-generation-with-sync) example requires further explanation:

   * It requires some little prior setup (see https://github.com/aaronicsubstances/code-augmentor/blob/master/examples/pojo-class-generation-with-sync/src/README.txt for details).

The library provides the `node_modules/.bin/effect-changes-nix` script for Bash (usable with Git Bash on Windows). The script is meant meant to accept 
through standard input a *changes-summary.txt* file, which is created in the codeGenScripts/generated folder when the example projects are run.

Run library tool with

```
node_modules/.bin/effect-changes-nix -f < changes-summary.txt
```

to update the files in source folder for the case were code change detection is enabled, like the pojo-class-generation-with-sync example. Its
src\tempSrc folder will be updated as determined by the contents of the changes-summary.txt file.

   * Can run the library tool with `-h` option to see all the available options.


