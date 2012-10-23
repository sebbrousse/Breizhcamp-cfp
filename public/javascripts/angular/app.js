'use strict';


// Declare app level module which depends on filters, and services
var App = angular.module('breizhCampCFP', ['breizhCampCFP.services']); // , ['breizhCampCFP.filters', 'breizhCampCFP.services', 'breizhCampCFP.directives']).


// Configuration des routes
App.config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/login', {templateUrl: '/assets/pages/partials/login.html', controller: LoginController});
    $routeProvider.when('/dashboard', {templateUrl: '/assets/pages/partials/dashboard.html'});
    $routeProvider.when('/submittalk/new', {templateUrl: '/assets/pages/partials/submittalk.html', controller: NewTalkController});
    $routeProvider.when('/edittalk/:talkId', {templateUrl: '/assets/pages/partials/submittalk.html', controller: EditTalkController});
    $routeProvider.when('/managetalk', {templateUrl: '/assets/pages/partials/managetalks.html', controller: ManageTalkController});
    $routeProvider.otherwise({redirectTo: '/login'});
  }]);

