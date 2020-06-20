# 2.0.0

   * Javadocs of core library is now included in app
   * New features of augmenting code objects
       - **genCodeIndent** field added
       - **matchingNestedLevelStartMarkerIndex** field added
       - **matchingNestedLevelEndMarkerIndex** field added
       - **externalNestedContent** field added
   * New features of generated code objects
       - interpretation of *skipped* field has been extended. In addition to augmenting code section being skipped
         for code generation, it can also lead to an entire file being skipped for generation into
         destination directory if all of the file's augmenting codes are skipped.
       - reinterpretation of *contentPart.exactMatch* property. Custom similarity algorithm has been removed in favour of simpler straightforward equality comparison. *exactMatch* now means that it should be skipped over during indentation of generated code section.
   
   * Dealing with code change detection
       - change details file format changed to well known Unix diff format
       - each source file has a counterpart generated even if there are no augmenting codes in it and code change detection is disabled.
   
   * OS shell scripts additions
       - -h option added to provide help information
       - can now accept arbitary commands to run for each pair of source and corresponding enerated files.

   * Plugins
       - removed process tasks
       - removed run task in ant plugin
       - singularized these names in ant plugin prepare task: **inlineGenCodeDirective**, **nestedLevelStartMarker**, **nestedLevelEndMarker**
       - introduced ability to disable use of a set of directives in prepare tasks by adding single blank.
       - removal of encoding property from complete tasks in favour of reusing property of prepare tasks with same name.
   
   * Supporting Packages
       - CodeAugmentor reserves the text 'CodeAugmentor' for use in script functions and scope variables it can define. A function or scope variable cannot have 'CodeAugmentor' as its prefix.
       - helper functions *setScopeVar()* and *setGlobalScopeVar()* available via **CodeAugmentorFunctions** function name for use by processing scripts.
       - helper methods **newSkippedGenCode()** and **getScopeVar()** added on helper context object.
       - **codeAugmentor_indent** variable added by default to global scope with a value of four spaces.


# 1.0

  * First release
