function FancyTableCtrl($scope,$http, $timeout) {

  this.isArray = Array.isArray;

  this.isClickable = function () {
    return $scope.properties.isBound('selectedRow');
  };

  this.selectRow = function (row) {
    if (this.isClickable()) {
      $scope.properties.selectedRow = row;
    }
  };
  
  $scope.assignMe = function(task){
   $http.put('/bonita/API/bpm/humanTask/'+ task.id, { assigned_id : $scope.properties.userId}).then(function successCallback(response) {
        task.assigned_id = $scope.properties.userId;
    });
  }
  
  ;$scope.unassign = function(task){
   $http.put('/bonita/API/bpm/humanTask/'+ task.id, { assigned_id : ''}).then(function successCallback(response) {
        task.assigned_id = null;
    });
  };
    

    $timeout(function() {
        $('table.fancyTable')
        .on( 'order.dt',  function () { } )
        .on( 'search.dt', function () { } )
        .on( 'page.dt',   function () { } )
        .dataTable({
            "order": [[ 1, "desc" ]],
            "retrieve": true,
        });
    }, 1000);

}
