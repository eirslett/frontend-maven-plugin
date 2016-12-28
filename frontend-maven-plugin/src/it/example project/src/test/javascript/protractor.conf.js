exports.config = {
  multiCapabilities: [
	{
		browserName: 'chrome',
	}
  ], 
  specs: [
    '../e2e/*.js'
  ],
};