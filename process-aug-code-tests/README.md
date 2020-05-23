## Build Notes

* create a LocalConfig.groovy copy out of LocalConfig.groovy.sample and set the relative/absolute paths to the nodejs, php, and python git repo folders. 
* Snapshots of code-augmentor-core artifact must be published to Maven local cache for Groovy Grape mechanism to pick it up. If java multi-project is built with `gradlew build`, that will be guaranteed.
* change directory into tests.groovy directory before running tests with `groovy tests.groovy`
