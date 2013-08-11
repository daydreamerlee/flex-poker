var flexpokerModule = angular.module('flexpoker', ['AngularStomp', 'ngGrid']);

flexpokerModule.controller('HelloController', ['$scope', function($scope) {
    $scope.greeting = {text: 'Hello, World'};
}]);

flexpokerModule.controller('TournamentRegisteringController', ['$scope', 'ngstomp', function($scope, ngstomp) {
    
    $('#create-game-dialog').hide();
    $('body').skin();
    
    $scope.games = [];
    $scope.gridOptions = { data: 'games' };
    $scope.client = ngstomp(new SockJS(rootUrl + 'application'));
    $scope.client.connect("", "", function() {
        $scope.client.subscribe("/app/availabletournaments", function(message) {
            $scope.games = $.parseJSON(message.body);
        });
        $scope.client.subscribe("/topic/availabletournaments-updates", function(message) {
            $scope.games = $.parseJSON(message.body);
        });
    }, function() {}, '/');
    
    $scope.openCreateGameDialog = function() {
        $('#create-game-dialog').dialog({ width: 550 });
    }
    
    $scope.numberOfTables = ($scope.players / $scope.playersPerTable) + 1;
    
    $scope.submit = function() {
        var newGame = {
                name: $scope.name,
                players: $scope.players,
                playersPerTable: $scope.playersPerTable
        };
        $scope.client.send("/app/creategame", {}, JSON.stringify(newGame));
        $scope.name = '';
        $scope.players = '';
        $scope.playersPerTable = '';
        $scope.numberOfTables = '--';
        $('#create-game-dialog').dialog('destroy');
    }
}]);

// taken from http://jsfiddle.net/thomporter/DwKZh/
flexpokerModule.directive('numbersOnly', function() {
    return {
      require: 'ngModel',
      link: function(scope, element, attrs, modelCtrl) {
        modelCtrl.$parsers.push(function (inputValue) {
            // this next if is necessary for when using ng-required on your input.
            // In such cases, when a letter is typed first, this parser will be called
            // again, and the 2nd time, the value will be undefined
            if (inputValue == undefined) {
                return '';
            }
            var transformedInput = inputValue.replace(/[^0-9]/g, '');
            if (transformedInput!=inputValue) {
               modelCtrl.$setViewValue(transformedInput);
               modelCtrl.$render();
            }

            return transformedInput;
        });
      }
    };
});

$.fn.skin = function() {
    this.find('button, input[type=submit]').button();
};

$(function() {
    $('body').skin();
});