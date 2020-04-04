ant = new AntBuilder()
ant.taskdef(resource: 'MyAntTasks.properties', onerror: 'failall')
ant.sequential {
	echo 'Succesfully loaded "MyAntTasks"'
}