var prefixUrl = "http://localhost:8080";
var test = {"Android-123_1/112481230A/ECG_LEAD_I_time":[1448872039.0565512,1448872039.059329,1448872039.0621068,1448872039.0648847,1448872039.0676627,1448872039.0704405,1448872039.0732183,1448872039.0759962,1448872039.078774,1448872039.0815518,1448872039.0843296,1448872039.0871074,1448872039.0898852,1448872039.092663,1448872039.0954409,1448872039.0982187,1448872039.1009965,1448872039.1037743,1448872039.1065524,1448872039.1093302,1448872039.112108,1448872039.1148858,1448872039.1176636,1448872039.1204414,1448872039.1232193,1448872039.125997,1448872039.128775,1448872039.1315527,1448872039.1343305,1448872039.1371083,1448872039.1398861,1448872039.142664,1448872039.145442,1448872039.1482198,1448872039.1509976,1448872039.1537755,1448872039.1565533,1448872039.159331,1448872039.162109,1448872039.1648867,1448872039.1676645,1448872039.1704423,1448872039.1732202,1448872039.175998,1448872039.1787758,1448872039.1815536,1448872039.1843317,1448872039.1871095,1448872039.1898873,1448872039.192665,1448872039.195443,1448872039.1982207,1448872039.2009985,1448872039.2037764,1448872039.2065542,1448872039.209332,1448872039.2121098,1448872039.2148876,1448872039.2176654,1448872039.2204432],"Android-123_1/112481230A/ECG_LEAD_I":[973,970,973,970,970,969,971,972,968,970,969,970,970,968,965,964,964,965,965,966,961,957,959,959,962,960,955,952,951,951,954,950,949,946,943,943,944,941,940,939,938,939,941,939,938,939,938,945,948,949,950,949,954,959,964,965,968,968,969,972]};
var app = angular.module('rdv-app', ['ngAnimate', 'ngMaterial', 'ngCookies'], function ($compileProvider){
  // create 'compile' directive
  $compileProvider.directive('compile', function($compile) {
    return function (scope, element, attrs) {
      scope.$watch(function(scope){return scope.$eval(attrs.compile);}, function(value) {element.html(value);$compile(element.contents())(scope);});
    };
  });
});

//app.controller('HeaderController', function ($scope) {});
app.controller('MainController', function ($scope, $rootScope, $cookies, $http, $mdToast, $timeout) {
  
  $scope.countLoad = 0;
  $scope.totalLoad = 1;
  // page to show
  // main -> main page has new button and recent sessions
  // connection -> search connection and select channel
  // session -> graph
  $scope.page = "main";
  $scope.session = {"name": "", "url": ""};
  $scope.hint_message = "loading... ({{countLoad}}/{{totalLoad}})";
  $scope.showBackIcon = false;
  $scope.session_list = [];
  $scope.dsn = "";
  $scope.showPingResult = false;
  $scope.showPingLoading = false;
  $scope.ping_result = {status: "offline", conncetions: []};

  $scope.showToast = function (message, delay) {
    delay = delay || 2000;
    $mdToast.show($mdToast.simple().content(message).action("ok").highlightAction(true).capsule(true).hideDelay(delay));
  }
  $scope.openConnectionPage = function () {
    $scope.page = "connection";
    $scope.showBackIcon = true;
  };

  $scope.openSession = function(session) {
    if (!session || !session["sid"] || !session["dsn"]) return;
    $scope.page = "session";
    $scope.session = session;

    $http.get(prefixUrl + "/session/" + session.sid).success(function (data) {
      if (data["error"]) 
        $scope.showToast(data["error"]);
      else
        console.log(data);
    }).error(function () {
      $scope.showToast("no connection from server " + prefixUrl);
    });
  };

  $scope.startSession = function(dsn) {
    $http.get(prefixUrl + "/session/start?dsn=" + dsn).success(function(data) {
      if (data["status"] == "success") {
        var session_id = data["return"];
        $scope.loadSession();
        $scope.openSession({"sid": session_id, "dsn": dsn});
        $scope.showToast("success");
      } else {
        $scope.showToast("fail to start session");
      }
    }).error(function(){$scope.showToast("no connection from " + prefixUrl)});
  };

  $scope.loadSession = function() {
    $http.get(prefixUrl + "/sessions").success(function (data) {
      if (angular.isArray(data)) {
        $scope.session_list = data;
        $scope.countLoad++;
      } else
        $scope.showToast("error: " + data);
    }).error(function () {$scope.showToast("no connection from server");});
  };
  $scope.loadSession();

  $scope.gotoMain = function() {
    $scope.page = "main";
    $scope.showBackIcon = false;
  };

  $scope.clearSessionList = function() {
    $scope.session_list = [];
    $cookies.remove("session_list");
  };

  $scope.checkEnter = function(event) {
    if (event.keyCode == 13 && $scope.dsn)
      $scope.queryConnection($scope.dsn);
  };

  $scope.queryConnection = function(dsn) {
    $scope.showPingLoading = true;
    $http.get(prefixUrl + "/ping?dsn=" + dsn).success(function(data) {
      if (data["error"]) {
        $scope.showToast("error: " + data["error"], 5000);
        $scope.showPingLoading = false;
      } else {
        $scope.showPingResult = true;
        $scope.ping_result = data;
        if (data["status"] == "online") {
          $scope.showPingLoading = false;
        } else {
          if ($scope.promise) $timeout.cancel($scope.promise);
          $scope.promise = $timeout(function(){$scope.queryConnection(dsn);}, 1000);
        }
      }
    }).error(function(){$scope.showToast("no connection from " + prefixUrl)});
  };

  $scope.cancelPromise = function () {
    $timeout.cancel($scope.promise);
    $scope.showPingLoading = false;
  };
});



app.directive('loader', function() {
  return {
    "restrict": "AEC",
    "template": '<div class="cssload-dots"><div class="cssload-dot"></div><div class="cssload-dot"></div><div class="cssload-dot"></div><div class="cssload-dot"></div><div class="cssload-dot"></div></div>' +
        '<svg version="1.1" xmlns="http://www.w3.org/2000/svg"><defs><filter id="goo"><feGaussianBlur in="SourceGraphic" result="blur" stdDeviation="12" ></feGaussianBlur><feColorMatrix in="blur" mode="matrix" values="1 0 0 0 0  0 1 0 0 0 0 0 1 0 0 0 0 0 18 -7" result="goo" ></feColorMatrix><!--<feBlend in2="goo" in="SourceGraphic" result="mix" ></feBlend>--></filter></defs></svg>'
  };
});