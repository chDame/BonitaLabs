function titleRefresh($scope, $http) {
 
 this.refresh = function () {
    $http.get($scope.properties.urlToFetch).then(function successCallback(response) {
        $scope.properties.refreshVar = response.data;
        console.log($scope.properties.refreshVar);
    });
  }
    
}
