def pluginName = args[0]
def pluginTaskType = args[1]

def templateName = pluginTaskType + 'Template.txt'
def configName = pluginName + 'Config.groovy'
def outputName = pluginName + pluginTaskType + "-generated.txt"

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parentFile
def templateText = new File(scriptDir, templateName)

def binding = new Binding()
GroovyShell configParser = new GroovyShell(binding)
configParser.evaluate(new File(scriptDir, configName))

def engine = new groovy.text.SimpleTemplateEngine()
def mergedTemplate = engine.createTemplate(templateText).make(binding.variables)

new File(scriptDir, outputName).text = mergedTemplate.toString()