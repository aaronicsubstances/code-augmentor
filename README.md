# code-augmentor-support

Code Augmentor is library that provides

1. a markup language for text files, especially programming language source files.
2. a way to insert generated code into source code files in a way that makes it easy to detect tampering with the generated code.
3. a way to detect and record changes between a list of files and their corresponding dynamically specified content.

## Markup Language

The [AstBuilder](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstBuilder.ts) and [AstFormatter](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/AstFormatter.ts) modules are responsible for providing parsing and formatting functionality of the markup language provided by this library.

## Code Transformation

The [DefaultAstTransformer](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/DefaultAstTransformer.ts) module provides a suggested way of transforming files written in the markup language provided by this library.

## Code Generation and Change Detection

The [CodeChangeDetective](https://github.com/aaronicsubstances/code-augmentor-nodejs/blob/master/src/CodeChangeDetective.ts) module provides the functionality of synchronizing the content of a pair of files (actually any pair of texts or binary blobs) to be equal.

## Install

`npm install code-augmentor-support`

## Building and Testing Locally

   * Clone repository locally
   * Install project dependencies with `npm install`
   * With all dependencies present locally, test project with `npm test`
