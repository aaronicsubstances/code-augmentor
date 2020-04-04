ant = new AntBuilder()
ant.taskdef(resource: 'MyAntTasks.properties', onerror: 'failall')
ant.sequential {
	echo 'About to call code augmentation preparation task...'
	code_aug_prepare()
}