/**
 * The controller is a JavaScript function that augments the AngularJS scope and exposes functions that can be used in the custom widget template
 * 
 * Custom widget properties defined on the right can be used as variables in a controller with $scope.properties
 * To use AngularJS standard services, you must declare them in the main function arguments.
 * 
 * You can leave the controller empty if you do not need it.
 */
function customTabs($scope, $http, $timeout, $filter) {
    
    // add a new variable in AngularJS scope. It'll be usable in the template directly with {{ backgroudColor }} 
    $scope.tab = 'task';
    $scope.popup = false;
    $scope.comments = null;
    $scope.newComment = null;
    
    this.select = function(tabName) {
        $scope.tab = tabName;
        if (tabName=='task') {
            
        } else if (tabName=='overview') {
            if ($scope.properties.task.id) {
                $scope.frameOverviewSrc=$scope.properties.baseUrl+'/portal/resource/processInstance/'+$scope.properties.task.processName+'/'+$scope.properties.task.processVersion+'/content/?id='+$scope.properties.task.parentCaseId;
            } else {
                $scope.frameOverviewSrc=null;
            }
        } else {
             $scope.loadComments();
        }
    }
    
  this.assignMe = function(){
   $http.put($scope.properties.baseUrl+'/API/bpm/humanTask/'+ $scope.properties.task.id, { assigned_id : $scope.properties.userId}).then(function successCallback(response) {
        $scope.properties.task.assigned_id = $scope.properties.userId;
    });
  }
  
  $scope.loadComments = function() {
      if (!$scope.comments) {
        $http.get($scope.properties.baseUrl+'/API/bpm/comment?c=2147483647&d=userId&f=processInstanceId%3D'+$scope.properties.task.parentCaseId+'&o=postDate+ASC&p=0').then(function successCallback(response) {
            $scope.comments = [];
        
            for(let i=0; i<response.data.length; i++) {
                let comment = response.data[i];
                if (comment.userId.userName!='System') {
                    comment.postDate = new Date(comment.postDate);
                    console.log( $filter('date')(new Date(comment.postDate), 'dd/MM/yyy h:mma'));
                    $scope.comments.push(comment);
                }
            }
        });
    }
  }
  
  $scope.addComment = function(newComment) {
      console.log(newComment);
      let postComment = {processInstanceId: $scope.properties.task.parentCaseId, content: newComment}
       $http.post($scope.properties.baseUrl+'/API/bpm/comment/', postComment).then(function successCallback(response) {
            $scope.comments = null;
            $scope.loadComments();
            $scope.newComment = null;
        });
  }
    
    $scope.$watch('properties.task', function(newValue, oldValue) {
        if ($scope.properties.task.id) {
            $scope.comments = null;
            $scope.frameTaskSrc=$scope.properties.baseUrl+'/portal/form/taskInstance/'+$scope.properties.task.id;
        } else {
            $scope.frameTaskSrc=null;
        }
        $scope.tab = 'task';
    });
    
    this.detailsPopup = function() {
         $scope.popup = ! $scope.popup;
    }
}