var app = angular.module('rdv-app', ['ngAnimate', 'ngMaterial', 'ngCookies'], function ($compileProvider){
  // create 'compile' directive
  $compileProvider.directive('compile', function($compile) {
    return function (scope, element, attrs) {
      scope.$watch(function(scope){return scope.$eval(attrs.compile);}, function(value) {element.html(value);$compile(element.contents())(scope);});
    };
  });
});

//app.controller('HeaderController', function ($scope) {});
app.controller('MainController', function ($scope, $rootScope) {
  // page to show
  // main -> main page has new button and recent sessions
  // connection -> search connection and select channel
  // session -> graph
  $scope.page = "main";
  $scope.session = {"name": "", "url": ""};
  $scope.showBackIcon = false;

  $scope.openConnectionPage = function () {
    $scope.page = "connection";
    $scope.showBackIcon = true;
  };

  $scope.openSession = function(url) {
    $scope.page = "session";
    $scope.session.name = "YO";
    $scope.session.url = url;
  };

  $scope.gotoMain = function() {
    $scope.page = "main";
    $scope.showBackIcon = false;
  };

});