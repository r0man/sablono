var page = require('webpage').create();
var sys = require('system');
var url = sys.args[1];

page.onConsoleMessage = function (message) {
  console.log(message);
};

function exit(code) {
  setTimeout(function(){ phantom.exit(code); }, 0);
  phantom.onError = function(){};
}

console.log("Loading URL: " + url);

page.open(url, function (status) {

  if (status != "success") {
    console.log('Failed to open ' + url);
    phantom.exit(1);
  }

  console.log("Running tests ...");

  var failures = page.evaluate(function() {
    sablono.test.main();
    return window["test-failures"];
  });

  if (failures == 0) {
    console.log("Tests succeeded.")
  }
  else {
    console.log("*** Tests failed! ***");
  }

  phantom.exit(failures?100:0);
});
