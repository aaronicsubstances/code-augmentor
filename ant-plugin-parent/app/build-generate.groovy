ant = new AntBuilder()
ant.taskdef(resource: 'MyAntTasks.properties', onerror: 'failall')
ant.sequential {
	echo 'About to call code augmentation generation task...'
	code_aug_generate()
}