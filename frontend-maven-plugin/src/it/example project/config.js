System.config({
  "transpiler": "traceur",
  "paths": {
    "*": "*.js",
    "github:*": "jspm_packages/github/*.js"
  }
});

System.config({
  "map": {
    "jquery": "github:jquery/jquery@3.3.1",
    "traceur": "github:jmcriffey/bower-traceur@0.0.111",
    "traceur-runtime": "github:jmcriffey/bower-traceur-runtime@0.0.111"
  }
});

